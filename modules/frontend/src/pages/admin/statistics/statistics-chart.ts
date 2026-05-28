import type { DailyReportAppointmentRow } from "@/api/types";

export interface ChartDataItem {
	name: string;
	count: number;
}

export function getStatusLabel(status: string): string {
	const statusLabels: Record<string, string> = {
		scheduled: "Запланировано",
		confirmed: "Подтверждено",
		in_progress: "В процессе",
		completed: "Завершено",
		cancelled: "Отменено",
		no_show: "Неявка",
		available: "Доступно",
	};
	return statusLabels[status] || status;
}

export function prepareChartData(
	appointments: DailyReportAppointmentRow[],
	statisticsType: string
): ChartDataItem[] {
	switch (statisticsType) {
		case "status":
			return Object.entries(
				appointments.reduce((acc: Record<string, number>, item) => {
					const status = item.status ?? "unknown";
					if (!acc[status]) {
						acc[status] = 0;
					}
					acc[status]++;
					return acc;
				}, {})
			).map(
				([status, count]): ChartDataItem => ({
					name: getStatusLabel(status),
					count: Number(count),
				})
			);
		case "doctors":
			return Object.entries(
				appointments.reduce((acc: Record<string, number>, item) => {
					const doctorName = item.doctorDisplayName ?? "Не указан";
					if (!acc[doctorName]) {
						acc[doctorName] = 0;
					}
					acc[doctorName]++;
					return acc;
				}, {})
			).map(
				([doctor, count]): ChartDataItem => ({
					name: doctor,
					count: Number(count),
				})
			);
		default:
			return Object.entries(
				appointments.reduce((acc: Record<string, number>, item) => {
					const doctorName = item.doctorDisplayName ?? "Не указан";
					if (!acc[doctorName]) {
						acc[doctorName] = 0;
					}
					acc[doctorName]++;
					return acc;
				}, {})
			).map(
				([doctor, count]): ChartDataItem => ({
					name: doctor,
					count: Number(count),
				})
			);
	}
}
