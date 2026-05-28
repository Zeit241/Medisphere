import type { Appointment } from "@/api/types";

export const statusLabels: Record<string, string> = {
	scheduled: "Запланирован",
	confirmed: "Подтверждён",
	in_progress: "В процессе",
	completed: "Завершён",
	cancelled: "Отменён",
	no_show: "Неявка",
	available: "Слот",
};

export function formatDiagnosisLabel(a: Appointment): string {
	const d = a.diagnosis;
	if (d == null) return "—";
	if (typeof d === "string") return d || "—";
	return [d.code, d.name].filter(Boolean).join(" — ") || "—";
}

export function patientShortName(a: Appointment): string {
	const p = a.patient;
	if (!p?.user) return a.patientId != null ? `Пациент #${a.patientId}` : "—";
	const u = p.user;
	return [u.lastName, u.firstName, u.middleName].filter(Boolean).join(" ");
}

export const terminalStatuses = new Set([
	"completed",
	"cancelled",
	"no_show",
]);
