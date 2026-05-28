package com.example.kursovaya.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.kursovaya.databinding.ActivityRegistrationStep2Binding
import com.example.kursovaya.model.AuthState
import com.example.kursovaya.repository.AuthApiRepository
import com.example.kursovaya.repository.AuthRepository
import com.google.android.material.snackbar.Snackbar
import com.redmadrobot.inputmask.MaskedTextChangedListener
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class RegistrationStep2Activity : BaseActivity() {
    private lateinit var binding: ActivityRegistrationStep2Binding
    private val authApiRepository = AuthApiRepository()
    private lateinit var authRepository: AuthRepository

    private var email: String = ""
    private var phone: String = ""
    private var password: String = ""
    private var confirmPassword: String = ""
    private var fio: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationStep2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(this)

        // Получаем данные из предыдущего шага
        email = intent.getStringExtra("email") ?: ""
        phone = intent.getStringExtra("phone") ?: ""
        password = intent.getStringExtra("password") ?: ""
        confirmPassword = intent.getStringExtra("confirmPassword") ?: ""
        fio = intent.getStringExtra("fio") ?: ""

        // Проверяем, что данные переданы
        if (email.isEmpty() || phone.isEmpty() || password.isEmpty() || fio.isEmpty()) {
            Toast.makeText(this, "Ошибка: данные не переданы", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupSnilsMask()
        setupDatePicker()
        setupErrorClearing()

        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonCompleteRegistration.setOnClickListener { view ->
            if (validateForm()) {
                val birthDate = binding.editTextBirthDate.text.toString().trim()
                val selectedGenderId = binding.radioGroupGender.checkedRadioButtonId

                // Получаем пол (1 = мужской, 2 = женский)
                val gender: Short =
                    if (selectedGenderId == binding.radioButtonMale.id) {
                        1
                    } else {
                        2
                    }

                // Форматируем дату в YYYY-MM-DD
                val formattedBirthDate = formatDateToApiFormat(birthDate)
                if (formattedBirthDate == null) {
                    binding.birthDateInputLayout.error = "Неверный формат даты"
                    return@setOnClickListener
                }

                // Получаем СНИЛС (убираем форматирование)
                val insuranceNumberFormatted = binding.editTextInsuranceNumber.text.toString().trim()
                val insuranceNumber =
                    if (insuranceNumberFormatted.isNotEmpty()) {
                        insuranceNumberFormatted.replace("[^0-9]".toRegex(), "")
                    } else {
                        null
                    }

                // Выполняем регистрацию через API
                binding.buttonCompleteRegistration.isEnabled = false
                binding.buttonCompleteRegistration.text = "Регистрация..."

                lifecycleScope.launch {
                    authApiRepository.registerWithPatient(
                        email = email,
                        phone = phone,
                        password = password,
                        confirmPassword = confirmPassword,
                        fio = fio,
                        birthDate = formattedBirthDate,
                        gender = gender,
                        insuranceNumber = insuranceNumber,
                        emergencyContact = null,
                    )
                        .onSuccess { response ->
                            // Сохраняем токен после успешной регистрации синхронно
                            authRepository.saveAuthToken(response.token)

                            // Проверяем, что токен сохранился
                            val savedToken = authRepository.getAuthState()
                            if (savedToken is AuthState.Authenticated) {
                                Toast.makeText(
                                    this@RegistrationStep2Activity,
                                    "Регистрация успешна!",
                                    Toast.LENGTH_SHORT,
                                ).show()

                                // Переходим в MainActivity, так как пользователь уже авторизован
                                val intent = Intent(this@RegistrationStep2Activity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                startActivity(intent)
                                finish()
                            } else {
                                // Если токен не сохранился, показываем ошибку
                                binding.buttonCompleteRegistration.isEnabled = true
                                binding.buttonCompleteRegistration.text = "Завершить регистрацию"
                                Snackbar.make(view, "Ошибка сохранения токена", Snackbar.LENGTH_LONG).show()
                            }
                        }
                        .onFailure { error ->
                            // Разблокируем кнопку
                            binding.buttonCompleteRegistration.isEnabled = true
                            binding.buttonCompleteRegistration.text = "Завершить регистрацию"

                            val errorMessage = error.message ?: "Ошибка регистрации"
                            Snackbar.make(view, errorMessage, Snackbar.LENGTH_LONG).show()
                        }
                }
            }
        }
    }

    /**
     * Настройка маски для ввода СНИЛС в формате XXX-XXX-XXX XX
     */
    private fun setupSnilsMask() {
        MaskedTextChangedListener.installOn(
            editText = binding.editTextInsuranceNumber,
            primaryFormat = "[000]-[000]-[000] [00]",
            autocomplete = true,
        )

        // Очищаем ошибку при вводе
        binding.editTextInsuranceNumber.addTextChangedListener(
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
                    binding.insuranceNumberInputLayout.error = null
                }
            },
        )
    }

    /**
     * Настройка DatePicker для выбора даты рождения
     */
    private fun setupDatePicker() {
        binding.editTextBirthDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    /**
     * Настройка автоматической очистки ошибок
     */
    private fun setupErrorClearing() {
        binding.editTextBirthDate.addTextChangedListener(
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
                    binding.birthDateInputLayout.error = null
                }
            },
        )
    }

    /**
     * Валидация формы
     */
    private fun validateForm(): Boolean {
        val birthDate = binding.editTextBirthDate.text.toString().trim()
        val selectedGenderId = binding.radioGroupGender.checkedRadioButtonId
        val insuranceNumber = binding.editTextInsuranceNumber.text.toString().trim()

        var hasErrors = false

        // Очистка ошибок
        clearErrors()

        // Валидация даты рождения
        if (birthDate.isEmpty()) {
            binding.birthDateInputLayout.error = "Выберите дату рождения"
            hasErrors = true
        } else {
            val birthDateError = validateBirthDate(birthDate)
            if (birthDateError != null) {
                binding.birthDateInputLayout.error = birthDateError
                hasErrors = true
            }
        }

        // Валидация пола
        if (selectedGenderId == -1) {
            Snackbar.make(binding.root, "Пожалуйста, выберите пол", Snackbar.LENGTH_SHORT).show()
            hasErrors = true
        }

        // Валидация СНИЛС (если указан)
        if (insuranceNumber.isNotEmpty()) {
            val snilsError = validateSnils(insuranceNumber)
            if (snilsError != null) {
                binding.insuranceNumberInputLayout.error = snilsError
                hasErrors = true
            }
        }

        return !hasErrors
    }

    /**
     * Очистка всех ошибок
     */
    private fun clearErrors() {
        binding.birthDateInputLayout.error = null
        binding.insuranceNumberInputLayout.error = null
    }

    /**
     * Валидация даты рождения
     */
    private fun validateBirthDate(birthDate: String): String? {
        return try {
            // Пробуем разные форматы
            val date =
                try {
                    // Формат DD/MM/YYYY
                    val parts = birthDate.split("/")
                    if (parts.size == 3) {
                        LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                    } else {
                        // Формат YYYY-MM-DD
                        LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    }
                } catch (e: Exception) {
                    return "Некорректный формат даты"
                }

            val now = LocalDate.now()

            // Проверка на будущую дату
            if (date.isAfter(now)) {
                return "Дата рождения не может быть в будущем"
            }

            // Проверка на слишком старую дату (более 150 лет)
            if (date.isBefore(now.minusYears(150))) {
                return "Некорректная дата рождения"
            }

            null
        } catch (e: Exception) {
            "Некорректный формат даты"
        }
    }

    /**
     * Валидация СНИЛС
     * Формат: XXX-XXX-XXX XX или XXXXXXXXXXX (11 цифр)
     */
    private fun validateSnils(snils: String): String? {
        val digitsOnly = snils.replace("[^0-9]".toRegex(), "")

        if (digitsOnly.length != 11) {
            return "СНИЛС должен содержать 11 цифр"
        }

        return null
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()

        // Парсим текущую дату, если она есть
        val currentDate = binding.editTextBirthDate.text?.toString()
        if (!currentDate.isNullOrEmpty()) {
            try {
                val parts = currentDate.split("/")
                if (parts.size == 3) {
                    calendar.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
                }
            } catch (e: Exception) {
                // Если не удалось распарсить, используем текущую дату
            }
        }

        val datePickerDialog =
            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Форматируем дату для отображения
                    val selectedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    binding.editTextBirthDate.setText(selectedDate)
                    binding.birthDateInputLayout.error = null
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
            )

        // Устанавливаем максимальную дату (сегодня)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        // Устанавливаем минимальную дату (150 лет назад)
        val minCalendar = Calendar.getInstance()
        minCalendar.add(Calendar.YEAR, -150)
        datePickerDialog.datePicker.minDate = minCalendar.timeInMillis

        datePickerDialog.show()
    }

    /**
     * Конвертирует дату из формата DD/MM/YYYY в YYYY-MM-DD для API
     */
    private fun formatDateToApiFormat(dateString: String): String? {
        return try {
            val parts = dateString.split("/")
            if (parts.size == 3) {
                val day = parts[0].padStart(2, '0')
                val month = parts[1].padStart(2, '0')
                val year = parts[2]
                "$year-$month-$day"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
