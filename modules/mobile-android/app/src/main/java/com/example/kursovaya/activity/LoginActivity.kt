package com.example.kursovaya.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.kursovaya.R
import com.example.kursovaya.model.AuthState
import com.example.kursovaya.repository.AuthApiRepository
import com.example.kursovaya.repository.AuthRepository
import com.example.kursovaya.repository.UserDataRepository
import com.example.kursovaya.repository.UserRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    private lateinit var authRepository: AuthRepository
    private val authApiRepository = AuthApiRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        authRepository = AuthRepository(this)
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val registration = findViewById<TextView>(R.id.textViewSignUp)
        val loginBtn = findViewById<Button>(R.id.buttonLogin)

        loginBtn.setOnClickListener { view ->
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Проверяем, не пустые ли поля
            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(view, "Пожалуйста, заполните все поля", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Блокируем кнопку во время запроса
            loginBtn.isEnabled = false
            loginBtn.text = "Вход..."

            // Выполняем запрос на авторизацию
            lifecycleScope.launch {
                authApiRepository.login(email, password)
                    .onSuccess { loginResponse ->
                        Log.d("LoginActivity", "Получен токен: ${loginResponse.token.take(20)}...")

                        // Сохраняем токен синхронно
                        authRepository.saveAuthToken(loginResponse.token)
                        Log.d("LoginActivity", "Токен сохранен")

                        // Проверяем, что токен сохранился
                        val savedToken = authRepository.getAuthState()
                        Log.d(
                            "LoginActivity",
                            "Проверка токена: ${savedToken.javaClass.simpleName}",
                        )

                        if (savedToken is AuthState.Authenticated) {
                            Log.d(
                                "LoginActivity",
                                "Токен подтвержден, загрузка данных пользователя...",
                            )

                            // Инициализируем UserDataRepository
                            UserDataRepository.init(this@LoginActivity)

                            // Загружаем данные пользователя
                            val userRepository = UserRepository(this@LoginActivity)
                            userRepository.getCurrentUser()
                                .onSuccess { user ->
                                    Log.d(
                                        "LoginActivity",
                                        "Данные пользователя загружены: ${user.email}",
                                    )
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Успешный вход!",
                                        Toast.LENGTH_SHORT,
                                    ).show()

                                    val intent =
                                        Intent(this@LoginActivity, MainActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .onFailure { error ->
                                    Log.e(
                                        "LoginActivity",
                                        "Ошибка загрузки данных пользователя",
                                        error,
                                    )
                                    // Все равно переходим в MainActivity, данные загрузятся там
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Успешный вход!",
                                        Toast.LENGTH_SHORT,
                                    ).show()

                                    val intent =
                                        Intent(this@LoginActivity, MainActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                        } else {
                            // Если токен не сохранился, показываем ошибку
                            Log.e("LoginActivity", "Токен не сохранился!")
                            loginBtn.isEnabled = true
                            loginBtn.text = "Войти"
                            Snackbar.make(view, "Ошибка сохранения токена", Snackbar.LENGTH_LONG)
                                .show()
                        }
                    }
                    .onFailure { error ->
                        Log.e("LoginActivity", "Ошибка входа: ${error.message}", error)
                        // Разблокируем кнопку
                        loginBtn.isEnabled = true
                        loginBtn.text = "Войти"

                        val errorMessage = error.message ?: "Ошибка входа"
                        Snackbar.make(view, errorMessage, Snackbar.LENGTH_LONG).show()
                    }
            }
        }

        registration.setOnClickListener { view ->
            val intent = Intent(this, RegistrationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }
}
