package com.example.kursovaya.websocket

import android.util.Log
import com.example.kursovaya.model.websocket.QueueResponse
import com.example.kursovaya.model.websocket.QueueUpdate
import com.example.kursovaya.repository.AuthRepository
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.StompMessage

class QueueWebSocketClient(
    private val authRepository: AuthRepository,
    private val baseUrl: String = "http://10.0.2.2:8085",
) {
    private var stompClient: StompClient? = null
    private val disposables = CompositeDisposable()
    private val gson = Gson()

    private var onMessageCallback: ((QueueResponse) -> Unit)? = null
    private var onQueueUpdateCallback: ((QueueUpdate) -> Unit)? = null
    private var onErrorCallback: ((Throwable) -> Unit)? = null
    private var onConnectedCallback: (() -> Unit)? = null
    private var onDisconnectedCallback: (() -> Unit)? = null

    // Храним активные подписки на обновления очередей
    private val queueSubscriptions = mutableMapOf<Long, Disposable>()

    fun setOnMessageCallback(callback: (QueueResponse) -> Unit) {
        onMessageCallback = callback
    }

    fun setOnErrorCallback(callback: (Throwable) -> Unit) {
        onErrorCallback = callback
    }

    fun setOnConnectedCallback(callback: () -> Unit) {
        onConnectedCallback = callback
    }

    fun setOnDisconnectedCallback(callback: () -> Unit) {
        onDisconnectedCallback = callback
    }

    fun setOnQueueUpdateCallback(callback: (QueueUpdate) -> Unit) {
        onQueueUpdateCallback = callback
    }

    fun connect() {
        try {
            val token =
                authRepository.getAuthState().let { state ->
                    if (state is com.example.kursovaya.model.AuthState.Authenticated) {
                        state.token
                    } else {
                        Log.e("QueueWebSocket", "User not authenticated")
                        onErrorCallback?.invoke(Exception("User not authenticated"))
                        return
                    }
                }

            // Используем SockJS URL (HTTP) для лучшей совместимости
            val wsUrl = "$baseUrl/queue-websocket?token=$token"
            Log.d("QueueWebSocket", "Connecting to: $wsUrl")

            stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)

            // Обработка подключения (должна быть до подписки на топики)
            val lifecycleSubscription =
                stompClient!!.lifecycle()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ lifecycleEvent ->
                        when (lifecycleEvent.type) {
                            ua.naiksoftware.stomp.dto.LifecycleEvent.Type.OPENED -> {
                                Log.d("QueueWebSocket", "WebSocket connected")
                                onConnectedCallback?.invoke()
                            }
                            ua.naiksoftware.stomp.dto.LifecycleEvent.Type.CLOSED -> {
                                Log.d("QueueWebSocket", "WebSocket closed")
                                onDisconnectedCallback?.invoke()
                            }
                            ua.naiksoftware.stomp.dto.LifecycleEvent.Type.ERROR -> {
                                Log.e("QueueWebSocket", "WebSocket error: ${lifecycleEvent.exception}")
                                onErrorCallback?.invoke(lifecycleEvent.exception ?: Exception("Unknown error"))
                            }
                            else -> {}
                        }
                    }, { error ->
                        Log.e("QueueWebSocket", "Lifecycle error", error)
                        onErrorCallback?.invoke(error)
                    })

            disposables.add(lifecycleSubscription)

            // Подписываемся на ответы от сервера
            val topicSubscription =
                stompClient!!.topic("/user/queue/user")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ message: StompMessage ->
                        try {
                            val response = gson.fromJson(message.payload, QueueResponse::class.java)
                            Log.d("QueueWebSocket", "Received message: ${response.message}")
                            onMessageCallback?.invoke(response)
                        } catch (e: Exception) {
                            Log.e("QueueWebSocket", "Error parsing message", e)
                            onErrorCallback?.invoke(e)
                        }
                    }, { error: Throwable ->
                        Log.e("QueueWebSocket", "Subscription error", error)
                        onErrorCallback?.invoke(error)
                    })

            disposables.add(topicSubscription)

            // Подключаемся (токен уже в URL, заголовки передаем через connect)
            // STOMP библиотека принимает List<StompHeader>? или можно передать null
            val headers =
                listOf(
                    ua.naiksoftware.stomp.dto.StompHeader("Authorization", "Bearer $token"),
                )
            stompClient!!.connect(headers)
        } catch (e: Exception) {
            Log.e("QueueWebSocket", "Connection error", e)
            onErrorCallback?.invoke(e)
        }
    }

    fun disconnect() {
        try {
            stompClient?.disconnect()
            disposables.clear()
            Log.d("QueueWebSocket", "Disconnected")
        } catch (e: Exception) {
            Log.e("QueueWebSocket", "Disconnect error", e)
        }
    }

    fun initializeQueue() {
        // Используем небольшую задержку, чтобы убедиться, что подключение полностью установлено
        val sendSubscription =
            io.reactivex.Observable.timer(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (stompClient?.isConnected == true) {
                        val message = gson.toJson(mapOf<String, Any>())
                        val initSubscription =
                            stompClient?.send("/app/queue/init", message)
                                ?.subscribeOn(Schedulers.io())
                                ?.observeOn(AndroidSchedulers.mainThread())
                                ?.subscribe(
                                    { Log.d("QueueWebSocket", "Init message sent") },
                                    { error ->
                                        Log.e("QueueWebSocket", "Error sending init", error)
                                        onErrorCallback?.invoke(error)
                                    },
                                )
                        initSubscription?.let { disposables.add(it) }
                    } else {
                        Log.w("QueueWebSocket", "Not connected after delay, cannot send init")
                    }
                }, { error ->
                    Log.e("QueueWebSocket", "Error in timer", error)
                })
        sendSubscription?.let { disposables.add(it) }
    }

    fun getPosition(doctorId: Long) {
        if (stompClient?.isConnected == true) {
            val message = gson.toJson(mapOf("doctorId" to doctorId))
            val sendSubscription =
                stompClient?.send("/app/queue/position", message)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe(
                        { Log.d("QueueWebSocket", "Position request sent for doctor $doctorId") },
                        { error ->
                            Log.e("QueueWebSocket", "Error sending position request", error)
                            onErrorCallback?.invoke(error)
                        },
                    )
            sendSubscription?.let { disposables.add(it) }
        } else {
            Log.w("QueueWebSocket", "Not connected, cannot request position")
        }
    }

    fun getMyQueues() {
        // Используем небольшую задержку, чтобы убедиться, что подключение полностью установлено
        val sendSubscription =
            io.reactivex.Observable.timer(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (stompClient?.isConnected == true) {
                        val message = gson.toJson(mapOf<String, Any>())
                        val queuesSubscription =
                            stompClient?.send("/app/queue/my-queues", message)
                                ?.subscribeOn(Schedulers.io())
                                ?.observeOn(AndroidSchedulers.mainThread())
                                ?.subscribe(
                                    { Log.d("QueueWebSocket", "My queues request sent") },
                                    { error ->
                                        Log.e("QueueWebSocket", "Error sending my-queues request", error)
                                        onErrorCallback?.invoke(error)
                                    },
                                )
                        queuesSubscription?.let { disposables.add(it) }
                    } else {
                        Log.w("QueueWebSocket", "Not connected after delay, cannot request queues")
                    }
                }, { error ->
                    Log.e("QueueWebSocket", "Error in timer", error)
                })
        sendSubscription?.let { disposables.add(it) }
    }

    fun isConnected(): Boolean {
        return stompClient?.isConnected == true
    }

    /**
     * Подписывается на обновления очереди для конкретного врача
     * @param doctorId ID врача
     */
    fun subscribeToQueueUpdates(doctorId: Long) {
        if (stompClient?.isConnected == true) {
            // Отписываемся от предыдущей подписки, если она есть
            unsubscribeFromQueueUpdates(doctorId)

            val topic = "/topic/queue/doctor/$doctorId"
            Log.d("QueueWebSocket", "Subscribing to queue updates for doctor $doctorId: $topic")

            val subscription =
                stompClient!!.topic(topic)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ message: StompMessage ->
                        try {
                            Log.d("QueueWebSocket", "Received raw message for doctor $doctorId: ${message.payload}")
                            val queueUpdate = gson.fromJson(message.payload, QueueUpdate::class.java)
                            Log.d(
                                "QueueWebSocket",
                                "Parsed queue update for doctor ${queueUpdate.doctorId}: ${queueUpdate.queue.size} entries",
                            )
                            onQueueUpdateCallback?.invoke(queueUpdate)
                        } catch (e: Exception) {
                            Log.e("QueueWebSocket", "Error parsing queue update for doctor $doctorId", e)
                            e.printStackTrace()
                            onErrorCallback?.invoke(e)
                        }
                    }, { error: Throwable ->
                        Log.e("QueueWebSocket", "Queue update subscription error for doctor $doctorId", error)
                        error.printStackTrace()
                        onErrorCallback?.invoke(error)
                    })

            queueSubscriptions[doctorId] = subscription
            disposables.add(subscription)
            Log.d("QueueWebSocket", "Successfully subscribed to $topic. Total subscriptions: ${queueSubscriptions.size}")
        } else {
            Log.w(
                "QueueWebSocket",
                "Not connected, cannot subscribe to queue updates for doctor $doctorId. isConnected=${stompClient?.isConnected}",
            )
        }
    }

    /**
     * Отписывается от обновлений очереди для конкретного врача
     * @param doctorId ID врача
     */
    fun unsubscribeFromQueueUpdates(doctorId: Long) {
        queueSubscriptions[doctorId]?.let { subscription ->
            if (!subscription.isDisposed) {
                subscription.dispose()
                Log.d("QueueWebSocket", "Unsubscribed from queue updates for doctor $doctorId")
            }
            queueSubscriptions.remove(doctorId)
        }
    }

    /**
     * Отписывается от всех обновлений очередей
     */
    fun unsubscribeFromAllQueueUpdates() {
        queueSubscriptions.keys.toList().forEach { doctorId ->
            unsubscribeFromQueueUpdates(doctorId)
        }
    }
}
