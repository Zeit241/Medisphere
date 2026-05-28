import { z } from "zod";

export const bookingFormSchema = z
	.object({
		step: z.number().min(1).max(2),
		searchMode: z.enum(["by_specialty", "by_service"]),
		specialty: z.string(),
		service: z.string(),
		doctor: z.string().min(1, "Выберите врача"),
		date: z.string().min(1, "Выберите дату"),
		time: z.string().min(1, "Выберите время"),
		slot_id: z.number().optional(),
		reason: z.string().optional(),
		notificationType: z.enum(["email", "none"], {
			required_error: "Выберите способ уведомления",
		}),
	})
	.superRefine((val, ctx) => {
		if (val.searchMode === "by_specialty" && !val.specialty.trim()) {
			ctx.addIssue({
				code: z.ZodIssueCode.custom,
				message: "Выберите специальность",
				path: ["specialty"],
			});
		}
		if (val.searchMode === "by_service" && !val.service.trim()) {
			ctx.addIssue({
				code: z.ZodIssueCode.custom,
				message: "Выберите услугу",
				path: ["service"],
			});
		}
	});

export type BookingFormValues = z.infer<typeof bookingFormSchema>;
