import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { mockUser } from "@/test/msw/handlers";
import { server } from "@/test/msw/server";
import { setupApiStore } from "@/test/store-utils";
import { API_BASE } from "./utils";

describe("auth integration", () => {
	it("login mutation returns token on success", async () => {
		const { store, api } = setupApiStore();
		const result = await store.dispatch(
			api.endpoints.login.initiate({ email: "admin@test.com", password: "secret" })
		);
		expect(result.data?.data?.token).toBe("test-token");
	});

	it("login mutation surfaces 401 without redirect", async () => {
		Object.defineProperty(window, "location", {
			value: { href: "http://localhost/" },
			writable: true,
			configurable: true,
		});
		const { store, api } = setupApiStore();
		const result = await store.dispatch(
			api.endpoints.login.initiate({ email: "admin@test.com", password: "wrong" })
		);
		expect(result.error).toBeDefined();
		expect(window.location.href).toBe("http://localhost/");
	});

	it("register mutation succeeds", async () => {
		const { store, api } = setupApiStore();
		const result = await store.dispatch(
			api.endpoints.register.initiate({
				email: "new@test.com",
				phone: "+79001112233",
				password: "pass",
				confirmPassword: "pass",
				fio: "New User",
			})
		);
		expect(result.data?.data?.token).toBe("new-token");
	});

	it("registerWithPatient mutation hits register-with-patient endpoint", async () => {
		server.use(
			http.post(`${API_BASE}/auth/register-with-patient`, () =>
				HttpResponse.json({ message: "OK", data: { token: "patient-token", message: "OK" } })
			)
		);
		const { store, api } = setupApiStore();
		const result = await store.dispatch(
			api.endpoints.registerWithPatient.initiate({
				email: "p@test.com",
				phone: "+79001112233",
				password: "pass",
				confirmPassword: "pass",
				fio: "Patient User",
				birthDate: "1990-01-01",
				gender: 1,
			})
		);
		expect(result.data?.data?.token).toBe("patient-token");
	});
});

describe("query integration", () => {
	it("getUsers unwraps list from ApiResponse", async () => {
		const { store, api } = setupApiStore();
		const result = await store.dispatch(api.endpoints.getUsers.initiate());
		expect(result.data?.[0]?.email).toBe(mockUser.email);
	});

	it("getDoctors returns doctor list", async () => {
		const { store, api } = setupApiStore();
		const result = await store.dispatch(api.endpoints.getDoctors.initiate(undefined));
		expect(result.data?.[0]?.displayName).toBe("Dr. Smith");
	});

	it("getDoctorById unwraps doctor entity", async () => {
		const { store, api } = setupApiStore();
		const result = await store.dispatch(api.endpoints.getDoctorById.initiate(10));
		expect(result.data?.id).toBe(10);
	});

	it("getPatients returns patient list", async () => {
		const { store, api } = setupApiStore();
		const result = await store.dispatch(api.endpoints.getPatients.initiate());
		expect(result.data?.[0]?.id).toBe(20);
	});

	it("getAppointments returns appointment list", async () => {
		const { store, api } = setupApiStore();
		const result = await store.dispatch(api.endpoints.getAppointments.initiate());
		expect(result.data?.[0]?.id).toBe(100);
	});

	it("getAppointmentById unwraps wrapped entity", async () => {
		const { store, api } = setupApiStore();
		const result = await store.dispatch(api.endpoints.getAppointmentById.initiate(100));
		expect(result.data?.id).toBe(100);
	});

	it("getServices returns service array", async () => {
		const { store, api } = setupApiStore();
		const result = await store.dispatch(api.endpoints.getServices.initiate());
		expect(result.data?.[0]?.name).toBe("Consultation");
	});

	it("getSchedules and getRooms return lists", async () => {
		const { store, api } = setupApiStore();
		const schedules = await store.dispatch(api.endpoints.getSchedules.initiate());
		const rooms = await store.dispatch(api.endpoints.getRooms.initiate());
		expect(schedules.data?.[0]?.dateAt).toBe("2026-06-01");
		expect(rooms.data?.[0]?.code).toBe("101");
	});
});
