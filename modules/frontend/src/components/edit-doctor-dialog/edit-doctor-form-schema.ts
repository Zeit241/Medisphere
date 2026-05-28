import * as z from "zod";

export const editDoctorFormSchema = z.object({
	lastName: z.string().min(2, "Фамилия должна содержать минимум 2 символа"),
	firstName: z.string().min(2, "Имя должно содержать минимум 2 символа"),
	middleName: z.string().optional(),
	phone: z
		.string()
		.regex(/^\+7 \(\d{3}\) \d{3}-\d{2}-\d{2}$/, "Неверный формат телефона")
		.optional(),
	email: z.string().email("Неверный формат email").min(1, "Email обязателен"),
	experienceYears: z
		.string()
		.optional()
		.refine((val) => {
			if (!val) return true;
			const num = parseInt(val);
			return !isNaN(num) && num >= 0 && num <= 80;
		}, "Стаж должен быть от 0 до 80 лет"),
	bio: z
		.string()
		.max(50, "Биография не должна превышать 50 символов")
		.optional(),
	specializationIds: z.array(z.number()).optional(),
	photo: z.string().optional(),
});

export type EditDoctorFormValues = z.infer<typeof editDoctorFormSchema>;
