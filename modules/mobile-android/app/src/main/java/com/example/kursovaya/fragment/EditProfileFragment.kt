package com.example.kursovaya.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.kursovaya.databinding.FragmentEditProfileBinding
import com.example.kursovaya.model.api.UpdatePatientRequest
import com.example.kursovaya.model.api.UpdateUserRequest
import com.example.kursovaya.model.api.User
import com.example.kursovaya.repository.PatientRepository
import com.example.kursovaya.repository.UserDataRepository
import com.example.kursovaya.repository.UserRepository
import com.redmadrobot.inputmask.MaskedTextChangedListener
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.regex.Pattern

class EditProfileFragment : Fragment() {
    companion object {
        // Паттерн для телефона: +7XXXXXXXXXX или 8XXXXXXXXXX или международный формат
        private val PHONE_PATTERN = Pattern.compile("^(\\+7|8)?[\\s-]?\\(?\\d{3}\\)?[\\s-]?\\d{3}[\\s-]?\\d{2}[\\s-]?\\d{2}$")

        // Паттерн для СНИЛС: XXX-XXX-XXX XX или XXXXXXXXXXX
        private val SNILS_PATTERN = Pattern.compile("^\\d{3}-?\\d{3}-?\\d{3}\\s?\\d{2}$")

        // Минимальная длина имени/фамилии
        private const val MIN_NAME_LENGTH = 3
    }

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var patientRepository: PatientRepository
    private lateinit var userRepository: UserRepository
    private var patientId: Long? = null

