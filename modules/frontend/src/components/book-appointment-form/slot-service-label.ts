import type { AvailableAppointmentSlot } from "@/api/types";

export function slotServiceLabel(slot: AvailableAppointmentSlot): string {
	if (slot.service?.name) return slot.service.name;
	if (slot.serviceId != null) return `Услуга #${slot.serviceId}`;
	return "Услуга не указана";
}
