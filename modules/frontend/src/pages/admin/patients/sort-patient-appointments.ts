import { compareAsc } from "date-fns";

export function sortPatientAppointments<T extends { startTime: string }>(response: T[]): T[] {
	return [...response].sort((a, b) => {
		const dateA = new Date(a.startTime);
		const dateB = new Date(b.startTime);
		const now = new Date();
		const aIsPast = dateA < now;
		const bIsPast = dateB < now;
		if (aIsPast !== bIsPast) {
			return aIsPast ? 1 : -1;
		}
		if (!aIsPast && !bIsPast) {
			return compareAsc(dateA, dateB);
		}
		return compareAsc(dateB, dateA);
	});
}
