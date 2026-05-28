import { http, HttpResponse } from "msw";

import type {
	Appointment,
	ClinicService,
	Doctor,
	Patient,
	Room,
	Schedule,
	Specialization,
	User,
} from "@/api/types";
import { API_BASE } from "@/store/api/utils";

export const mockUser: User = {
	id: 1,
	email: "admin@test.com",
	phone: "+79001234567",
	firstName: "Admin",
	lastName: "User",
	middleName: "",
	createdAt: "2024-01-01T00:00:00Z",
	updatedAt: "2024-01-01T00:00:00Z",
	active: true,
	role: "admin",
};

export const mockDoctor: Doctor = {
	id: 10,
	user: { ...mockUser, id: 2, role: "doctor" },
	displayName: "Dr. Smith",
	bio: "Experienced physician",
	experienceYears: 10,
	photo: null,
	rating: 4.5,
	reviewCount: 12,
	specializations: [],
	specialization: null,
	createdAt: "2024-01-01T00:00:00Z",
	updatedAt: "2024-01-01T00:00:00Z",
};

export const mockPatient: Patient = {
	id: 20,
	user: { ...mockUser, id: 3, role: "patient" },
	birthDate: "1990-05-15",
	gender: 1,
	insuranceNumber: "1234567890",
	createdAt: "2024-01-01T00:00:00Z",
	updatedAt: "2024-01-01T00:00:00Z",
};

export const mockAppointment: Appointment = {
	id: 100,
	scheduleId: 1,
	doctorId: mockDoctor.id,
	patientId: mockPatient.id,
	roomId: 1,
	startTime: "2026-06-01T10:00:00Z",
	endTime: "2026-06-01T10:30:00Z",
	status: "scheduled",
	source: "web",
	createdBy: 1,
	createdAt: "2024-01-01T00:00:00Z",
	updatedAt: "2024-01-01T00:00:00Z",
	cancelReason: null,
};

export const mockService: ClinicService = {
	id: 5,
	name: "Consultation",
	code: "CONS",
	price: 1500,
	durationMinutes: 30,
};

export const mockSchedule: Schedule = {
	id: 1,
	doctorId: mockDoctor.id,
	roomId: 1,
	dateAt: "2026-06-01",
	startTime: "09:00:00",
	endTime: "17:00:00",
	slotDurationMinutes: 30,
	createdAt: "2024-01-01T00:00:00Z",
	updatedAt: "2024-01-01T00:00:00Z",
};

export const mockRoom: Room = {
	id: 1,
	code: "101",
	name: "Cabinet 101",
};

export const mockSpecialization: Specialization = {
	id: 1,
	code: "THER",
	name: "Therapist",
	description: "General therapy",
};

export const handlers = [
	http.post(`${API_BASE}/auth/login`, async ({ request }) => {
		const body = (await request.json()) as { email: string; password: string };
		if (body.password === "wrong") {
			return HttpResponse.json({ message: "Invalid credentials" }, { status: 401 });
		}
		return HttpResponse.json({
			message: "OK",
			data: {
				token: "test-token",
				email: body.email,
				message: "Logged in",
				roleCode: "admin",
				user: mockUser,
			},
		});
	}),

	http.post(`${API_BASE}/auth/register`, () =>
		HttpResponse.json({
			message: "Registered",
			data: { token: "new-token", message: "OK" },
		})
	),

	http.get(`${API_BASE}/users`, () =>
		HttpResponse.json({ success: true, data: [mockUser], message: "", status: 200 })
	),

	http.get(`${API_BASE}/users/:id`, ({ params }) =>
		HttpResponse.json({ ...mockUser, id: Number(params.id) })
	),

	http.post(`${API_BASE}/users/create`, async ({ request }) => {
		const body = (await request.json()) as Record<string, unknown>;
		return HttpResponse.json({ ...mockUser, id: 99, email: body.email });
	}),

	http.put(`${API_BASE}/users/:id`, async ({ params, request }) => {
		const body = (await request.json()) as Record<string, unknown>;
		return HttpResponse.json({ ...mockUser, id: Number(params.id), ...body });
	}),

	http.delete(`${API_BASE}/users/:id`, () => new HttpResponse(null, { status: 204 })),

	http.get(`${API_BASE}/doctors`, () =>
		HttpResponse.json({ success: true, data: [mockDoctor], message: "", status: 200 })
	),

	http.get(`${API_BASE}/doctors/:id`, () => HttpResponse.json(mockDoctor)),

	http.post(`${API_BASE}/doctors/create`, () => HttpResponse.json({ ...mockDoctor, id: 11 })),

	http.put(`${API_BASE}/doctors/:id`, ({ params }) =>
		HttpResponse.json({ ...mockDoctor, id: Number(params.id) })
	),

	http.delete(`${API_BASE}/doctors/:id`, () => new HttpResponse(null, { status: 204 })),

	http.get(`${API_BASE}/patients`, () =>
		HttpResponse.json({ success: true, data: [mockPatient], message: "", status: 200 })
	),

	http.get(`${API_BASE}/patients/:id`, () => HttpResponse.json(mockPatient)),

	http.put(`${API_BASE}/patients/:id`, ({ params }) =>
		HttpResponse.json({ ...mockPatient, id: Number(params.id) })
	),

	http.get(`${API_BASE}/appointments`, () =>
		HttpResponse.json({ success: true, data: [mockAppointment], message: "", status: 200 })
	),

	http.get(`${API_BASE}/appointments/:id`, ({ params }) =>
		HttpResponse.json({ ...mockAppointment, id: Number(params.id) })
	),

	http.post(`${API_BASE}/appointments`, () =>
		HttpResponse.json({ ...mockAppointment, id: 101 })
	),

	http.put(`${API_BASE}/appointments/:id`, ({ params }) =>
		HttpResponse.json({ success: true, data: { ...mockAppointment, id: Number(params.id) } })
	),

	http.post(`${API_BASE}/appointments/:id/cancel`, ({ params }) =>
		HttpResponse.json({
			success: true,
			message: "Cancelled",
			appointment: { ...mockAppointment, id: Number(params.id), status: "cancelled" },
			queue: [],
		})
	),

	http.post(`${API_BASE}/appointments/book`, () =>
		HttpResponse.json({ ...mockAppointment, status: "confirmed" })
	),

	http.get(`${API_BASE}/services`, () => HttpResponse.json([mockService])),

	http.post(`${API_BASE}/services`, () => HttpResponse.json({ ...mockService, id: 6 })),

	http.get(`${API_BASE}/schedules`, () =>
		HttpResponse.json({ success: true, data: [mockSchedule], message: "", status: 200 })
	),

	http.post(`${API_BASE}/schedules`, () =>
		HttpResponse.json({ ...mockSchedule, id: 2, dateAt: "2026-07-01" })
	),

	http.get(`${API_BASE}/rooms`, () =>
		HttpResponse.json({ success: true, data: [mockRoom], message: "", status: 200 })
	),

	http.get(`${API_BASE}/specializations`, () =>
		HttpResponse.json({ success: true, data: [mockSpecialization], message: "", status: 200 })
	),

	http.get(`${API_BASE}/diagnoses`, () =>
		HttpResponse.json({ success: true, data: [], message: "", status: 200 })
	),
];
