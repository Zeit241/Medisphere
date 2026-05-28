import { describe, expect, it } from "vitest";

import type { Appointment } from "@/api/types";

import {
	diagnosisToFormString,
	formatDiagnosisDisplay,
	statusLabels,
} from "./appointment-display-helpers";

describe("diagnosisToFormString", () => {
	it("returns empty string for nullish values", () => {
		expect(diagnosisToFormString(null)).toBe("");
		expect(diagnosisToFormString("")).toBe("");
	});

	it("returns string diagnosis as-is", () => {
		expect(diagnosisToFormString("J06.9")).toBe("J06.9");
	});

	it("returns code or id from object diagnosis", () => {
		expect(diagnosisToFormString({ id: 5, code: "J06.9", name: "Cold", category: null })).toBe("J06.9");
		expect(diagnosisToFormString({ id: 7, code: "", name: "X", category: null })).toBe("7");
	});
});

describe("formatDiagnosisDisplay", () => {
	it("formats object diagnosis as code and name", () => {
		expect(
			formatDiagnosisDisplay({ id: 1, code: "J06.9", name: "Acute infection", category: null })
		).toBe("J06.9 — Acute infection");
	});

	it("exposes Russian status labels", () => {
		const statuses: Appointment["status"][] = [
			"scheduled",
			"confirmed",
			"completed",
			"cancelled",
		];
		for (const status of statuses) {
			expect(statusLabels[status].length).toBeGreaterThan(0);
		}
	});
});
