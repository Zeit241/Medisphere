import { z } from "zod";

export const patientProfileSchema = z.object({
	last_name: z.string().min(1, "Пожалуйста, введите фамилию"),
	first_name: z.string().min(1, "Пожалуйста, введите имя"),
	middle_name: z.string().optional(),
	birth_date: z
		.string()
		.min(1, "Пожалуйста, выберите дату рождения")
		.refine((date) => {
			const birthDate = new Date(date);
			const today = new Date();
			return birthDate < today;
		}, "Дата рождения не может быть в будущем"),
	gender: z.enum(["male", "female"], {
		required_error: "Пожалуйста, выберите пол",
	}),
	phone: z.string().min(1, "Пожалуйста, введите номер телефона"),
	insurance_number: z.string().optional(),
});

export type PatientProfileFormValues = z.infer<typeof patientProfileSchema>;
