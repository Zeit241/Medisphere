import type { Appointment } from "@/api/types";

export function diagnosisToFormString(d: Appointment["diagnosis"]): string {
	if (d == null || d === "") return "";
	if (typeof d === "string") return d;
	return d.code || String(d.id ?? "");
}

export function formatDiagnosisDisplay(d: Appointment["diagnosis"]): string {
	if (d == null || d === "") return "";
	if (typeof d === "string") return d;
	const code = d.code ? `${d.code}` : "";
	const name = d.name || "";
	return [code, name].filter(Boolean).join(" — ");
}

export const statusLabels: Record<Appointment["status"], string> = {
	scheduled: "Запланирован",
	confirmed: "Подтвержден",
	in_progress: "В процессе",
	completed: "Завершен",
	cancelled: "Отменен",
	no_show: "Неявка",
	available: "Доступен",
};
