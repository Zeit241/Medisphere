import type { ClinicService } from "@/api/types";

export function parseClinicPrice(price: number | string | null | undefined): number {
	if (price == null) return NaN;
	const n = typeof price === "number" ? price : Number(String(price).replace(",", "."));
	return Number.isFinite(n) ? n : NaN;
}

/** Рубли или «—», если цены нет или она не положительная */
export function formatClinicServicePriceFromFields(
	price: number | string | null | undefined
): string {
	const n = parseClinicPrice(price);
	if (!Number.isFinite(n) || n <= 0) return "—";
	return new Intl.NumberFormat("ru-RU", {
		style: "currency",
		currency: "RUB",
		maximumFractionDigits: 0,
	}).format(n);
}

export function formatClinicServicePrice(service: Pick<ClinicService, "price">): string {
	return formatClinicServicePriceFromFields(service.price);
}
