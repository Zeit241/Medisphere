import { describe, expect, it } from "vitest";

import {
	formatClinicServicePrice,
	formatClinicServicePriceFromFields,
	parseClinicPrice,
} from "./format-clinic-service-price";

describe("parseClinicPrice", () => {
	it("returns NaN for null and undefined", () => {
		expect(Number.isNaN(parseClinicPrice(null))).toBe(true);
		expect(Number.isNaN(parseClinicPrice(undefined))).toBe(true);
	});

	it("parses numeric and comma-decimal strings", () => {
		expect(parseClinicPrice(1500)).toBe(1500);
		expect(parseClinicPrice("1500,50")).toBe(1500.5);
	});

	it("returns NaN for invalid strings", () => {
		expect(Number.isNaN(parseClinicPrice("not-a-price"))).toBe(true);
	});
});

describe("formatClinicServicePriceFromFields", () => {
	it('returns em dash for missing or non-positive prices', () => {
		expect(formatClinicServicePriceFromFields(null)).toBe("—");
		expect(formatClinicServicePriceFromFields(0)).toBe("—");
		expect(formatClinicServicePriceFromFields(-100)).toBe("—");
	});

	it("formats positive prices in RUB", () => {
		const formatted = formatClinicServicePriceFromFields(1500);
		expect(formatted).toContain("1");
		expect(formatted).toContain("500");
		expect(formatted).toMatch(/₽|RUB/);
	});
});

describe("formatClinicServicePrice", () => {
	it("delegates to formatClinicServicePriceFromFields", () => {
		expect(formatClinicServicePrice({ price: 2000 })).toContain("2");
		expect(formatClinicServicePrice({ price: null })).toBe("—");
	});
});
