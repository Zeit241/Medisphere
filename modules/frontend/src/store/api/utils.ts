import type { ApiResponse } from "@/api/types";

export const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8085";
export const API_BASE = `${API_URL}/api`;

export function unwrapList<T>(data: unknown): T[] {
	if (Array.isArray(data)) return data as T[];
	const wrapped = data as ApiResponse<T[]>;
	if (wrapped?.data) return wrapped.data;
	return [];
}

export function unwrapEntity<T>(data: unknown, fallbackMessage: string): T {
	if (data && typeof data === "object" && "id" in data && !("success" in data)) {
		return data as T;
	}
	const wrapped = data as ApiResponse<T>;
	if (wrapped?.data) return wrapped.data;
	throw new Error((wrapped as ApiResponse<T>)?.message || fallbackMessage);
}

export function unwrapDoctorDto(data: unknown): import("@/api/types").Doctor {
	return unwrapEntity<import("@/api/types").Doctor>(data, "Врач не найден");
}

/** Для сущностей с id и ответов ApiResponse<T> */
export function unwrapWrappedOrEntity<T extends { id?: unknown }>(
	data: unknown,
	notFoundMessage: string
): T {
	return unwrapEntity<T>(data, notFoundMessage);
}

export function doctorsQueryParams(params?: import("@/api/types").DoctorsQueryParams) {
	if (!params) return undefined;
	const query: Record<string, string | number> = {};
	if (params.q != null && String(params.q).trim() !== "") query.q = String(params.q).trim();
	if (params.serviceId != null && Number.isFinite(Number(params.serviceId))) {
		query.serviceId = Number(params.serviceId);
	}
	if (params.limit != null) query.limit = params.limit;
	if (params.offset != null) query.offset = params.offset;
	if (params.sortBy != null) query.sortBy = params.sortBy;
	if (params.sortOrder != null) query.sortOrder = params.sortOrder;
	return query;
}

export function isAuthPath(url: string): boolean {
	const u = url.toLowerCase();
	return (
		u.includes("auth/login") ||
		u.includes("auth/register") ||
		u.includes("register-with-patient")
	);
}
