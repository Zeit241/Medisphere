import { describe, expect, it } from "vitest";

import type { LoginCurrentUserPayload, Patient, User } from "@/api/types";

import { mergeUserAfterPatientUpdate, userFromLoginPayload } from "./mapLoginUser";

const basePayload: LoginCurrentUserPayload = {
	id: 1,
	email: "user@test.com",
	phone: "+79001112233",
	firstName: "Ivan",
	lastName: "Ivanov",
	middleName: "Ivanovich",
	createdAt: "2024-01-01T00:00:00Z",
	updatedAt: "2024-01-02T00:00:00Z",
	active: true,
	patientId: 10,
	doctorId: null,
};

describe("userFromLoginPayload", () => {
	it("maps core fields and optional role", () => {
		const user = userFromLoginPayload(basePayload, "patient");
		expect(user.id).toBe(1);
		expect(user.email).toBe("user@test.com");
		expect(user.role).toBe("patient");
		expect(user.patientId).toBe(10);
		const withoutRole = userFromLoginPayload({ ...basePayload, phone: null, middleName: null });
		expect(withoutRole.phone).toBe("");
		expect(withoutRole.role).toBeUndefined();
	});

	it("defaults empty strings for missing name fields", () => {
		const user = userFromLoginPayload({ ...basePayload, firstName: null, lastName: null });
		expect(user.firstName).toBe("");
		expect(user.lastName).toBe("");
	});

	it("embeds patient with gender 2 when provided", () => {
		const user = userFromLoginPayload({
			...basePayload,
			patient: { id: 10, birthDate: "1990-01-01", gender: 2 },
		});
		expect(user.patient?.id).toBe(10);
		expect(user.patient?.gender).toBe(2);
		expect(user.patient?.user.id).toBe(user.id);
	});

	it("defaults patient gender to 1 when not 2", () => {
		const user = userFromLoginPayload({
			...basePayload,
			patient: { id: 10, birthDate: "1990-01-01", gender: 9 },
		});
		expect(user.patient?.gender).toBe(1);
	});
});

describe("mergeUserAfterPatientUpdate", () => {
	it("preserves role and doctorId from previous user", () => {
		const prev: User = {
			...userFromLoginPayload(basePayload, "patient"),
			doctorId: null,
		};
		const updatedPatient: Patient = {
			id: 10,
			user: { ...prev, firstName: "Petr" },
			birthDate: "1990-01-01",
			gender: 1,
			insuranceNumber: null,
			createdAt: prev.createdAt,
			updatedAt: prev.updatedAt,
		};
		const merged = mergeUserAfterPatientUpdate({ ...prev, patientId: null }, updatedPatient);
		expect(merged.role).toBe("patient");
		expect(merged.firstName).toBe("Petr");
		expect(merged.patient).toBe(updatedPatient);
		expect(merged.patientId).toBe(10);
	});
});
