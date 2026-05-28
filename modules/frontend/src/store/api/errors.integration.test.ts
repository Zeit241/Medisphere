import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";

import { server } from "@/test/msw/server";
import { setupApiStore } from "@/test/store-utils";
import { API_BASE } from "./utils";

describe("error handling integration", () => {
	beforeEach(() => {
		Object.defineProperty(window, "location", {
			value: { href: "http://localhost/app" },
			writable: true,
			configurable: true,
		});
		localStorage.setItem("token", "expired-token");
		localStorage.setItem("user", JSON.stringify({ id: 1 }));
	});

	it("401 on getUsers clears auth and redirects to login", async () => {
		server.use(http.get(`${API_BASE}/users`, () => HttpResponse.json({}, { status: 401 })));
		const { store, api } = setupApiStore();
		const result = await store.dispatch(api.endpoints.getUsers.initiate());
		expect(result.error).toBeDefined();
		expect(localStorage.getItem("token")).toBeNull();
		expect(localStorage.getItem("user")).toBeNull();
		expect(window.location.href).toBe("/auth/login");
	});

	it("401 on getPatients clears storage", async () => {
		server.use(http.get(`${API_BASE}/patients`, () => HttpResponse.json({}, { status: 401 })));
		const { store, api } = setupApiStore();
		await store.dispatch(api.endpoints.getPatients.initiate());
		expect(localStorage.getItem("token")).toBeNull();
	});

	it("500 on getAppointments returns error status", async () => {
		server.use(
			http.get(`${API_BASE}/appointments`, () =>
				HttpResponse.json({ message: "Server error" }, { status: 500 })
			)
		);
		const { store, api } = setupApiStore();
		const result = await store.dispatch(api.endpoints.getAppointments.initiate());
		expect(result.error).toBeDefined();
		expect(result.error && "status" in result.error ? result.error.status : null).toBe(500);
	});

	it("503 on createService returns error", async () => {
		server.use(
			http.post(`${API_BASE}/services`, () =>
				HttpResponse.json({ message: "Unavailable" }, { status: 503 })
			)
		);
		const { store, api } = setupApiStore();
		const result = await store.dispatch(
			api.endpoints.createService.initiate({
				name: "Fail",
				price: 100,
				durationMinutes: 10,
			})
		);
		expect(result.error).toBeDefined();
		expect(result.error && "status" in result.error ? result.error.status : null).toBe(503);
	});

	it("502 on getDoctors surfaces fetch error", async () => {
		server.use(http.get(`${API_BASE}/doctors`, () => HttpResponse.json({}, { status: 502 })));
		const { store, api } = setupApiStore();
		const result = await store.dispatch(api.endpoints.getDoctors.initiate(undefined));
		expect(result.error).toBeDefined();
		expect(result.error && "status" in result.error ? result.error.status : null).toBe(502);
	});
});
