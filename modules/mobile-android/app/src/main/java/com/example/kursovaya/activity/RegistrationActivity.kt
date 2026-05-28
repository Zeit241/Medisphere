package com.example.kursovaya.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import com.example.kursovaya.databinding.ActivityRegistrationBinding
import com.google.android.material.textfield.TextInputLayout
import com.redmadrobot.inputmask.MaskedTextChangedListener

class RegistrationActivity : BaseActivity() {
    companion object {
        private const val MIN_NAME_LENGTH = 2
        private const val MIN_PASSWORD_LENGTH = 6
    }

    private lateinit var binding: ActivityRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPhoneMask()
        setupErrorClearing()

        binding.checkboxTerms.setOnCheckedChangeListener { _, isChecked ->
            binding.buttonRegister.isEnabled = isChecked
        }

        binding.buttonBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.textViewLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.buttonRegister.setOnClickListener {
            if (validateForm()) {
                val fullName = binding.editTextFullName.text.toString().trim()
                val email = binding.editTextEmail.text.toString().trim()
                val phoneFormatted = binding.editTextPhone.text.toString().trim()
                val password = binding.editTextPassword.text.toString().trim()
                val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

                // Нормализуем телефон для отправки
                val phone = normalizePhone(phoneFormatted)

                // Передаем данные на следующий шаг регистрации
                val intent = Intent(this, RegistrationStep2Activity::class.java)
                intent.putExtra("email", email)
                intent.putExtra("phone", phone)
                intent.putExtra("password", password)
                intent.putExtra("confirmPassword", confirmPassword)
                intent.putExtra("fio", fullName)
                startActivity(intent)
            }
        }
    }

    /**
     * Настройка маски для ввода телефона в формате +7 (XXX) XXX-XX-XX
     */
    private fun setupPhoneMask() {
        MaskedTextChangedListener.installOn(
            editText = binding.editTextPhone,
            primaryFormat = "+7 ([000]) [000]-[00]-[00]",
            autocomplete = true,
        )

        // Обработка ввода 8 в начале - заменяем на +7
        binding.editTextPhone.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {}

                override fun afterTextChanged(s: Editable?) {
                    val text = s?.toString() ?: ""
                    if (text.startsWith("8") && text.length > 1) {
                        val newText = "+7" + text.substring(1)
                        binding.editTextPhone.removeTextChangedListener(this)
                        binding.editTextPhone.setText(newText)
                        binding.editTextPhone.setSelection(newText.length)
                        binding.editTextPhone.addTextChangedListener(this)
                    }
                    binding.phoneInputLayout.error = null
                }
            },
        )
    }

    /**
     * Настройка автоматической очистки ошибок при вводе
     */
    private fun setupErrorClearing() {
        binding.editTextFullName.addTextChangedListener(createErrorClearingWatcher(binding.fullNameInputLayout))
        binding.editTextEmail.addTextChangedListener(createErrorClearingWatcher(binding.emailInputLayout))
        binding.editTextPassword.addTextChangedListener(createErrorClearingWatcher(binding.passwordInputLayout))
        binding.editTextConfirmPassword.addTextChangedListener(createErrorClearingWatcher(binding.confirmPasswordInputLayout))
    }

    private fun createErrorClearingWatcher(layout: TextInputLayout): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int,
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int,
            ) {}

            override fun afterTextChanged(s: Editable?) {
                layout.error = null
            }
        }
    }

    /**
     * Валидация всей формы
     */
    private fun validateForm(): Boolean {
        val fullName = binding.editTextFullName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

        var hasErrors = false

        // Очистка ошибок
        clearErrors()

        // Валидация ФИО (минимум имя и фамилия)
        val fioError = validateFullName(fullName)
        if (fioError != null) {
            binding.fullNameInputLayout.error = fioError
            hasErrors = true
        }

        // Валидация email
        val emailError = validateEmail(email)
        if (emailError != null) {
            binding.emailInputLayout.error = emailError
            hasErrors = true
        }

        // Валидация телефона
        val phoneError = validatePhone(phone)
        if (phoneError != null) {
            binding.phoneInputLayout.error = phoneError
            hasErrors = true
        }

        // Валидация пароля
        val passwordError = validatePassword(password)
        if (passwordError != null) {
            binding.passwordInputLayout.error = passwordError
            hasErrors = true
        }

        // Валидация подтверждения пароля
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInputLayout.error = "Подтвердите пароль"
            hasErrors = true
        } else if (password != confirmPassword) {
            binding.confirmPasswordInputLayout.error = "Пароли не совпадают"
            hasErrors = true
        }

        // Фокус на первое поле с ошибкой
        if (hasErrors) {
            val firstErrorField =
                when {
                    binding.fullNameInputLayout.error != null -> binding.editTextFullName
                    binding.emailInputLayout.error != null -> binding.editTextEmail
                    binding.phoneInputLayout.error != null -> binding.editTextPhone
                    binding.passwordInputLayout.error != null -> binding.editTextPassword
                    binding.confirmPasswordInputLayout.error != null -> binding.editTextConfirmPassword
                    else -> null
                }
            firstErrorField?.requestFocus()
        }

        return !hasErrors
    }

    /**
     * Очистка всех ошибок
     */
    private fun clearErrors() {
        binding.fullNameInputLayout.error = null
        binding.emailInputLayout.error = null
        binding.phoneInputLayout.error = null
        binding.passwordInputLayout.error = null
        binding.confirmPasswordInputLayout.error = null
    }

    /**
     * Валидация ФИО - минимум имя и фамилия
     */
    private fun validateFullName(fullName: String): String? {
        if (fullName.isEmpty()) {
            return "Введите ФИО"
        }

        val parts = fullName.split(" ").filter { it.isNotBlank() }

        if (parts.size < 2) {
            return "Введите минимум фамилию и имя"
        }

        // Проверяем каждую часть
        for ((index, part) in parts.withIndex()) {
            val partName =
                when (index) {
                    0 -> "Фамилия"
                    1 -> "Имя"
                    else -> "Отчество"
                }

            if (part.length < MIN_NAME_LENGTH) {
                return "$partName должна содержать минимум $MIN_NAME_LENGTH символа"
            }

            if (!part.matches("^[а-яА-ЯёЁa-zA-Z\\-]+$".toRegex())) {
                return "$partName может содержать только буквы и дефис"
            }
        }

        return null
    }

    /**
     * Валидация email
     */
    private fun validateEmail(email: String): String? {
        if (email.isEmpty()) {
            return "Введите email"
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Некорректный формат email"
        }

        return null
    }

    /**
     * Валидация телефона
     */
    private fun validatePhone(phone: String): String? {
        if (phone.isEmpty()) {
            return "Введите номер телефона"
        }

        val cleanPhone = phone.replace("[^0-9+]".toRegex(), "")

        if (!cleanPhone.startsWith("+7") || cleanPhone.length != 12) {
            return "Некорректный формат телефона"
        }

        return null
    }

    /**
     * Валидация пароля
     */
    private fun validatePassword(password: String): String? {
        if (password.isEmpty()) {
            return "Введите пароль"
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            return "Пароль должен содержать минимум $MIN_PASSWORD_LENGTH символов"
        }

        return null
    }

    /**
     * Нормализация телефона для отправки на сервер
     */
    private fun normalizePhone(phoneFormatted: String): String {
        val phoneDigits = phoneFormatted.replace("[^0-9]".toRegex(), "")
        return when {
            phoneDigits.startsWith("8") -> "+7" + phoneDigits.substring(1)
            phoneDigits.startsWith("7") -> "+7" + phoneDigits.substring(1)
            else -> "+7$phoneDigits"
        }
    }
}
