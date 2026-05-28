import { describe, expect, it } from "vitest";

import {
	doctorsQueryParams,
	isAuthPath,
	unwrapEntity,
	unwrapList,
} from "./utils";

describe("unwrapList", () => {
	it("returns arrays and unwraps ApiResponse data", () => {
		expect(unwrapList([{ id: 1 }])).toEqual([{ id: 1 }]);
		expect(unwrapList({ success: true, data: [{ id: 2 }], message: "", status: 200 })).toEqual([
			{ id: 2 },
		]);
		expect(unwrapList({ success: true, data: null, message: "", status: 200 })).toEqual([]);
	});
});

describe("unwrapEntity", () => {
	it("returns bare entity with id", () => {
		expect(unwrapEntity({ id: 3, name: "x" }, "err")).toEqual({ id: 3, name: "x" });
	});

	it("throws when wrapped response has no data", () => {
		expect(() => unwrapEntity({ success: false, message: "Not found", data: null, status: 404 }, "fallback")).toThrow(
			"Not found"
		);
		expect(() => unwrapEntity(null, "fallback")).toThrow("fallback");
	});
});

describe("doctorsQueryParams and isAuthPath", () => {
	it("detects auth endpoints and builds doctor query params", () => {
		expect(doctorsQueryParams({ q: "  ivan  ", serviceId: 5, limit: 10, offset: 0, sortBy: "rating", sortOrder: "desc" })).toEqual({
			q: "ivan",
			serviceId: 5,
			limit: 10,
			offset: 0,
			sortBy: "rating",
			sortOrder: "desc",
		});
		expect(doctorsQueryParams({ q: "   " })).toEqual({});
		expect(isAuthPath("auth/login")).toBe(true);
		expect(isAuthPath("/api/auth/register-with-patient")).toBe(true);
		expect(isAuthPath("users/1")).toBe(false);
	});
});
