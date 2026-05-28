import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { PatternFormat } from "react-number-format";
import { Link, useNavigate } from "react-router-dom";
import { z } from "zod";
// @ts-ignore
import { transliterate as tr } from "transliteration";

import { useCreatePatientMutation } from "@/store/api/apiSlice";
import { Button } from "@/components/ui/button";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import {
	Form,
	FormControl,
	FormField,
	FormItem,
	FormLabel,
	FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { toast } from "sonner";
import React from "react";

const patientSchema = z.object({
	lastName: z.string().min(2, "Фамилия должна содержать минимум 2 символа").default(""),
	firstName: z.string().min(2, "Имя должно содержать минимум 2 символа").default(""),
	middleName: z.string().optional(),
	birthDate: z.string()
		.min(1, "Пожалуйста, укажите дату рождения")
		.refine((date) => new Date(date) <= new Date(), {
			message: "Дата рождения не может быть в будущем"
		})
		.default(""),
	gender: z.enum(["male", "female"], {
		required_error: "Пожалуйста, выберите пол",
	}).default("male"),
	phone: z.string().min(10, "Введите корректный номер телефона").default(""),
	email: z.string().email("Введите корректный email").min(1, "Email обязателен для заполнения"),
	policyNumber: z
		.string()
		.min(1, "Пожалуйста, укажите номер полиса ОМС")
		.length(16, "Номер полиса ОМС должен содержать 16 цифр")
		.default(""),
});

type PatientFormValues = z.infer<typeof patientSchema>;

interface GeneratedCredentials {
	login: string;
	password: string;
}

const generateRandomPassword = () => {
	const lowercase = "abcdefghijklmnopqrstuvwxyz";
	const uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	const numbers = "0123456789";
	const all = lowercase + uppercase + numbers;

	// Генерируем случайные числа с помощью Web Crypto API
	const array = new Uint32Array(12);
	window.crypto.getRandomValues(array);

	// Обеспечиваем наличие как минимум одного символа каждого типа
	let password = "";
	password += lowercase[array[0] % lowercase.length];
	password += uppercase[array[1] % uppercase.length];
	password += numbers[array[2] % numbers.length];

	// Добавляем остальные случайные символы
	for (let i = 4; i < 12; i++) {
		password += all[array[i] % all.length];
	}

	// Перемешиваем пароль
	return password
		.split("")
		.sort(() => 0.5 - Math.random())
		.join("");
};

const StepTwo = ({ credentials }: { credentials: GeneratedCredentials }) => {
	return (
		<div className="space-y-6">
			<div className="text-center mb-6">
				<h3 className="text-xl font-semibold mb-2">
					Данные для входа в систему
				</h3>
				<p className="text-gray-600">
					Сохраните эти данные, они понадобятся для входа в систему
				</p>
			</div>
			<div className="space-y-4 p-6 border rounded-lg bg-gray-50">
				<div>
					<Label className="text-sm font-medium">Логин (Email):</Label>
					<div className="mt-1 p-3 bg-white border rounded-md">
						{credentials.login}
					</div>
				</div>
				<div>
					<Label className="text-sm font-medium">Пароль:</Label>
					<div className="mt-1 p-3 bg-white border rounded-md font-mono">
						{credentials.password}
					</div>
				</div>
			</div>
		</div>
	);
};

export default function NewPatientPage() {
	const [createPatient] = useCreatePatientMutation();
	const [isSubmitting, setIsSubmitting] = useState(false);
	const [currentStep, setCurrentStep] = useState(1);
	const [credentials, setCredentials] = useState<GeneratedCredentials | null>(null);
	const navigate = useNavigate();

	const form = useForm<PatientFormValues>({
		resolver: zodResolver(patientSchema),
		defaultValues: {
			gender: "male",
		},
		mode: "onChange",
	});

	React.useEffect(() => {
		const subscription = form.watch(() => {
			console.log("Form errors:", form.formState.errors);
		});
		return () => subscription.unsubscribe();
	}, [form.watch, form.formState.errors]);

	const onSubmit = async (data: PatientFormValues) => {
		// Если первый шаг - переходим на второй и генерируем пароль
		if (currentStep === 1) {
			const generatedPassword = generateRandomPassword();
			setCredentials({
				login: data.email,
				password: generatedPassword,
			});
			setCurrentStep(2);
			return;
		}

		// Если второй шаг - создаем пациента
		setIsSubmitting(true);

		try {
			// Создаем пациента с данными пользователя
			await createPatient({
				user: {
					email: data.email || "",
					phone: data.phone,
					firstName: data.firstName,
					lastName: data.lastName,
					middleName: data.middleName,
				},
				birthDate: data.birthDate,
				gender: data.gender === "male" ? 1 : 2,
				insuranceNumber: data.policyNumber,
			}).unwrap();

			toast.success("Пациент добавлен", {
				description: "Новый пациент успешно добавлен в систему",
			});
			navigate("/admin/patients");
		} catch (error: any) {
			if (error.response?.data) {
				const errors = error.response.data;
				// Обрабатываем все поля с ошибками
				Object.entries(errors).forEach(([field, messages]) => {
					if (Array.isArray(messages) && messages.length > 0) {
						const fieldName = field.replace("user.", "") as keyof PatientFormValues;
						form.setError(fieldName, {
							type: "manual",
							message: messages[0]
						});
					}
				});
				setCurrentStep(1);
			} else {
				toast.error("Ошибка при добавлении пациента");
			}
		} finally {
			setIsSubmitting(false);
		}
	};

	return (
		<div className="flex flex-1 flex-col">
			<main className="py-8">
				<div className="container mx-auto px-4">
					<div className="mb-8">
						<Button variant="ghost" asChild className="mb-2 hover:">
							<Link to="/admin/patients">
								<ArrowLeft className="mr-2 h-4 w-4" />
								Назад к списку пациентов
							</Link>
						</Button>
						<h2 className="text-3xl font-bold gradient-heading">
							Добавить нового пациента
						</h2>
					</div>

					<Card className="mx-auto max-w-2xl gradient-card">
						<div className="h-1 w-full bg-gradient-to-r from-purple-500 via-teal-500 to-blue-500"></div>
						<CardHeader>
							<CardTitle>
								{currentStep === 1
									? "Информация о пациенте"
									: "Данные для входа"}
							</CardTitle>
							<CardDescription>
								{currentStep === 1
									? "Введите данные нового пациента"
									: "Проверьте сгенерированные данные для входа"}
							</CardDescription>
						</CardHeader>
						<CardContent>
							{currentStep === 1 ? (
								<Form {...form}>
									<form
										onSubmit={form.handleSubmit(onSubmit)}
										className="space-y-6"
									>
										<div className="grid gap-4 md:grid-cols-2">
											<FormField
												control={form.control}
												name="lastName"
												render={({ field }) => (
													<FormItem>
														<FormLabel>
															Фамилия
														</FormLabel>
														<FormControl>
															<Input
																placeholder="Иванов"
																className=" border-slate-700"
																{...field}
															/>
														</FormControl>
														<FormMessage />
													</FormItem>
												)}
											/>
											<FormField
												control={form.control}
												name="firstName"
												render={({ field }) => (
													<FormItem>
														<FormLabel>
															Имя
														</FormLabel>
														<FormControl>
															<Input
																placeholder="Иван"
																className=" border-slate-700"
																{...field}
															/>
														</FormControl>
														<FormMessage />
													</FormItem>
												)}
											/>
										</div>

										<div className="grid gap-4 md:grid-cols-2">
											<FormField
												control={form.control}
												name="middleName"
												render={({ field }) => (
													<FormItem>
														<FormLabel>
															Отчество
														</FormLabel>
														<FormControl>
															<Input
																placeholder="Иванович"
																className=" border-slate-700"
																{...field}
															/>
														</FormControl>
														<FormMessage />
													</FormItem>
												)}
											/>
											<FormField
												control={form.control}
												name="birthDate"
												render={({ field }) => (
													<FormItem>
														<FormLabel>
															Дата рождения
														</FormLabel>
														<FormControl>
															<Input
																type="date"
																className=" border-slate-700"
																{...field}
															/>
														</FormControl>
														<FormMessage />
													</FormItem>
												)}
											/>
										</div>

										<FormField
											control={form.control}
											name="gender"
											render={({ field }) => (
												<FormItem>
													<FormLabel>Пол</FormLabel>
													<FormControl>
														<RadioGroup
															onValueChange={
																field.onChange
															}
															value={field.value}
															className="flex gap-4"
														>
															<div className="flex items-center space-x-2">
																<RadioGroupItem
																	value="male"
																	id="male"
																/>
																<Label htmlFor="male">
																	Мужской
																</Label>
															</div>
															<div className="flex items-center space-x-2">
																<RadioGroupItem
																	value="female"
																	id="female"
																/>
																<Label htmlFor="female">
																	Женский
																</Label>
															</div>
														</RadioGroup>
													</FormControl>
													<FormMessage />
												</FormItem>
											)}
										/>

										<div className="grid gap-4 md:grid-cols-2">
											<FormField
												control={form.control}
												name="phone"
												render={({ field }) => (
													<FormItem>
														<FormLabel>
															Телефон
														</FormLabel>
														<FormControl>
															<PatternFormat
																customInput={
																	Input
																}
																format="+7 (###) ###-##-##"
																mask="_"
																placeholder="+7 (900) 123-45-67"
																className="border-slate-700"
																value={
																	field.value
																}
																onValueChange={(
																	values
																) => {
																	field.onChange(
																		values.value
																	);
																}}
															/>
														</FormControl>
														<FormMessage />
													</FormItem>
												)}
											/>
											<FormField
												control={form.control}
												name="email"
												render={({ field }) => (
													<FormItem>
														<FormLabel>
															Email
														</FormLabel>
														<FormControl>
															<Input
																type="email"
																placeholder="patient@example.com"
																className="border-slate-700"
																{...field}
															/>
														</FormControl>
														<FormMessage />
													</FormItem>
												)}
											/>
										</div>

										<FormField
											control={form.control}
											name="policyNumber"
											render={({ field }) => (
												<FormItem>
													<FormLabel>
														Номер полиса ОМС
													</FormLabel>
													<FormControl>
														<PatternFormat
															customInput={
																Input
															}
															format="#### #### #### ####"
															mask="_"
															placeholder="1234 5678 9012 3456"
															className="border-slate-700"
															value={
																field.value
															}
															onValueChange={(
																values
															) => {
																field.onChange(
																	values.value.replace(
																		/\s/g,
																		""
																	)
																);
															}}
														/>
													</FormControl>
													<FormMessage />
												</FormItem>
											)}
										/>

										<div className="flex justify-end gap-4">
											<Button
												variant="outline"
												asChild
												className="border-slate-700 hover:"
											>
												<Link to="/admin/patients">
													Отмена
												</Link>
											</Button>
											<Button
												type="submit"
												className="gradient-button"
											>
												Далее
											</Button>
										</div>
									</form>
								</Form>
							) : credentials ? (
								<div>
									<StepTwo credentials={credentials} />
									<div className="flex justify-end gap-4 mt-6">
										<Button
											variant="outline"
											onClick={() => setCurrentStep(1)}
											className="border-slate-700"
										>
											Назад
										</Button>
										<Button
											onClick={form.handleSubmit(
												onSubmit
											)}
											disabled={isSubmitting}
											className="gradient-button"
										>
											{isSubmitting
												? "Сохранение..."
												: "Завершить"}
										</Button>
									</div>
								</div>
							) : null}
						</CardContent>
					</Card>
				</div>
			</main>
		</div>
	);
}
