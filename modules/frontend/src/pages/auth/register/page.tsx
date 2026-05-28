"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff, UserPlus } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { PatternFormat } from "react-number-format";
import { Link, useNavigate } from "react-router-dom";
import { z } from "zod";

import { useRegisterMutation } from "@/store/api/apiSlice";
import { Button } from "@/components/ui/button";
import {
	Card,
	CardContent,
	CardDescription,
	CardFooter,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";

// Схема валидации с помощью Zod
const registerSchema = z
	.object({
		email: z.string().email("Введите корректный email адрес"),
		password: z
			.string()
			.min(8, "Пароль должен содержать не менее 8 символов")
			.regex(
				/[A-Z]/,
				"Пароль должен содержать хотя бы одну заглавную букву"
			)
			.regex(
				/[a-z]/,
				"Пароль должен содержать хотя бы одну строчную букву"
			)
			.regex(/[0-9]/, "Пароль должен содержать хотя бы одну цифру"),
		confirmPassword: z.string(),
		phone: z
			.string()
			.min(10, "Телефон должен содержать не менее 10 символов"),
		fio: z.string().min(1, "ФИО обязательно"),
	})
	.refine((data) => data.password === data.confirmPassword, {
		message: "Пароли не совпадают",
		path: ["confirmPassword"],
	});

type RegisterFormData = z.infer<typeof registerSchema>;

export default function RegisterPage() {
	const router = useNavigate();
	const [registerMut] = useRegisterMutation();
	const [isLoading, setIsLoading] = useState(false);
	const [showPassword, setShowPassword] = useState(false);
	const [showConfirmPassword, setShowConfirmPassword] = useState(false);

	const {
		register,
		handleSubmit,
		formState: { errors },
	} = useForm<RegisterFormData>({
		resolver: zodResolver(registerSchema),
		defaultValues: {
			email: "",
			password: "",
			confirmPassword: "",
			phone: "",
			fio: "",
		},
	});

	const onSubmit = async (data: RegisterFormData) => {
		setIsLoading(true);

		try {
			const response = await registerMut({
				email: data.email,
				password: data.password,
				confirmPassword: data.confirmPassword,
				phone: data.phone,
				fio: data.fio,
			}).unwrap();

			// Сохраняем токены и данные пользователя
			if (response.success && response.data) {
				if (response.data.token) {
					localStorage.setItem("token", response.data.token);
				}
				if (response.data.user) {
					localStorage.setItem("user", JSON.stringify(response.data.user));
				}
			}

			toast.success("Регистрация выполнена успешно", {
				description: "Теперь вы можете войти в систему",
			});

			// Перенаправление на страницу входа
			router("/auth/login");
		} catch (error: unknown) {
			console.error("Ошибка при регистрации:", error);

			const errorData =
				error && typeof error === "object" && "data" in error
					? (error as { data?: unknown }).data
					: undefined;

			if (errorData && typeof errorData === "object") {
				Object.entries(errorData as Record<string, unknown>).forEach(([field, messages]) => {
					toast.error(`Ошибка в поле ${field}`, {
						description: Array.isArray(messages)
							? String(messages[0])
							: String(messages),
					});
				});
			} else if (errorData !== undefined) {
				toast.error("Ошибка регистрации", {
					description: String(errorData),
				});
			} else {
				toast.error("Ошибка регистрации", {
					description:
						"Не удалось зарегистрировать пользователя. Пожалуйста, попробуйте позже.",
				});
			}
		} finally {
			setIsLoading(false);
		}
	};

	const toggleShowPassword = () => {
		setShowPassword(!showPassword);
	};

	const toggleShowConfirmPassword = () => {
		setShowConfirmPassword(!showConfirmPassword);
	};

	return (
		<div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-blue-50 via-white to-purple-50 p-4">
			<div className="w-full max-w-2xl fade-in">
				<div className="mb-8 text-center">
					<h1 className="text-3xl font-bold gradient-heading">
						Клиника Менеджмент
					</h1>
					<p className="mt-2 text-slate-600">
						Создайте новую учетную запись
					</p>
				</div>

				<Card className="gradient-card hover-glow">
					<div className="h-1 w-full bg-gradient-to-r from-blue-500 via-purple-500 to-teal-500"></div>
					<CardHeader>
						<CardTitle className="text-xl">Регистрация</CardTitle>
						<CardDescription>
							Заполните форму для создания учетной записи
						</CardDescription>
					</CardHeader>
					<CardContent>
						<form
							onSubmit={handleSubmit(onSubmit)}
							className="space-y-4"
						>
							<div className="space-y-2">
								<Label htmlFor="email">
									Email{" "}
									<span className="text-red-500">*</span>
								</Label>
								<Input
									id="email"
									{...register("email")}
									type="email"
									placeholder="email@example.com"
									className={`light-input hover-scale ${
										errors.email ? "border-red-500" : ""
									}`}
								/>
								{errors.email && (
									<p className="text-xs text-red-500">
										{errors.email.message}
									</p>
								)}
							</div>

							<div className="space-y-2">
								<Label htmlFor="fio">
									ФИО{" "}
									<span className="text-red-500">*</span>
								</Label>
								<Input
									id="fio"
									{...register("fio")}
									type="text"
									placeholder="Иванов Иван Иванович"
									className={`light-input hover-scale ${
										errors.fio ? "border-red-500" : ""
									}`}
								/>
								{errors.fio && (
									<p className="text-xs text-red-500">
										{errors.fio.message}
									</p>
								)}
							</div>

							<div className="space-y-2">
								<Label htmlFor="phone">
									Телефон{" "}
									<span className="text-red-500">*</span>
								</Label>
								<PatternFormat
									format="+7 (###) ###-##-##"
									mask="_"
									customInput={Input}
									onValueChange={(values) => {
										const { value } = values;
										console.log(value);
										// Устанавливаем значение в форму
										register("phone").onChange({
											target: {
												name: "phone",
												value: value,
											},
										});
									}}
									id="phone"
									type="tel"
									placeholder="+7 (___) ___-__-__"
									className={`light-input hover-scale ${
										errors.phone ? "border-red-500" : ""
									}`}
								/>
								{errors.phone && (
									<p className="text-xs text-red-500">
										{errors.phone.message}
									</p>
								)}
							</div>

							<div className="grid gap-4 md:grid-cols-2">
								<div className="space-y-2">
									<Label htmlFor="password">
										Пароль{" "}
										<span className="text-red-500">*</span>
									</Label>
									<div className="relative">
										<Input
											id="password"
											{...register("password")}
											type={
												showPassword
													? "text"
													: "password"
											}
											placeholder="••••••••"
											className={`light-input hover-scale pr-10 ${
												errors.password
													? "border-red-500"
													: ""
											}`}
										/>
										<button
											type="button"
											className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
											onClick={toggleShowPassword}
										>
											{showPassword ? (
												<EyeOff size={18} />
											) : (
												<Eye size={18} />
											)}
										</button>
									</div>
									{errors.password && (
										<p className="text-xs text-red-500">
											{errors.password.message}
										</p>
									)}
								</div>
								<div className="space-y-2">
									<Label htmlFor="confirmPassword">
										Подтверждение пароля{" "}
										<span className="text-red-500">*</span>
									</Label>
									<div className="relative">
										<Input
											id="confirmPassword"
											{...register("confirmPassword")}
											type={
												showConfirmPassword
													? "text"
													: "password"
											}
											placeholder="••••••••"
											className={`light-input hover-scale pr-10 ${
												errors.confirmPassword
													? "border-red-500"
													: ""
											}`}
										/>
										<button
											type="button"
											className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
											onClick={toggleShowConfirmPassword}
										>
											{showConfirmPassword ? (
												<EyeOff size={18} />
											) : (
												<Eye size={18} />
											)}
										</button>
									</div>
									{errors.confirmPassword && (
										<p className="text-xs text-red-500">
											{errors.confirmPassword.message}
										</p>
									)}
								</div>
							</div>

							<div className="mt-6">
								<Button
									type="submit"
									className="gradient-button w-full"
									disabled={isLoading}
									aria-disabled={isLoading}
								>
									{isLoading ? (
										<span className="flex items-center gap-2">
											<span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
											Регистрация...
										</span>
									) : (
										<span className="flex items-center gap-2">
											<UserPlus size={18} />
											Зарегистрироваться
										</span>
									)}
								</Button>
							</div>
						</form>
					</CardContent>
					<CardFooter className="flex flex-col space-y-4">
						<p className="text-center text-sm text-slate-600">
							Уже есть учетная запись?{" "}
							<Link
								to="/auth/login"
								className="text-blue-600 hover:text-blue-800 hover:underline"
							>
								Войти
							</Link>
						</p>
					</CardFooter>
				</Card>
			</div>
		</div>
	);
}
