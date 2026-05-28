import { startOfDay } from "date-fns";
import * as z from "zod";

export const scheduleFormSchema = z
	.object({
		singleDate: z.date({
			required_error: "Выберите дату",
			invalid_type_error: "Некорректная дата",
		}),
		roomId: z.string().optional(),
		startTime: z.string({
			required_error: "Укажите время начала работы",
		}),
		endTime: z.string({
			required_error: "Укажите время окончания работы",
		}),
		appointmentDuration: z.string({
			required_error: "Укажите длительность приема",
		}),
	})
	.superRefine((data, ctx) => {
		const [startHour, startMinute] = data.startTime.split(":").map(Number);
		const [endHour, endMinute] = data.endTime.split(":").map(Number);
		const startInMinutes = startHour * 60 + startMinute;
		const endInMinutes = endHour * 60 + endMinute;

		const now = new Date();
		const currentHour = now.getHours();
		const currentMinute = now.getMinutes();
		const currentTimeInMinutes = currentHour * 60 + currentMinute;

		const isToday = (date: Date | undefined) => {
			if (!date) return false;
			const today = new Date();
			return (
				date.getDate() === today.getDate() &&
				date.getMonth() === today.getMonth() &&
				date.getFullYear() === today.getFullYear()
			);
		};

		if (isToday(data.singleDate)) {
			if (startInMinutes <= currentTimeInMinutes) {
				ctx.addIssue({
					code: z.ZodIssueCode.custom,
					message:
						"Время начала не может быть меньше или равно текущему времени",
					path: ["startTime"],
				});
			}
		}

		if (endInMinutes <= startInMinutes) {
			ctx.addIssue({
				code: z.ZodIssueCode.custom,
				message: "Время окончания должно быть позже времени начала",
				path: ["endTime"],
			});
		}

		if (!data.singleDate) {
			ctx.addIssue({
				code: z.ZodIssueCode.custom,
				message: "Выберите дату",
				path: ["singleDate"],
			});
		} else if (data.singleDate < startOfDay(new Date())) {
			ctx.addIssue({
				code: z.ZodIssueCode.custom,
				message: "Дата не может быть меньше текущей",
				path: ["singleDate"],
			});
		}
	});

export type ScheduleFormData = z.infer<typeof scheduleFormSchema>;
