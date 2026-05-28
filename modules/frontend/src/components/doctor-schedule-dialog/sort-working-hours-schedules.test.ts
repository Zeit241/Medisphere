import { describe, expect, it, vi } from "vitest";

import type { Schedule } from "@/api/types";

import { sortWorkingHoursSchedules } from "./sort-working-hours-schedules";

function schedule(dateAt: string, id: number): Schedule {
	return {
		id,
		doctorId: 1,
		roomId: 1,
		dateAt,
		startTime: "09:00:00",
		endTime: "17:00:00",
		slotDurationMinutes: 30,
		createdAt: "2024-01-01T00:00:00Z",
		updatedAt: "2024-01-01T00:00:00Z",
	};
}

describe("sortWorkingHoursSchedules", () => {
	it("sorts future dates before past dates", () => {
		vi.useFakeTimers();
		vi.setSystemTime(new Date("2026-06-15T12:00:00Z"));
		const past = schedule("2026-06-01", 1);
		const future = schedule("2026-07-01", 2);
		expect(sortWorkingHoursSchedules([past, future])).toEqual([future, past]);
		vi.useRealTimers();
	});

	it("sorts ascending within the same past/future group", () => {
		vi.useFakeTimers();
		vi.setSystemTime(new Date("2026-06-15T12:00:00Z"));
		const a = schedule("2026-08-01", 1);
		const b = schedule("2026-09-01", 2);
		expect(sortWorkingHoursSchedules([b, a])).toEqual([a, b]);
		vi.useRealTimers();
	});

	it("pushes entries without dateAt to the end", () => {
		const withDate = schedule("2026-08-01", 1);
		const noDate = { ...withDate, dateAt: "" };
		expect(sortWorkingHoursSchedules([noDate, withDate])[0]).toBe(withDate);
	});

	it("does not mutate the input array", () => {
		const input = [schedule("2026-08-02", 1), schedule("2026-08-01", 2)];
		const copy = [...input];
		sortWorkingHoursSchedules(input);
		expect(input).toEqual(copy);
	});
});
