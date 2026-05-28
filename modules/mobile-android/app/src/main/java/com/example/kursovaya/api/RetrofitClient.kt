package com.example.kursovaya.api

import android.content.Context
import android.util.Log
import com.example.kursovaya.BuildConfig
import com.example.kursovaya.auth.AuthSessionCoordinator
import com.example.kursovaya.model.AuthState
import com.example.kursovaya.repository.AuthRepository
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8085/" // Для эмулятора Android
    // Для реального устройства используйте: "http://YOUR_IP:8085/"

    private var context: Context? = null
    private var retrofitInstance: Retrofit? = null

    fun init(context: Context) {
        this.context = context.applicationContext
        // Пересоздаем Retrofit при инициализации
        retrofitInstance = null
    }

    // Получаем актуальный AuthRepository при каждом запросе
    private fun getAuthRepository(): AuthRepository? {
        val ctx = context
        return if (ctx != null) {
            AuthRepository(ctx)
        } else {
            Log.e("RetrofitClient", "Context is null! Нужно вызвать RetrofitClient.init(context)")
            null
        }
    }

    // Interceptor для добавления JWT токена в заголовки
    private fun createAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()

            // Получаем актуальный authRepository при каждом запросе
            val authRepo = getAuthRepository()
            val token =
                authRepo?.getAuthState()?.let { state ->
                    if (state is AuthState.Authenticated) {
                        Log.d("RetrofitClient", "Токен найден: ${state.token.take(20)}...")
                        state.token
                    } else {
                        Log.w("RetrofitClient", "Пользователь не авторизован")
                        null
                    }
                } ?: run {
                    Log.e("RetrofitClient", "authRepository is null или токен не найден для ${originalRequest.url}")
                    null
                }

            val newRequest =
                if (token != null) {
                    val requestWithAuth =
                        originalRequest.newBuilder()
                            .header("Authorization", "Bearer $token")
                            .build()
                    Log.d("RetrofitClient", "Добавлен заголовок Authorization для ${originalRequest.url}")
                    requestWithAuth
                } else {
                    Log.e("RetrofitClient", "Запрос без токена: ${originalRequest.url}")
                    originalRequest
                }

            chain.proceed(newRequest)
        }
    }

    /** 401 с Bearer → очистка сессии и редирект через [AuthSessionCoordinator]. */
    private fun createUnauthorizedInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code != 401) return@Interceptor response

            val hadAuth = !request.header("Authorization").isNullOrBlank()
            val path = request.url.encodedPath
            val isPublicAuthPath =
                path.contains("/api/auth/login", ignoreCase = true) ||
                    path.contains("/api/auth/register", ignoreCase = true)

            if (hadAuth && !isPublicAuthPath) {
                context?.let { AuthSessionCoordinator.notifyUnauthorized(it) }
            }
            response
        }
    }

    /**
     * Level.BODY буферизует всё тело ответа в String — при больших JSON (например фото в base64)
     * легко получить OutOfMemoryError. Для отладки достаточно HEADERS.
     */
    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level =
                if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.HEADERS
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
        }

    private val retrofit: Retrofit
        get() {
            if (retrofitInstance == null) {
                val okHttpClient =
                    OkHttpClient.Builder()
                        .addInterceptor(createAuthInterceptor())
                        .addInterceptor(createUnauthorizedInterceptor())
                        .addInterceptor(loggingInterceptor)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build()

                retrofitInstance =
                    Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
            }
            return retrofitInstance!!
        }

    val authApi: AuthApi
        get() = retrofit.create(AuthApi::class.java)

    val doctorsApi: DoctorsApi
        get() = retrofit.create(DoctorsApi::class.java)

    val userApi: UserApi
        get() = retrofit.create(UserApi::class.java)

    val appointmentApi: AppointmentApi
        get() = retrofit.create(AppointmentApi::class.java)

    val patientApi: PatientApi
        get() = retrofit.create(PatientApi::class.java)

    val reviewApi: ReviewApi
        get() = retrofit.create(ReviewApi::class.java)

    val dashboardApi: DashboardApi
        get() = retrofit.create(DashboardApi::class.java)

    val catalogApi: CatalogApi
        get() = retrofit.create(CatalogApi::class.java)

    val clinicServicesApi: ClinicServicesApi
        get() = retrofit.create(ClinicServicesApi::class.java)
}
