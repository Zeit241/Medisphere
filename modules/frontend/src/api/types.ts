// Типы данных в соответствии с API документацией

export interface User {
	id: number;
	email: string;
	phone: string;
	firstName: string;
	lastName: string;
	middleName: string;
	createdAt: string;
	updatedAt: string;
	active: boolean;
	role?: string; // "patient", "doctor", "admin"
	patientId?: number | null;
	doctorId?: number | null;
	patient?: Patient | null;
	doctor?: Doctor | null;
}

export interface Patient {
	id: number;
	user: User;
	birthDate: string;
	gender: 1 | 2; // 1 = мужской, 2 = женский
	insuranceNumber: string | null;
	createdAt: string;
	updatedAt: string;
}

export interface Specialization {
	id: number;
	code: string;
	name: string;
	description: string;
}

export interface Room {
	id: number;
	code: string;
	name?: string;
}

export interface Diagnosis {
	id: number;
	code: string;
	name: string;
	category: string | null;
}

export interface AppointmentDiagnosisInfo {
	id: number;
	code: string;
	name: string;
	category: string | null;
}

export interface Doctor {
	id: number;
	user: User;
	displayName: string;
	bio: string;
	experienceYears: number;
	photo: string | null;
	rating: number | null;
	reviewCount: number;
	specializations: Specialization[];
	specialization: string | null;
	createdAt: string;
	updatedAt: string;
}

export interface Schedule {
	id: number;
	doctorId: number;
	roomId: number | null;
	dateAt: string; // YYYY-MM-DD
	startTime: string; // HH:mm:ss
	endTime: string; // HH:mm:ss
	slotDurationMinutes: number;
	createdAt: string;
	updatedAt: string;
}

export interface Appointment {
	id: number;
	scheduleId: number | null;
	doctorId: number;
	patientId: number | null;
	roomId: number | null;
	startTime: string; // ISO 8601
	endTime: string; // ISO 8601
	isBooked?: boolean;
	status: "scheduled" | "confirmed" | "in_progress" | "completed" | "cancelled" | "no_show" | "available";
	source: "mobile" | "reception" | "web" | "admin" | "schedule";
	createdBy: number | null;
	createdAt: string;
	updatedAt: string;
	cancelReason: string | null;
	/** Жалобы, анамнез, рекомендации (мед. карта приёма) */
	complaints?: string | null;
	anamnesis?: string | null;
	recommendations?: string | null;
	/** ID диагноза (МКБ) из БД */
	diagnosisId?: number | null;
	/** Вложенный объект из API; для обратной совместимости допускается строка в старых ответах */
	diagnosis?: AppointmentDiagnosisInfo | string | null;
	// Результат приёма (может отсутствовать в некоторых ответах)
	result?: string | null;
	doctor?: Doctor | null;
	patient?: Patient | null;
	room?: {
		id: number;
		code: string;
		name?: string;
	} | null;
	service?: {
		id: number;
		name: string;
		code?: string | null;
		price?: number | string | null;
		durationMinutes?: number | null;
	} | null;
}

/** Услуга клиники (не путать с глобальным DOM Service) */
export interface ClinicService {
	id: number;
	name: string;
	code?: string | null;
	price?: number | string | null;
	durationMinutes?: number | null;
	description?: string | null;
	/** Специализации (категории) из specialization_services */
	specializationNames?: string[] | null;
	/** ID специализаций (админка, формы редактирования) */
	specializationIds?: number[] | null;
}

export interface AvailableAppointmentSlot {
	id: number;
	scheduleId: number | null;
	doctorId: number;
	patientId: number | null;
	roomId: number | null;
	startTime: string; // ISO 8601
	endTime: string; // ISO 8601
	isBooked: boolean;
	status: string;
	source: string;
	createdBy: number | null;
	createdAt: string;
	updatedAt: string;
	cancelReason: string | null;
	diagnosis: string | null;
	serviceId?: number | null;
	service?: Pick<ClinicService, "id" | "name" | "code" | "durationMinutes"> | null;
}

export interface Queue {
	id?: number | null;
	doctorId: number;
	appointmentId?: number | null;
	patientId: number;
	position: number;
	lastUpdated: string;
}

