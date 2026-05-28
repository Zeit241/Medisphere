import type { Schedule } from "@/api/types";
import { compareAsc, isBefore, isEqual, parseISO, startOfDay } from "date-fns";

export function sortWorkingHoursSchedules(schedules: Schedule[]): Schedule[] {
	return [...schedules].sort((a, b) => {
		if (!a.dateAt) return 1;
		if (!b.dateAt) return -1;
		try {
			const dateA = parseISO(a.dateAt);
			const dateB = parseISO(b.dateAt);
			const isPastA =
				isBefore(startOfDay(dateA), startOfDay(new Date())) ||
				isEqual(startOfDay(dateA), startOfDay(new Date()));
			const isPastB =
				isBefore(startOfDay(dateB), startOfDay(new Date())) ||
				isEqual(startOfDay(dateB), startOfDay(new Date()));
			if (isPastA !== isPastB) {
				return isPastA ? 1 : -1;
			}
			return compareAsc(dateA, dateB);
		} catch {
			return 0;
		}
	});
}