    private val genderOptions = listOf("Мужской", "Женский")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        com.example.kursovaya.api.RetrofitClient.init(requireContext())
        patientRepository = PatientRepository(requireContext())
        userRepository = UserRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveProfile()
        }

        setupGenderSpinner()
        setupDatePicker()
        setupPhoneMask()
        setupSnilsMask()
        setupErrorClearing()
        loadUserData()
    }

    /**
     * Настройка автоматической очистки ошибок при вводе
     */
    private fun setupErrorClearing() {
        binding.emailEditText.addTextChangedListener(createErrorClearingWatcher(binding.emailInputLayout))
        binding.phoneEditText.addTextChangedListener(createErrorClearingWatcher(binding.phoneInputLayout))
        binding.lastNameEditText.addTextChangedListener(createErrorClearingWatcher(binding.lastNameInputLayout))
        binding.firstNameEditText.addTextChangedListener(createErrorClearingWatcher(binding.firstNameInputLayout))
        binding.birthDateEditText.addTextChangedListener(createErrorClearingWatcher(binding.birthDateInputLayout))

        // Для AutoCompleteTextView используем другой подход
        binding.genderSpinner.setOnItemClickListener { _, _, _, _ ->
            binding.genderInputLayout.error = null
        }
    }

    private fun createErrorClearingWatcher(layout: com.google.android.material.textfield.TextInputLayout): android.text.TextWatcher {
        return object : android.text.TextWatcher {
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

            override fun afterTextChanged(s: android.text.Editable?) {
                layout.error = null
            }
        }
    }

    /**
     * Настройка DatePicker для выбора даты рождения
     */
    private fun setupDatePicker() {
        binding.birthDateEditText.setOnClickListener {
            showDatePicker()
        }

        binding.birthDateEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showDatePicker()
            }
        }

        // Делаем поле только для чтения, чтобы пользователь не мог вводить вручную
        binding.birthDateEditText.isFocusable = true
        binding.birthDateEditText.isClickable = true
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Парсим текущую дату, если она есть
        val currentDate = binding.birthDateEditText.text?.toString()
        if (!currentDate.isNullOrEmpty()) {
            try {
                val date = LocalDate.parse(currentDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                calendar.set(date.year, date.monthValue - 1, date.dayOfMonth)
            } catch (e: Exception) {
                // Если не удалось распарсить, используем текущую дату
            }
        }

        val datePickerDialog =
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    binding.birthDateEditText.setText(formattedDate)
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
     * Настройка маски для ввода телефона в формате +7 (XXX) XXX-XX-XX
     */
    private fun setupPhoneMask() {
        // Библиотека сама управляет inputType
        MaskedTextChangedListener.installOn(
            editText = binding.phoneEditText,
            primaryFormat = "+7 ([000]) [000]-[00]-[00]",
            autocomplete = true,
        )

        // Обработка ввода 8 в начале - заменяем на +7
        binding.phoneEditText.addTextChangedListener(
            object : android.text.TextWatcher {
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

                override fun afterTextChanged(s: android.text.Editable?) {
                    val text = s?.toString() ?: ""
                    if (text.startsWith("8") && text.length > 1) {
                        // Заменяем 8 на +7
                        val newText = "+7" + text.substring(1)
                        binding.phoneEditText.removeTextChangedListener(this)
                        binding.phoneEditText.setText(newText)
                        binding.phoneEditText.setSelection(newText.length)
                        binding.phoneEditText.addTextChangedListener(this)
                    }
                    // Очищаем ошибку при вводе
                    binding.phoneInputLayout.error = null
                }
            },
        )
    }

    /**
     * Настройка маски для ввода СНИЛС в формате XXX-XXX-XXX XX
     */
    private fun setupSnilsMask() {
        // В библиотеке input-mask формат: [000] - три цифры
        // Пробел в маске нужно указывать как обычный символ
        // Библиотека сама управляет inputType
        MaskedTextChangedListener.installOn(
            editText = binding.insuranceNumberEditText,
            primaryFormat = "[000]-[000]-[000] [00]",
            autocomplete = true,
        )

        // Очищаем ошибку при вводе
        binding.insuranceNumberEditText.addTextChangedListener(
            object : android.text.TextWatcher {
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

                override fun afterTextChanged(s: android.text.Editable?) {
                    binding.insuranceNumberInputLayout.error = null
                }
            },
        )
    }

    private fun setupGenderSpinner() {
        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                genderOptions,
            )
        binding.genderSpinner.setAdapter(adapter)

        // Открываем выпадающий список при клике
        binding.genderSpinner.setOnClickListener {
            binding.genderSpinner.showDropDown()
        }

        // Очищаем ошибку при выборе
        binding.genderSpinner.setOnItemClickListener { _, _, position, _ ->
            binding.genderInputLayout.error = null
        }

        // Также открываем при фокусе
        binding.genderSpinner.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.genderSpinner.showDropDown()
            }
        }
    }

    private fun loadUserData() {
        // Сначала загружаем из кэша для быстрого отображения
        val cachedUser = UserDataRepository.getCurrentUser()
        if (cachedUser != null) {
            patientId = cachedUser.patientId
            fillForm(cachedUser)
            binding.saveButton.isEnabled = patientId != null
            Log.d("EditProfileFragment", "Данные загружены из кэша")
        } else {
            // Если кэша нет, загружаем с сервера
            binding.progressBar.visibility = View.VISIBLE
            binding.saveButton.isEnabled = false
        }

        // Обновляем данные с сервера в фоне
        lifecycleScope.launch {
            userRepository.getCurrentUser()
                .onSuccess { user ->
                    patientId = user.patientId
                    fillForm(user)
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = patientId != null
                }
                .onFailure { error ->
                    Log.e("EditProfileFragment", "Ошибка загрузки данных пользователя", error)
                    // Если кэш был загружен, не показываем ошибку
                    if (cachedUser == null) {
                        Toast.makeText(
                            requireContext(),
                            "Ошибка загрузки данных: ${error.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = cachedUser?.patientId != null
                }
        }
    }

    private fun fillForm(user: User) {
        binding.emailEditText.setText(user.email)

        // Для телефона: извлекаем только цифры, маска сама отформатирует
        val phoneDigits = user.phone.replace("[^0-9]".toRegex(), "")
        val phoneNormalized =
            if (phoneDigits.startsWith("8")) {
                phoneDigits.substring(1)
            } else if (phoneDigits.startsWith("7")) {
                phoneDigits.substring(1)
            } else {
                phoneDigits
            }
        // Устанавливаем +7 и цифры, маска отформатирует
        if (phoneNormalized.length == 10) {
            binding.phoneEditText.setText("+7$phoneNormalized")
        } else {
            binding.phoneEditText.setText(user.phone)
        }

        binding.lastNameEditText.setText(user.lastName)
        binding.firstNameEditText.setText(user.firstName)
        binding.middleNameEditText.setText(user.middleName ?: "")

        // Данные пациента из вложенного объекта
        val patient = user.patient
        if (patient != null) {
            binding.birthDateEditText.setText(patient.birthDate ?: "")
            // Устанавливаем выбранный пол: 1 = Мужской (индекс 0), 2 = Женский (индекс 1)
            val genderIndex = if (patient.gender == 1.toShort()) 0 else 1
            binding.genderSpinner.setText(genderOptions[genderIndex], false)
            // Для СНИЛС: устанавливаем только цифры, маска отформатирует
            val snilsDigits = (patient.insuranceNumber ?: "").replace("[^0-9]".toRegex(), "")
            if (snilsDigits.length == 11) {
                binding.insuranceNumberEditText.setText(snilsDigits)
            } else {
                binding.insuranceNumberEditText.setText(patient.insuranceNumber ?: "")
            }
        }
    }

    private fun saveProfile() {
        val patientId = this.patientId
        if (patientId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пациента не найден", Toast.LENGTH_SHORT).show()
            return
        }

        // Получаем значения полей
        val email = binding.emailEditText.text?.toString()?.trim() ?: ""
        val phoneFormatted = binding.phoneEditText.text?.toString()?.trim() ?: ""
        val lastName = binding.lastNameEditText.text?.toString()?.trim() ?: ""
        val firstName = binding.firstNameEditText.text?.toString()?.trim() ?: ""
        val middleName = binding.middleNameEditText.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val birthDate = binding.birthDateEditText.text?.toString()?.trim() ?: ""
        val insuranceNumberFormatted = binding.insuranceNumberEditText.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

        // Убираем форматирование для валидации и отправки
        // Для телефона: извлекаем только цифры и добавляем +7
        val phoneDigits = phoneFormatted.replace("[^0-9]".toRegex(), "")
        val phone =
            if (phoneDigits.isNotEmpty()) {
                if (phoneDigits.startsWith("8")) {
                    "+7" + phoneDigits.substring(1)
                } else if (phoneDigits.startsWith("7")) {
                    "+7" + phoneDigits.substring(1)
                } else {
                    "+7$phoneDigits"
                }
            } else {
                phoneFormatted
            }

        // Для СНИЛС: убираем все нецифровые символы
        val insuranceNumber = insuranceNumberFormatted?.replace("[^0-9]".toRegex(), "")

        // Валидация с подсветкой ошибок
        if (!validateForm(email, phoneFormatted, lastName, firstName, birthDate, insuranceNumberFormatted)) {
            // Прокручиваем к первому полю с ошибкой
            binding.root.post {
                val firstErrorField =
                    when {
                        binding.emailInputLayout.error != null -> binding.emailEditText
                        binding.phoneInputLayout.error != null -> binding.phoneEditText
                        binding.lastNameInputLayout.error != null -> binding.lastNameEditText
                        binding.firstNameInputLayout.error != null -> binding.firstNameEditText
                        binding.birthDateInputLayout.error != null -> binding.birthDateEditText
                        binding.insuranceNumberInputLayout.error != null -> binding.insuranceNumberEditText
                        else -> null
                    }
                firstErrorField?.requestFocus()
            }
            return
        }

        // Получаем выбранный пол из AutoCompleteTextView: 0 = Мужской (1), 1 = Женский (2)
        val selectedGender = binding.genderSpinner.text?.toString() ?: ""
        val selectedGenderIndex = genderOptions.indexOf(selectedGender)
        val gender = if (selectedGenderIndex == 0) 1.toShort() else 2.toShort()

        // Валидация пола
        if (selectedGenderIndex == -1) {
            binding.genderInputLayout.error = "Выберите пол"
            binding.root.post { binding.genderSpinner.requestFocus() }
            return
        }

        val request =
            UpdatePatientRequest(
                user =
                    UpdateUserRequest(
                        email = email,
                        phone = phone,
                        firstName = firstName,
                        lastName = lastName,
                        middleName = middleName,
                    ),
                birthDate = birthDate,
                gender = gender,
                insuranceNumber = insuranceNumber,
            )

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.saveButton.isEnabled = false

            patientRepository.updatePatient(patientId, request)
                .onSuccess { patient ->
                    Log.d("EditProfileFragment", "Профиль успешно обновлен")
                    Toast.makeText(requireContext(), "Профиль успешно обновлен", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                    // Возвращаемся назад
                    findNavController().navigateUp()
                }
                .onFailure { error ->
                    Log.e("EditProfileFragment", "Ошибка обновления профиля", error)
                    Toast.makeText(
                        requireContext(),
                        "Ошибка обновления: ${error.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                }
        }
    }

    /**
     * Валидация формы с подсветкой ошибок
     * @return true если валидация прошла, false если есть ошибки
     */
    private fun validateForm(
        email: String,
        phone: String,
        lastName: String,
        firstName: String,
        birthDate: String,
        insuranceNumber: String?,
    ): Boolean {
        var hasErrors = false

        // Очищаем предыдущие ошибки
        clearErrors()

        // Email
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Введите email"
            hasErrors = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Некорректный формат email"
            hasErrors = true
        }

        // Телефон
        if (phone.isEmpty()) {
            binding.phoneInputLayout.error = "Введите номер телефона"
            hasErrors = true
        } else {
            val cleanPhone = phone.replace("[^0-9+]".toRegex(), "")
            if (!cleanPhone.startsWith("+7") || cleanPhone.length != 12) {
                binding.phoneInputLayout.error = "Некорректный формат телефона"
                hasErrors = true
            }
        }

        // Фамилия
        if (lastName.isEmpty()) {
            binding.lastNameInputLayout.error = "Введите фамилию"
            hasErrors = true
        } else if (lastName.length < MIN_NAME_LENGTH) {
            binding.lastNameInputLayout.error = "Минимум $MIN_NAME_LENGTH символа"
            hasErrors = true
        } else if (!lastName.matches("^[а-яА-ЯёЁa-zA-Z\\-]+$".toRegex())) {
            binding.lastNameInputLayout.error = "Только буквы и дефис"
            hasErrors = true
        }

        // Имя
        if (firstName.isEmpty()) {
            binding.firstNameInputLayout.error = "Введите имя"
            hasErrors = true
        } else if (firstName.length < MIN_NAME_LENGTH) {
            binding.firstNameInputLayout.error = "Минимум $MIN_NAME_LENGTH символа"
            hasErrors = true
        } else if (!firstName.matches("^[а-яА-ЯёЁa-zA-Z\\-]+$".toRegex())) {
            binding.firstNameInputLayout.error = "Только буквы и дефис"
            hasErrors = true
        }

        // Дата рождения
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

        // СНИЛС (если указан)
        if (!insuranceNumber.isNullOrEmpty()) {
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
        binding.emailInputLayout.error = null
        binding.phoneInputLayout.error = null
        binding.lastNameInputLayout.error = null
        binding.firstNameInputLayout.error = null
        binding.birthDateInputLayout.error = null
        binding.genderInputLayout.error = null
        binding.insuranceNumberInputLayout.error = null
    }

    /**
     * Валидация даты рождения
     */
    private fun validateBirthDate(birthDate: String): String? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val date = LocalDate.parse(birthDate, formatter)
            val now = LocalDate.now()

            // Проверка на будущую дату
            if (date.isAfter(now)) {
                return "Дата рождения не может быть в будущем"
            }

            // Проверка на слишком старую дату (более 150 лет)
            if (date.isBefore(now.minusYears(150))) {
                return "Некорректная дата рождения"
            }

            // Проверка на возраст менее 0 лет (на всякий случай)
            if (date.isAfter(now)) {
                return "Дата рождения не может быть в будущем"
            }

            null
        } catch (e: DateTimeParseException) {
            "Некорректный формат даты. Используйте ГГГГ-ММ-ДД (например, 1990-01-15)"
        }
    }

    /**
     * Валидация СНИЛС
     * Формат: XXX-XXX-XXX XX или XXXXXXXXXXX (11 цифр)
     */
    private fun validateSnils(snils: String): String? {
        // Убираем все нецифровые символы
        val digitsOnly = snils.replace("[^0-9]".toRegex(), "")

        if (digitsOnly.length != 11) {
            return "СНИЛС должен содержать 11 цифр. Формат: XXX-XXX-XXX XX"
        }

        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