/** Ответ POST .../appointments/:id/complete */
export interface CompleteAppointmentResponse {
	success: boolean;
	message: string;
	appointment: Appointment;
	queue: Queue[];
}

export interface CompleteAppointmentRequest {
	/** Код МКБ или числовой id */
	diagnosis?: string | number | null;
	diagnosisId?: number | null;
}

export interface Review {
	id: number;
	appointmentId: number;
	doctorId: number;
	patientId: number;
	patientName?: string | null;
	rating: number;
	reviewText: string | null;
	createdAt: string;
}

// Request/Response типы для авторизации
export interface LoginRequest {
	email: string;
	password: string;
}

/** Фрагмент CurrentUserDto.patient (ответ логина) */
export interface LoginPatientInfoPayload {
	id: number;
	birthDate?: string | null;
	gender?: number | null;
	insuranceNumber?: string | null;
	createdAt?: string;
	updatedAt?: string;
}

/** CurrentUserDto в теле POST /api/auth/login */
export interface LoginCurrentUserPayload {
	id: number;
	email: string;
	phone?: string | null;
	firstName?: string | null;
	lastName?: string | null;
	middleName?: string | null;
	createdAt: string;
	updatedAt: string;
	active: boolean;
	patientId?: number | null;
	doctorId?: number | null;
	patient?: LoginPatientInfoPayload | null;
	doctor?: unknown;
}

export interface LoginResponse {
	success?: boolean;
	status?: number;
	message: string;
	data: {
		token: string;
		email: string;
		message: string;
		roleCode?: string; // "patient", "doctor", "admin"
		user?: LoginCurrentUserPayload | null;
	} | null;
}

export interface RegisterRequest {
	email: string;
	phone: string;
	password: string;
	confirmPassword: string;
	fio: string;
}

export interface RegisterWithPatientRequest {
	email: string;
	phone: string;
	password: string;
	confirmPassword: string;
	fio: string;
	birthDate: string; // YYYY-MM-DD
	gender: 1 | 2;
	insuranceNumber?: string;
}

export interface RegisterResponse {
	success?: boolean;
	status?: number;
	message: string;
	data: {
		token: string;
		user?: User;
		patient?: Patient;
		message: string;
	} | null;
}

// Request типы для создания/обновления
export interface CreateUserRequest {
	email: string;
	phone: string;
	password: string;
	confirmPassword: string;
	fio: string;
}

export interface UpdateUserRequest {
	email?: string;
	phone?: string;
	firstName?: string;
	lastName?: string;
	middleName?: string;
}

export interface CreateDoctorRequest {
	user: {
		email: string;
		phone?: string;
		firstName: string;
		lastName: string;
		middleName: string;
	};
	displayName: string;
	bio?: string;
	experienceYears?: number;
	photo?: string | null;
	specializationIds?: number[];
}

export interface UpdateDoctorRequest {
	user?: {
		email?: string;
		phone?: string;
		firstName?: string;
		lastName?: string;
		middleName?: string;
	};
	displayName?: string;
	bio?: string;
	experienceYears?: number;
	photo?: string | null;
	specializationIds?: number[];
}

export interface CreateSpecializationRequest {
	code: string;
	name: string;
	description: string;
}

export interface UpdateSpecializationRequest {
	code?: string;
	name?: string;
	description?: string;
}

/** Тело POST/PUT услуги (модель Service на бэкенде) */
export interface CreateClinicServiceRequest {
	name: string;
	code?: string | null;
	price: number | string;
	durationMinutes: number;
	description?: string | null;
}

export interface UpdateClinicServiceRequest {
	name?: string;
	code?: string | null;
	price?: number | string;
	durationMinutes?: number;
	description?: string | null;
}

export interface SetServiceSpecializationsRequest {
	specializationIds: number[];
}

export interface CreatePatientRequest {
	user: {
		email: string;
		phone?: string;
		firstName: string;
		lastName: string;
		middleName?: string;
	};
	birthDate?: string; // YYYY-MM-DD
	gender?: 1 | 2;
	insuranceNumber?: string;
}

export interface UpdatePatientRequest {
	user?: {
		email?: string;
		phone?: string;
		firstName?: string;
		lastName?: string;
		middleName?: string;
	};
	birthDate?: string;
	gender?: 1 | 2;
	insuranceNumber?: string;
}

