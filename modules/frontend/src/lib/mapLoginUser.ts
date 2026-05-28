import type { LoginCurrentUserPayload, Patient, User } from "@/api/types";

export function userFromLoginPayload(
	payload: LoginCurrentUserPayload,
	roleCode?: string | null
): User {
	const role = roleCode ?? undefined;
	const base: User = {
		id: payload.id,
		email: payload.email,
		phone: payload.phone ?? "",
		firstName: payload.firstName ?? "",
		lastName: payload.lastName ?? "",
		middleName: payload.middleName ?? "",
		createdAt: payload.createdAt,
		updatedAt: payload.updatedAt,
		active: payload.active,
		role,
		patientId: payload.patientId ?? null,
		doctorId: payload.doctorId ?? null,
	};

	if (payload.patient) {
		const p = payload.patient;
		const gender: 1 | 2 = p.gender === 2 ? 2 : 1;
		const patientUser: User = { ...base };
		base.patient = {
			id: p.id,
			user: patientUser,
			birthDate: (p.birthDate as string) ?? "",
			gender,
			insuranceNumber: p.insuranceNumber ?? null,
			createdAt: p.createdAt ?? base.createdAt,
			updatedAt: p.updatedAt ?? base.updatedAt,
		};
	}

	return base;
}

/** После обновления профиля: синхронизировать User в контексте с ответом Patient */
export function mergeUserAfterPatientUpdate(
	prev: User,
	updatedPatient: Patient
): User {
	const u = updatedPatient.user;
	return {
		...u,
		role: prev.role,
		patientId: prev.patientId ?? updatedPatient.id,
		doctorId: prev.doctorId,
		patient: updatedPatient,
	};
}
