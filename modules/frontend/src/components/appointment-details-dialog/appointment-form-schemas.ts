import * as z from "zod";

export const formSchema = z.object({
	status: z.enum([
		"scheduled",
		"confirmed",
		"in_progress",
		"completed",
		"cancelled",
		"no_show",
	]),
	diagnosis: z.string().optional(),
});

export const reviewFormSchema = z.object({
	rating: z.number().min(1, "Выберите оценку").max(5, "Оценка должна быть от 1 до 5"),
	reviewText: z.string().optional(),
});

export type FormValues = z.infer<typeof formSchema>;
export type ReviewFormValues = z.infer<typeof reviewFormSchema>;
