import type { BaseQueryApi, FetchBaseQueryError } from "@reduxjs/toolkit/query";
import { createApi } from "@reduxjs/toolkit/query/react";

import type {
	AppointmentsQueryParams,
	Appointment,
	AvailableAppointmentSlot,
	BookAppointmentRequest,
	CancelAppointmentRequest,
	CancelAppointmentResponse,
	ClinicService,
	CompleteAppointmentRequest,
	CompleteAppointmentResponse,
	CreateAppointmentRequest,
	CreateClinicServiceRequest,
	CreateDoctorRequest,
	CreatePatientRequest,
	CreateQueueRequest,
	CreateReviewRequest,
	CreateRoomRequest,
	CreateScheduleRequest,
	CreateSpecializationRequest,
	CreateUserRequest,
	DailyReport,
	Diagnosis,
	Doctor,
	DoctorsQueryParams,
	LoginRequest,
	LoginResponse,
	MyDoctorAppointmentsParams,
	Patient,
	Queue,
	RegisterRequest,
	RegisterResponse,
	RegisterWithPatientRequest,
	Review,
	Room,
	Schedule,
	SetServiceSpecializationsRequest,
	ServicesQueryParams,
	Specialization,
	UpdateAppointmentRequest,
	UpdateClinicServiceRequest,
	UpdateDoctorRequest,
	UpdatePatientRequest,
	UpdateReviewRequest,
	UpdateSpecializationRequest,
	UpdateUserRequest,
	User,
	UserStats,
} from "@/api/types";

import { baseQueryWithReauth } from "./baseQuery";
import {
	doctorsQueryParams,
	unwrapDoctorDto,
	unwrapEntity,
	unwrapList,
	unwrapWrappedOrEntity,
} from "./utils";

