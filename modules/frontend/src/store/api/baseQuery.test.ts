import { http, HttpResponse } from "msw";
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";

import { baseQueryWithReauth } from "./baseQuery";
import { API_BASE } from "./utils";

describe("baseQueryWithReauth", () => {
	beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
	afterEach(() => server.resetHandlers());
	afterAll(() => server.close());

	const locationAssign = vi.fn();

	beforeEach(() => {
		Object.defineProperty(window, "location", {
			value: { href: "http://localhost/", assign: locationAssign },
			writable: true,
			configurable: true,
		});
	});

	afterEach(() => {
		localStorage.clear();
	});

	it("adds Authorization header when token is stored", async () => {
		let capturedAuth: string | null = null;
		server.use(
			http.get(`${API_BASE}/users`, ({ request }) => {
				capturedAuth = request.headers.get("Authorization");
				return HttpResponse.json([]);
			})
		);
		localStorage.setItem("token", "secret-token");
		await baseQueryWithReauth("users", {} as never, {});
		expect(capturedAuth).toBe("Bearer secret-token");
	});

	it("returns data on successful request", async () => {
		server.use(http.get(`${API_BASE}/users`, () => HttpResponse.json([{ id: 1 }])));
		const result = await baseQueryWithReauth("users", {} as never, {});
		expect(result.data).toEqual([{ id: 1 }]);
		expect(result.error).toBeUndefined();
	});

	it("clears storage and redirects on 401 for protected routes", async () => {
		localStorage.setItem("token", "expired");
		localStorage.setItem("user", "{}");
		server.use(http.get(`${API_BASE}/users`, () => HttpResponse.json({}, { status: 401 })));
		await baseQueryWithReauth("users", {} as never, {});
		expect(localStorage.getItem("token")).toBeNull();
		expect(localStorage.getItem("user")).toBeNull();
		expect(window.location.href).toBe("/auth/login");
	});

	it("does not redirect on 401 for auth/login", async () => {
		window.location.href = "http://localhost/dashboard";
		server.use(
			http.post(`${API_BASE}/auth/login`, () => HttpResponse.json({}, { status: 401 }))
		);
		await baseQueryWithReauth(
			{ url: "auth/login", method: "POST", body: {} },
			{} as never,
			{}
		);
		expect(window.location.href).toBe("http://localhost/dashboard");
	});

	it("passes through non-401 errors", async () => {
		server.use(http.get(`${API_BASE}/users`, () => HttpResponse.json({ msg: "fail" }, { status: 500 })));
		const result = await baseQueryWithReauth("users", {} as never, {});
		expect(result.error?.status).toBe(500);
	});

	it("sets Content-Type application/json on requests", async () => {
		let contentType: string | null = null;
		server.use(
			http.get(`${API_BASE}/users`, ({ request }) => {
				contentType = request.headers.get("Content-Type");
				return HttpResponse.json([]);
			})
		);
		await baseQueryWithReauth("users", {} as never, {});
		expect(contentType).toBe("application/json");
	});
});
