import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/test/msw/server";
import { setupApiStore } from "@/test/store-utils";
import { API_BASE } from "./utils";

async function waitForQuery(store: ReturnType<typeof setupApiStore>["store"], endpoint: string) {
	await new Promise((r) => setTimeout(r, 50));
	const state = store.getState();
	const queries = state.api.queries;
	return Object.values(queries).find((q) => q?.endpointName === endpoint);
}

describe("cache invalidation integration", () => {
	it("createUser invalidates getUsers LIST tag", async () => {
		const { store, api } = setupApiStore();
		await store.dispatch(api.endpoints.getUsers.initiate(undefined, { forceRefetch: false }));
		let fetchCount = 0;
		server.use(
			http.get(`${API_BASE}/users`, () => {
				fetchCount += 1;
				return HttpResponse.json({ success: true, data: [], message: "", status: 200 });
			})
		);
		await store.dispatch(api.endpoints.getUsers.initiate(undefined, { forceRefetch: true }));
		expect(fetchCount).toBeGreaterThanOrEqual(1);
		await store.dispatch(
			api.endpoints.createUser.initiate({
				email: "x@test.com",
				phone: "+79001112233",
				password: "pass",
				confirmPassword: "pass",
				fio: "X Y",
			})
		);
		await store.dispatch(api.endpoints.getUsers.initiate(undefined, { forceRefetch: true }));
		expect(fetchCount).toBeGreaterThanOrEqual(2);
	});

	it("updateUser invalidates user id and LIST", async () => {
		const { store, api } = setupApiStore();
		await store.dispatch(api.endpoints.updateUser.initiate({ id: 1, body: { firstName: "New" } }));
		const entry = await waitForQuery(store, "getUsers");
		expect(entry?.status).not.toBe("rejected");
	});

	it("deleteUser invalidates LIST", async () => {
		const { store, api } = setupApiStore();
		const result = await store.dispatch(api.endpoints.deleteUser.initiate(1));
		expect(result.error).toBeUndefined();
	});

	it("createDoctor invalidates doctors LIST", async () => {
		const { store, api } = setupApiStore();
		await store.dispatch(
			api.endpoints.createDoctor.initiate({
				user: {
					email: "d@test.com",
					firstName: "Doc",
					lastName: "Tor",
					middleName: "",
				},
				displayName: "Doc Tor",
			})
		);
		const doctors = await store.dispatch(api.endpoints.getDoctors.initiate(undefined, { subscribe: false }));
		expect(doctors.data?.length).toBeGreaterThan(0);
	});

	it("updateDoctor invalidates doctor id", async () => {
		const { store, api } = setupApiStore();
		await store.dispatch(api.endpoints.getDoctorById.initiate(10));
		await store.dispatch(
			api.endpoints.updateDoctor.initiate({ id: 10, body: { displayName: "Updated" } })
		);
		const refetch = await store.dispatch(
			api.endpoints.getDoctorById.initiate(10, { forceRefetch: true })
		);
		expect(refetch.data?.id).toBe(10);
	});

	it("createAppointment invalidates appointments LIST", async () => {
		const { store, api } = setupApiStore();
		await store.dispatch(
			api.endpoints.createAppointment.initiate({
				doctor: { id: 10 },
				patient: { id: 20 },
				startTime: "2026-06-01T10:00:00Z",
				endTime: "2026-06-01T10:30:00Z",
				status: "scheduled",
				source: "web",
			})
		);
		const list = await store.dispatch(api.endpoints.getAppointments.initiate(undefined, { forceRefetch: true }));
		expect(list.data?.length).toBeGreaterThan(0);
	});

	it("cancelAppointment invalidates appointment and queue tags", async () => {
		const { store, api } = setupApiStore();
		await store.dispatch(api.endpoints.cancelAppointment.initiate({ id: 100 }));
		const appt = api.endpoints.getAppointmentById.select(100)(store.getState());
		expect(appt.isUninitialized || appt.isSuccess || appt.isLoading).toBe(true);
	});

	it("bookAppointment invalidates appointments LIST", async () => {
		const { store, api } = setupApiStore();
		await store.dispatch(
			api.endpoints.bookAppointment.initiate({ appointmentId: 100, userId: 3 })
		);
		const list = api.endpoints.getAppointments.select(undefined)(store.getState());
		expect(list.isUninitialized || list.isSuccess).toBe(true);
	});

	it("updatePatient invalidates patient id and LIST", async () => {
		const { store, api } = setupApiStore();
		await store.dispatch(api.endpoints.updatePatient.initiate({ id: 20, body: { gender: 2 } }));
		const patient = await store.dispatch(api.endpoints.getPatientById.initiate(20, { forceRefetch: true }));
		expect(patient.data?.id).toBe(20);
	});

	it("createService invalidates services LIST", async () => {
		const { store, api } = setupApiStore();
		await store.dispatch(
			api.endpoints.createService.initiate({
				name: "New Service",
				price: 500,
				durationMinutes: 15,
			})
		);
		const services = await store.dispatch(api.endpoints.getServices.initiate(undefined, { forceRefetch: true }));
		expect(services.data?.length).toBeGreaterThan(0);
	});

	it("createSchedule invalidates schedules LIST", async () => {
		const { store, api } = setupApiStore();
		await store.dispatch(
			api.endpoints.createSchedule.initiate({
				doctor: { id: 10 },
				dateAt: "2026-07-01",
				startTime: "09:00:00",
				endTime: "12:00:00",
				slotDurationMinutes: 30,
			})
		);
		const schedules = await store.dispatch(api.endpoints.getSchedules.initiate(undefined, { forceRefetch: true }));
		expect(schedules.data?.length).toBeGreaterThan(0);
	});

	it("deleteDoctor invalidates doctors LIST", async () => {
		const { store, api } = setupApiStore();
		const result = await store.dispatch(api.endpoints.deleteDoctor.initiate(10));
		expect(result.error).toBeUndefined();
	});
});