export const api = createApi({
	reducerPath: "api",
	baseQuery: baseQueryWithReauth,
	tagTypes: [
		"Appointment",
		"Doctor",
		"Patient",
		"Schedule",
		"Queue",
		"Review",
		"Room",
		"User",
		"Specialization",
		"Service",
		"Diagnosis",
	],
	endpoints: (build) => ({
		login: build.mutation<LoginResponse, LoginRequest>({
			query: (body) => ({ url: "auth/login", method: "POST", body }),
		}),
		register: build.mutation<RegisterResponse, RegisterRequest>({
			query: (body) => ({ url: "auth/register", method: "POST", body }),
		}),
		registerWithPatient: build.mutation<RegisterResponse, RegisterWithPatientRequest>({
			query: (body) => ({ url: "auth/register-with-patient", method: "POST", body }),
		}),
		requestPasswordReset: build.mutation<unknown, { email: string }>({
			query: (body) => ({ url: "auth/forgot-password", method: "POST", body }),
		}),
		resetPassword: build.mutation<unknown, { token: string; password: string }>({
			query: (body) => ({ url: "auth/reset-password", method: "POST", body }),
		}),

		getUsers: build.query<User[], void>({
			query: () => "users",
			transformResponse: (r: unknown) => unwrapList<User>(r),
			providesTags: (r) =>
				r
					? [...r.map((u) => ({ type: "User" as const, id: u.id })), { type: "User", id: "LIST" }]
					: [{ type: "User", id: "LIST" }],
		}),
		getUserStats: build.query<UserStats, void>({
			query: () => "users/userStats",
			transformResponse: (r: unknown) => unwrapEntity<UserStats>(r, "Статистика не найдена"),
		}),
		getUserById: build.query<User, number>({
			query: (id) => `users/${id}`,
			transformResponse: (r: unknown) => r as User,
			providesTags: (_r, _e, id) => [{ type: "User", id }],
		}),
		createUser: build.mutation<User, CreateUserRequest>({
			query: (body) => ({ url: "users/create", method: "POST", body }),
			transformResponse: (r: unknown) => r as User,
			invalidatesTags: [{ type: "User", id: "LIST" }],
		}),
		updateUser: build.mutation<User, { id: number; body: UpdateUserRequest }>({
			query: ({ id, body }) => ({ url: `users/${id}`, method: "PUT", body }),
			transformResponse: (r: unknown) => r as User,
			invalidatesTags: (_r, _e, { id }) => [{ type: "User", id }, { type: "User", id: "LIST" }],
		}),
		deleteUser: build.mutation<void, number>({
			query: (id) => ({ url: `users/${id}`, method: "DELETE" }),
			invalidatesTags: [{ type: "User", id: "LIST" }],
		}),

		getDoctors: build.query<Doctor[], DoctorsQueryParams | void>({
			query: (params) => ({ url: "doctors", params: doctorsQueryParams(params ?? undefined) }),
			transformResponse: (r: unknown) => unwrapList<Doctor>(r),
			providesTags: (r) =>
				r
					? [...r.map((d) => ({ type: "Doctor" as const, id: d.id })), { type: "Doctor", id: "LIST" }]
					: [{ type: "Doctor", id: "LIST" }],
		}),
		getDoctorById: build.query<Doctor, number>({
			query: (id) => `doctors/${id}`,
			transformResponse: (r: unknown) => unwrapDoctorDto(r),
			providesTags: (_r, _e, id) => [{ type: "Doctor", id }],
		}),
		createDoctor: build.mutation<Doctor, CreateDoctorRequest>({
			query: (body) => ({ url: "doctors/create", method: "POST", body }),
			transformResponse: (r: unknown) => unwrapDoctorDto(r),
			invalidatesTags: [{ type: "Doctor", id: "LIST" }],
		}),
		updateDoctor: build.mutation<Doctor, { id: number; body: UpdateDoctorRequest }>({
			query: ({ id, body }) => ({ url: `doctors/${id}`, method: "PUT", body }),
			transformResponse: (r: unknown) => unwrapDoctorDto(r),
			invalidatesTags: (_r, _e, { id }) => [{ type: "Doctor", id }, { type: "Doctor", id: "LIST" }],
		}),
		deleteDoctor: build.mutation<void, number>({
			query: (id) => ({ url: `doctors/${id}`, method: "DELETE" }),
			invalidatesTags: [{ type: "Doctor", id: "LIST" }],
		}),

		getSpecializations: build.query<Specialization[], void>({
			query: () => "specializations",
			transformResponse: (r: unknown) => unwrapList<Specialization>(r),
			providesTags: [{ type: "Specialization", id: "LIST" }],
		}),
		getSpecializationById: build.query<Specialization, number>({
			query: (id) => `specializations/${id}`,
			providesTags: (_r, _e, id) => [{ type: "Specialization", id }],
		}),
		getSpecializationByCode: build.query<Specialization, string>({
			query: (code) => `specializations/code/${code}`,
		}),
		createSpecialization: build.mutation<Specialization, CreateSpecializationRequest>({
			query: (body) => ({ url: "specializations", method: "POST", body }),
			invalidatesTags: [{ type: "Specialization", id: "LIST" }],
		}),
		updateSpecialization: build.mutation<
			Specialization,
			{ id: number; body: UpdateSpecializationRequest }
		>({
			query: ({ id, body }) => ({ url: `specializations/${id}`, method: "PUT", body }),
			invalidatesTags: (_r, _e, { id }) => [{ type: "Specialization", id }, { type: "Specialization", id: "LIST" }],
		}),
		deleteSpecialization: build.mutation<void, number>({
			query: (id) => ({ url: `specializations/${id}`, method: "DELETE" }),
			invalidatesTags: [{ type: "Specialization", id: "LIST" }],
		}),

		getServices: build.query<ClinicService[], ServicesQueryParams | void>({
			query: (params) => ({ url: "services", params: params ?? undefined }),
			transformResponse: (r: unknown) => (Array.isArray(r) ? r : []) as ClinicService[],
			providesTags: [{ type: "Service", id: "LIST" }],
		}),
		getServiceById: build.query<ClinicService, number>({
			query: (id) => `services/${id}`,
			providesTags: (_r, _e, id) => [{ type: "Service", id }],
		}),
		createService: build.mutation<ClinicService, CreateClinicServiceRequest>({
			query: (body) => ({ url: "services", method: "POST", body }),
			transformResponse: (r: unknown) => r as ClinicService,
			invalidatesTags: [{ type: "Service", id: "LIST" }],
		}),
		updateService: build.mutation<ClinicService, { id: number; body: UpdateClinicServiceRequest }>({
			query: ({ id, body }) => ({ url: `services/${id}`, method: "PUT", body }),
			transformResponse: (r: unknown) => r as ClinicService,
			invalidatesTags: (_r, _e, { id }) => [{ type: "Service", id }, { type: "Service", id: "LIST" }],
		}),
		deleteService: build.mutation<void, number>({
			query: (id) => ({ url: `services/${id}`, method: "DELETE" }),
			invalidatesTags: (_r, _e, id) => [{ type: "Service", id }, { type: "Service", id: "LIST" }],
		}),
		setServiceSpecializations: build.mutation<
			ClinicService,
			{ id: number; body: SetServiceSpecializationsRequest }
		>({
			query: ({ id, body }) => ({
				url: `services/${id}/specializations`,
				method: "PUT",
				body,
			}),
			transformResponse: (r: unknown) => r as ClinicService,
			invalidatesTags: (_r, _e, { id }) => [{ type: "Service", id }, { type: "Service", id: "LIST" }],
		}),

		getPatients: build.query<Patient[], void>({
			query: () => "patients",
			transformResponse: (r: unknown) => unwrapList<Patient>(r),
			providesTags: (r) =>
				r
					? [...r.map((p) => ({ type: "Patient" as const, id: p.id })), { type: "Patient", id: "LIST" }]
					: [{ type: "Patient", id: "LIST" }],
		}),
		getPatientById: build.query<Patient, number>({
			query: (id) => `patients/${id}`,
			transformResponse: (r: unknown) => unwrapEntity<Patient>(r, "Пациент не найден"),
			providesTags: (_r, _e, id) => [{ type: "Patient", id }],
		}),
		createPatient: build.mutation<Patient, CreatePatientRequest>({
			query: (body) => ({ url: "patients", method: "POST", body }),
			transformResponse: (r: unknown) => unwrapEntity<Patient>(r, "Ошибка при создании пациента"),
			invalidatesTags: [{ type: "Patient", id: "LIST" }],
		}),
		updatePatient: build.mutation<Patient, { id: number; body: UpdatePatientRequest }>({
			query: ({ id, body }) => ({ url: `patients/${id}`, method: "PUT", body }),
			transformResponse: (r: unknown) => unwrapEntity<Patient>(r, "Ошибка при обновлении пациента"),
			invalidatesTags: (_r, _e, { id }) => [{ type: "Patient", id }, { type: "Patient", id: "LIST" }],
		}),
		deletePatient: build.mutation<void, number>({
			query: (id) => ({ url: `patients/${id}`, method: "DELETE" }),
			invalidatesTags: [{ type: "Patient", id: "LIST" }],
		}),

		getAppointments: build.query<Appointment[], AppointmentsQueryParams | void>({
			query: (params) => ({ url: "appointments", params: params ?? undefined }),
			transformResponse: (r: unknown) => unwrapList<Appointment>(r),
			providesTags: [{ type: "Appointment", id: "LIST" }],
		}),
		getAppointmentById: build.query<Appointment, number>({
			query: (id) => `appointments/${id}`,
			transformResponse: (r: unknown) => unwrapWrappedOrEntity<Appointment>(r, "Запись не найдена"),
			providesTags: (_r, _e, id) => [{ type: "Appointment", id }],
		}),
		getAppointmentsByDoctor: build.query<Appointment[], { doctorId: number; date?: string }>({
			query: ({ doctorId, date }) => ({
				url: `appointments/doctor/${doctorId}`,
				params: date ? { date } : undefined,
			}),
			transformResponse: (r: unknown) => unwrapList<Appointment>(r),
			providesTags: (_r, _e, { doctorId }) => [
				{ type: "Appointment", id: "LIST" },
				{ type: "Appointment", id: `doctor-${doctorId}` },
			],
		}),
		getMyDoctorAppointments: build.query<Appointment[], MyDoctorAppointmentsParams | void>({
			query: (params) => ({ url: "appointments/my/doctor", params: params ?? undefined }),
			transformResponse: (r: unknown) => unwrapList<Appointment>(r),
			providesTags: [{ type: "Appointment", id: "LIST" }, { type: "Appointment", id: "MY_DOCTOR" }],
		}),
		getAppointmentsByPatient: build.query<Appointment[], number>({
			query: (patientId) => `appointments/patient/${patientId}`,
			transformResponse: (r: unknown) => unwrapList<Appointment>(r),
			providesTags: (_r, _e, patientId) => [
				{ type: "Appointment", id: "LIST" },
				{ type: "Appointment", id: `patient-${patientId}` },
			],
		}),
		getAvailableSlots: build.query<
			AvailableAppointmentSlot[],
			{ doctorId: number; date: string; serviceId?: number | null }
		>({
			query: ({ doctorId, date, serviceId }) => {
				const params: Record<string, string | number> = { doctorId, date };
				if (serviceId != null && serviceId !== undefined) params.serviceId = serviceId;
				return { url: "appointments/available", params };
			},
			transformResponse: (r: unknown) => unwrapList<AvailableAppointmentSlot>(r),
		}),
		getAvailableDates: build.query<
			string[],
			{ doctorId: number; serviceId?: number; from?: string; to?: string }
		>({
			query: ({ doctorId, serviceId, from, to }) => {
				const params: Record<string, string | number> = { doctorId };
				if (serviceId != null && Number.isFinite(serviceId)) params.serviceId = serviceId;
				if (from) params.from = from;
				if (to) params.to = to;
				return { url: "appointments/available/dates", params };
			},
			transformResponse: (r: unknown) => (Array.isArray(r) ? r : []) as string[],
		}),
		createAppointment: build.mutation<Appointment, CreateAppointmentRequest>({
			query: (body) => ({ url: "appointments", method: "POST", body }),
			transformResponse: (r: unknown) => r as Appointment,
			invalidatesTags: [{ type: "Appointment", id: "LIST" }],
		}),
		updateAppointment: build.mutation<Appointment, { id: number; body: UpdateAppointmentRequest }>({
			async queryFn({ id, body }, apiCtx, extraOptions) {
				const r1 = await baseQueryWithReauth(
					{ url: `appointments/${id}`, method: "PUT", body },
					apiCtx as BaseQueryApi,
					extraOptions
				);
				if (r1.error) return { error: r1.error };
				const payload = r1.data;
				if (
					payload &&
					typeof payload === "object" &&
					"success" in payload &&
					(payload as { success?: boolean }).success === false
				) {
					return {
						error: {
							status: 400,
							data: (payload as { message?: string }).message || "Ошибка при обновлении приёма",
						} as FetchBaseQueryError,
					};
				}
				const r2 = await baseQueryWithReauth(`appointments/${id}`, apiCtx as BaseQueryApi, extraOptions);
				if (r2.error) return { error: r2.error };
				return {
					data: unwrapWrappedOrEntity<Appointment>(r2.data, "Запись не найдена"),
				};
			},
			invalidatesTags: (_r, _e, { id }) => [
				{ type: "Appointment", id },
				{ type: "Appointment", id: "LIST" },
			],
		}),
		bookAppointment: build.mutation<Appointment, BookAppointmentRequest>({
			query: (body) => ({ url: "appointments/book", method: "POST", body }),
			transformResponse: (r: unknown) => r as Appointment,
			invalidatesTags: [{ type: "Appointment", id: "LIST" }],
		}),
		cancelAppointment: build.mutation<CancelAppointmentResponse, { id: number; body?: CancelAppointmentRequest }>({
			query: ({ id, body }) => ({
				url: `appointments/${id}/cancel`,
				method: "POST",
				body: body ?? {},
			}),
			transformResponse: (r: unknown) => r as CancelAppointmentResponse,
			invalidatesTags: (_r, _e, { id }) => [
				{ type: "Appointment", id },
				{ type: "Appointment", id: "LIST" },
				{ type: "Queue", id: "LIST" },
			],
		}),
		completeAppointment: build.mutation<
			CompleteAppointmentResponse,
			{ id: number; body?: CompleteAppointmentRequest }
		>({
			query: ({ id, body }) => ({
				url: `appointments/${id}/complete`,
				method: "POST",
				body: body ?? {},
			}),
			transformResponse: (r: unknown) => r as CompleteAppointmentResponse,
			invalidatesTags: (_r, _e, { id }) => [
				{ type: "Appointment", id },
				{ type: "Appointment", id: "LIST" },
				{ type: "Queue", id: "LIST" },
			],
		}),
		deleteAppointment: build.mutation<void, number>({
			query: (id) => ({ url: `appointments/${id}`, method: "DELETE" }),
			invalidatesTags: (_r, _e, id) => [
				{ type: "Appointment", id },
				{ type: "Appointment", id: "LIST" },
			],
		}),
		sendAppointmentNotification: build.mutation<void, number>({
			query: (id) => ({ url: `appointments/${id}/send_notification`, method: "POST" }),
		}),

		getDiagnoses: build.query<Diagnosis[], void>({
			query: () => "diagnoses",
			transformResponse: (r: unknown) => unwrapList<Diagnosis>(r),
			providesTags: [{ type: "Diagnosis", id: "LIST" }],
		}),

		getSchedules: build.query<Schedule[], void>({
			query: () => "schedules",
			transformResponse: (r: unknown) => unwrapList<Schedule>(r),
			providesTags: [{ type: "Schedule", id: "LIST" }],
		}),
		getScheduleById: build.query<Schedule, number>({
			query: (id) => `schedules/${id}`,
			transformResponse: (r: unknown) => unwrapWrappedOrEntity<Schedule>(r, "Расписание не найдено"),
			providesTags: (_r, _e, id) => [{ type: "Schedule", id }],
		}),
		getSchedulesByDoctor: build.query<Schedule[], number>({
			query: (doctorId) => `schedules/doctor/${doctorId}`,
			transformResponse: (r: unknown) => unwrapList<Schedule>(r),
			providesTags: (_r, _e, doctorId) => [
				{ type: "Schedule", id: "LIST" },
				{ type: "Schedule", id: `doctor-${doctorId}` },
			],
		}),
		createSchedule: build.mutation<Schedule, CreateScheduleRequest>({
			query: (body) => ({ url: "schedules", method: "POST", body }),
			transformResponse: (r: unknown) => r as Schedule,
			invalidatesTags: [{ type: "Schedule", id: "LIST" }],
		}),
		deleteSchedule: build.mutation<void, number>({
			query: (id) => ({ url: `schedules/${id}`, method: "DELETE" }),
			invalidatesTags: [{ type: "Schedule", id: "LIST" }],
		}),

		getQueueById: build.query<Queue, number>({
			query: (id) => `queue/${id}`,
			transformResponse: (r: unknown) => unwrapWrappedOrEntity<Queue>(r, "Запись очереди не найдена"),
			providesTags: (_r, _e, id) => [{ type: "Queue", id }],
		}),
		getQueueByDoctor: build.query<Queue[], number>({
			query: (doctorId) => `queue/doctor/${doctorId}`,
			transformResponse: (r: unknown) => unwrapList<Queue>(r),
			providesTags: (_r, _e, doctorId) => [
				{ type: "Queue", id: "LIST" },
				{ type: "Queue", id: `doctor-${doctorId}` },
			],
		}),
		getLiveQueueByDoctor: build.query<Queue[], { doctorId: number; date?: string }>({
			query: ({ doctorId, date }) => ({
				url: `queue/live/doctor/${doctorId}`,
				params: date ? { date } : undefined,
			}),
			transformResponse: (r: unknown) => unwrapList<Queue>(r),
			providesTags: (_r, _e, { doctorId }) => [
				{ type: "Queue", id: "LIST" },
				{ type: "Queue", id: `live-${doctorId}` },
			],
		}),
		createQueueEntry: build.mutation<Queue, CreateQueueRequest>({
			query: (body) => ({ url: "queue", method: "POST", body }),
			transformResponse: (r: unknown) => r as Queue,
			invalidatesTags: [{ type: "Queue", id: "LIST" }],
		}),
		deleteQueueEntry: build.mutation<void, number>({
			query: (id) => ({ url: `queue/${id}`, method: "DELETE" }),
			invalidatesTags: [{ type: "Queue", id: "LIST" }],
		}),

		getReviewById: build.query<Review, number>({
			query: (id) => `reviews/${id}`,
			transformResponse: (r: unknown) => unwrapWrappedOrEntity<Review>(r, "Отзыв не найден"),
			providesTags: (_r, _e, id) => [{ type: "Review", id }],
		}),
		getReviewsByDoctor: build.query<Review[], number>({
			query: (doctorId) => `reviews/doctor/${doctorId}`,
			transformResponse: (r: unknown) => unwrapList<Review>(r),
			providesTags: (_r, _e, doctorId) => [
				{ type: "Review", id: "LIST" },
				{ type: "Review", id: `doctor-${doctorId}` },
			],
		}),
		getReviewByAppointment: build.query<Review, number>({
			query: (appointmentId) => `reviews/appointment/${appointmentId}`,
			transformResponse: (r: unknown) => unwrapWrappedOrEntity<Review>(r, "Отзыв не найден"),
		}),
		createReview: build.mutation<Review, CreateReviewRequest>({
			query: (body) => ({ url: "reviews", method: "POST", body }),
			transformResponse: (r: unknown) => r as Review,
			invalidatesTags: (_r, _e, arg) => [
				{ type: "Review", id: "LIST" },
				{ type: "Review", id: `doctor-${arg.doctor.id}` },
			],
		}),
		updateReview: build.mutation<Review, { id: number; body: UpdateReviewRequest }>({
			query: ({ id, body }) => ({ url: `reviews/${id}`, method: "PUT", body }),
			transformResponse: (r: unknown) => r as Review,
			invalidatesTags: (_r, _e, { id }) => [{ type: "Review", id }, { type: "Review", id: "LIST" }],
		}),
		deleteReview: build.mutation<void, number>({
			query: (id) => ({ url: `reviews/${id}`, method: "DELETE" }),
			invalidatesTags: [{ type: "Review", id: "LIST" }],
		}),

		getRooms: build.query<Room[], void>({
			query: () => "rooms",
			transformResponse: (r: unknown) => unwrapList<Room>(r),
			providesTags: [{ type: "Room", id: "LIST" }],
		}),
		getRoomById: build.query<Room, number>({
			query: (id) => `rooms/${id}`,
			transformResponse: (r: unknown) => unwrapWrappedOrEntity<Room>(r, "Кабинет не найден"),
			providesTags: (_r, _e, id) => [{ type: "Room", id }],
		}),
		createRoom: build.mutation<Room, CreateRoomRequest>({
			query: (body) => ({ url: "rooms", method: "POST", body }),
			transformResponse: (r: unknown) => unwrapWrappedOrEntity<Room>(r, "Ошибка при создании кабинета"),
			invalidatesTags: [{ type: "Room", id: "LIST" }],
		}),
		deleteRoom: build.mutation<void, number>({
			query: (id) => ({ url: `rooms/${id}`, method: "DELETE" }),
			invalidatesTags: [{ type: "Room", id: "LIST" }],
		}),

		getDailyReport: build.query<DailyReport, string>({
			query: (date) => ({ url: "reports/daily", params: { date } }),
		}),
		getDailyReportByDoctor: build.query<DailyReport, { doctorId: number; date: string }>({
			query: ({ doctorId, date }) => ({
				url: `reports/daily/doctor/${doctorId}`,
				params: { date },
			}),
		}),
		getRangeReport: build.query<DailyReport, { startDate: string; endDate: string }>({
			query: ({ startDate, endDate }) => ({
				url: "reports/range",
				params: { startDate, endDate },
			}),
		}),
		getRangeReportByDoctor: build.query<
			DailyReport,
			{ doctorId: number; startDate: string; endDate: string }
		>({
			query: ({ doctorId, startDate, endDate }) => ({
				url: `reports/range/doctor/${doctorId}`,
				params: { startDate, endDate },
			}),
		}),

		exportDailyExcel: build.query<Blob, string>({
			query: (date) => ({
				url: "reports/daily/excel",
				params: { date },
				responseHandler: (response) => response.blob(),
			}),
			keepUnusedDataFor: 0,
		}),
		exportDailyPdf: build.query<Blob, string>({
			query: (date) => ({
				url: "reports/daily/pdf",
				params: { date },
				responseHandler: (response) => response.blob(),
			}),
			keepUnusedDataFor: 0,
		}),
		exportDailyExcelByDoctor: build.query<Blob, { doctorId: number; date: string }>({
			query: ({ doctorId, date }) => ({
				url: `reports/daily/doctor/${doctorId}/excel`,
				params: { date },
				responseHandler: (response) => response.blob(),
			}),
			keepUnusedDataFor: 0,
		}),
		exportDailyPdfByDoctor: build.query<Blob, { doctorId: number; date: string }>({
			query: ({ doctorId, date }) => ({
				url: `reports/daily/doctor/${doctorId}/pdf`,
				params: { date },
				responseHandler: (response) => response.blob(),
			}),
			keepUnusedDataFor: 0,
		}),
		exportRangeExcel: build.query<Blob, { startDate: string; endDate: string }>({
			query: ({ startDate, endDate }) => ({
				url: "reports/range/excel",
				params: { startDate, endDate },
				responseHandler: (response) => response.blob(),
			}),
			keepUnusedDataFor: 0,
		}),
		exportRangePdf: build.query<Blob, { startDate: string; endDate: string }>({
			query: ({ startDate, endDate }) => ({
				url: "reports/range/pdf",
				params: { startDate, endDate },
				responseHandler: (response) => response.blob(),
			}),
			keepUnusedDataFor: 0,
		}),
		exportRangeExcelByDoctor: build.query<
			Blob,
			{ doctorId: number; startDate: string; endDate: string }
		>({
			query: ({ doctorId, startDate, endDate }) => ({
				url: `reports/range/doctor/${doctorId}/excel`,
				params: { startDate, endDate },
				responseHandler: (response) => response.blob(),
			}),
			keepUnusedDataFor: 0,
		}),
		exportRangePdfByDoctor: build.query<
			Blob,
			{ doctorId: number; startDate: string; endDate: string }
		>({
			query: ({ doctorId, startDate, endDate }) => ({
				url: `reports/range/doctor/${doctorId}/pdf`,
				params: { startDate, endDate },
				responseHandler: (response) => response.blob(),
			}),
			keepUnusedDataFor: 0,
		}),
	}),
});

