import * as z from "zod";

export const editPatientFormSchema = z.object({
	lastName: z.string().min(2, "Фамилия должна содержать минимум 2 символа"),
	firstName: z.string().min(2, "Имя должно содержать минимум 2 символа"),
	middleName: z.string().optional(),
	birthDate: z
		.string()
		.min(1, "Пожалуйста, укажите дату рождения")
		.refine((date) => new Date(date) <= new Date(), {
			message: "Дата рождения не может быть в будущем",
		}),
	gender: z.enum(["male", "female"], {
		required_error: "Пожалуйста, выберите пол",
	}),
	phone: z.string().min(10, "Введите корректный номер телефона"),
	email: z.string().email("Введите корректный email").min(1, "Email обязателен для заполнения"),
	policyNumber: z
		.string()
		.min(1, "Пожалуйста, укажите номер полиса ОМС")
		.length(16, "Номер полиса ОМС должен содержать 16 цифр"),
});

export type EditPatientFormValues = z.infer<typeof editPatientFormSchema>;