export interface CreateScheduleRequest {
	doctor: {
		id: number;
	};
	room?: {
		id: number;
	} | null;
	dateAt: string; // YYYY-MM-DD
	startTime: string; // HH:mm:ss
	endTime: string; // HH:mm:ss
	slotDurationMinutes: number;
}

export interface CreateRoomRequest {
	code: string;
	name?: string;
}

export interface CreateAppointmentRequest {
	schedule?: {
		id: number;
	} | null;
	doctor: {
		id: number;
	};
	patient: {
		id: number;
	};
	room?: {
		id: number;
	} | null;
	startTime: string; // ISO 8601
	endTime: string; // ISO 8601
	status: "scheduled" | "confirmed" | "in_progress" | "completed" | "cancelled" | "no_show";
	source: "mobile" | "reception" | "web" | "admin";
	createdBy?: {
		id: number;
	} | null;
}

// Частичное обновление приёма
export interface UpdateAppointmentRequest {
	status?: "scheduled" | "confirmed" | "in_progress" | "completed" | "cancelled" | "no_show" | "available";
	/** Код МКБ или id диагноза */
	diagnosis?: string | number | null;
	diagnosisId?: number | null;
	cancelReason?: string | null;
	complaints?: string | null;
	anamnesis?: string | null;
	recommendations?: string | null;
	result?: string | null;
}

export interface BookAppointmentRequest {
	appointmentId: number;
	userId: number;
	/** Если слот без услуги — бэкенд проставит; если у слота услуга — должна совпадать */
	serviceId?: number | null;
}

export interface CreateQueueRequest {
	doctor: {
		id: number;
	};
	appointment: {
		id: number;
	};
	patient: {
		id: number;
	};
	position: number;
}

export interface CreateReviewRequest {
	appointment: {
		id: number;
	};
	doctor: {
		id: number;
	};
	patient: {
		id: number;
	};
	rating: number;
	reviewText: string;
}

// Обёрнутые ответы API
export interface ApiResponse<T> {
	success: boolean;
	message: string;
	status: number;
	data: T | null;
}

// Статистика пользователя
export interface UserStats {
	appointmentsCount: number;
	reviewsCount: number;
	queueEntriesCount: number;
}

// Ответ при отмене записи
export interface CancelAppointmentResponse {
	success: boolean;
	message: string;
	appointment: Appointment;
	queue: Queue[];
}

// Запрос на отмену записи
export interface CancelAppointmentRequest {
	cancelReason?: string;
}

// Обновление отзыва
export interface UpdateReviewRequest {
	rating?: number;
	reviewText?: string;
}

// Query параметры для поиска и фильтрации
export interface DoctorsQueryParams {
	q?: string; // Поисковый запрос
	/** Врачи, у которых специализация связана с услугой (specialization_services) */
	serviceId?: number;
	limit?: number;
	offset?: number;
	sortBy?: "firstName" | "first_name" | "lastName" | "last_name" | "experience" | "experience_years" | "experienceYears" | "rating" | "created" | "created_at" | "createdAt" | "updated" | "updated_at" | "updatedAt";
	sortOrder?: "asc" | "desc";
}

export interface ServicesQueryParams {
	/** Услуги по специализациям врача */
	doctorId?: number;
}

/** Параметры списка приёмов (админ/фильтры) */
export interface AppointmentsQueryParams {
	doctorId?: number;
	status?: string;
	date?: string;
}

/** Приёмы текущего врача (JWT) */
export interface MyDoctorAppointmentsParams {
	date?: string;
}

/** Строка приёма в отчётах (агрегированные данные) */
export interface DailyReportAppointmentRow {
	appointmentId?: number;
	startTime?: string;
	status?: string;
	doctorDisplayName?: string;
	patientFullName?: string;
	patientPhone?: string;
	roomNumber?: string;
	diagnosis?: unknown;
}

/** Ответ отчётов (reports API) */
export interface DailyReport {
	date?: string;
	doctorId?: number;
	doctorDisplayName?: string | null;
	appointments?: DailyReportAppointmentRow[];
	totalAppointments?: number;
	scheduledCount?: number;
	completedCount?: number;
	cancelledCount?: number;
	noShowCount?: number;
}