export const {
	useLoginMutation,
	useRegisterMutation,
	useRegisterWithPatientMutation,
	useRequestPasswordResetMutation,
	useResetPasswordMutation,
	useGetUsersQuery,
	useGetUserStatsQuery,
	useGetUserByIdQuery,
	useCreateUserMutation,
	useUpdateUserMutation,
	useDeleteUserMutation,
	useGetDoctorsQuery,
	useLazyGetDoctorsQuery,
	useGetDoctorByIdQuery,
	useCreateDoctorMutation,
	useUpdateDoctorMutation,
	useDeleteDoctorMutation,
	useGetSpecializationsQuery,
	useGetSpecializationByIdQuery,
	useGetSpecializationByCodeQuery,
	useCreateSpecializationMutation,
	useUpdateSpecializationMutation,
	useDeleteSpecializationMutation,
	useGetServicesQuery,
	useLazyGetServicesQuery,
	useGetServiceByIdQuery,
	useCreateServiceMutation,
	useUpdateServiceMutation,
	useDeleteServiceMutation,
	useSetServiceSpecializationsMutation,
	useGetPatientsQuery,
	useLazyGetPatientsQuery,
	useGetPatientByIdQuery,
	useLazyGetPatientByIdQuery,
	useCreatePatientMutation,
	useUpdatePatientMutation,
	useDeletePatientMutation,
	useGetAppointmentsQuery,
	useGetAppointmentByIdQuery,
	useGetAppointmentsByDoctorQuery,
	useGetMyDoctorAppointmentsQuery,
	useGetAppointmentsByPatientQuery,
	useGetAvailableSlotsQuery,
	useGetAvailableDatesQuery,
	useCreateAppointmentMutation,
	useUpdateAppointmentMutation,
	useBookAppointmentMutation,
	useCancelAppointmentMutation,
	useCompleteAppointmentMutation,
	useDeleteAppointmentMutation,
	useSendAppointmentNotificationMutation,
	useGetDiagnosesQuery,
	useGetSchedulesQuery,
	useGetScheduleByIdQuery,
	useGetSchedulesByDoctorQuery,
	useCreateScheduleMutation,
	useDeleteScheduleMutation,
	useGetQueueByIdQuery,
	useGetQueueByDoctorQuery,
	useGetLiveQueueByDoctorQuery,
	useCreateQueueEntryMutation,
	useDeleteQueueEntryMutation,
	useGetReviewByIdQuery,
	useGetReviewsByDoctorQuery,
	useGetReviewByAppointmentQuery,
	useLazyGetReviewByAppointmentQuery,
	useCreateReviewMutation,
	useUpdateReviewMutation,
	useDeleteReviewMutation,
	useGetRoomsQuery,
	useGetRoomByIdQuery,
	useCreateRoomMutation,
	useDeleteRoomMutation,
	useGetDailyReportQuery,
	useGetDailyReportByDoctorQuery,
	useGetRangeReportQuery,
	useGetRangeReportByDoctorQuery,
	useLazyGetDailyReportQuery,
	useLazyGetDailyReportByDoctorQuery,
	useLazyGetRangeReportQuery,
	useLazyGetRangeReportByDoctorQuery,
	useLazyExportDailyExcelQuery,
	useLazyExportDailyPdfQuery,
	useLazyExportDailyExcelByDoctorQuery,
	useLazyExportDailyPdfByDoctorQuery,
	useLazyExportRangeExcelQuery,
	useLazyExportRangePdfQuery,
	useLazyExportRangeExcelByDoctorQuery,
	useLazyExportRangePdfByDoctorQuery,
} = api;
