"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Send } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useSearchParams } from "react-router-dom";
import * as z from "zod";

import { useRequestPasswordResetMutation, useResetPasswordMutation } from "@/store/api/apiSlice";
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

// Схема валидации для запроса сброса пароля
const requestResetSchema = z.object({
	email: z.string().email("Введите корректный email адрес"),
});

// Схема валидации для сброса пароля
const resetPasswordSchema = z
	.object({
		password: z
			.string()
			.min(8, "Пароль должен содержать минимум 8 символов")
			.regex(
				/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/,
				"Пароль должен содержать хотя бы одну заглавную букву, одну строчную букву и одну цифру"
			),
		confirmPassword: z.string(),
	})
	.refine((data) => data.password === data.confirmPassword, {
		message: "Пароли не совпадают",
		path: ["confirmPassword"],
	});

type RequestResetFormData = z.infer<typeof requestResetSchema>;
type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;

export default function ForgotPasswordPage() {
	const [requestPasswordReset] = useRequestPasswordResetMutation();
	const [resetPasswordMut] = useResetPasswordMutation();
	const [isLoading, setIsLoading] = useState(false);
	const [isSubmitted, setIsSubmitted] = useState(false);
	const [searchParams] = useSearchParams();
	const token = searchParams.get("token");

	// Форма запроса сброса пароля
	const requestResetForm = useForm<RequestResetFormData>({
		resolver: zodResolver(requestResetSchema),
	});

	// Форма сброса пароля
	const resetPasswordForm = useForm<ResetPasswordFormData>({
		resolver: zodResolver(resetPasswordSchema),
	});

	const handleRequestReset = async (data: RequestResetFormData) => {
		setIsLoading(true);

		try {
			await requestPasswordReset({ email: data.email }).unwrap();
			toast.success("Инструкции отправлены", {
				description:
					"Проверьте вашу электронную почту для сброса пароля",
			});
			setIsSubmitted(true);
		} catch (error) {
			toast.error("Ошибка", {
				description: "Не удалось отправить инструкции по сбросу пароля",
			});
		} finally {
			setIsLoading(false);
		}
	};

	const handleResetPassword = async (data: ResetPasswordFormData) => {
		setIsLoading(true);

		try {
			await resetPasswordMut({ token: token!, password: data.password }).unwrap();
			toast.success("Пароль успешно изменен", {
				description: "Теперь вы можете войти с новым паролем",
			});
			// Перенаправляем на страницу входа
			window.location.href = "/auth/login";
		} catch (error) {
			toast.error("Ошибка", {
				description: "Не удалось изменить пароль",
			});
		} finally {
			setIsLoading(false);
		}
	};

	return (
		<div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-blue-50 via-white to-purple-50 p-4">
			<div className="w-full max-w-md fade-in">
				<div className="mb-8 text-center">
					<h1 className="text-3xl font-bold gradient-heading">
						Клиника Менеджмент
					</h1>
					<p className="mt-2 text-slate-600">
						{token ? "Сброс пароля" : "Восстановление пароля"}
					</p>
				</div>

				<Card className="gradient-card hover-glow">
					<div className="h-1 w-full bg-gradient-to-r from-blue-500 via-purple-500 to-teal-500"></div>
					<CardHeader>
						<CardTitle className="text-xl">
							{token ? "Введите новый пароль" : "Забыли пароль?"}
						</CardTitle>
						<CardDescription>
							{token
								? "Введите новый пароль для вашей учетной записи"
								: isSubmitted
								? "Инструкции по сбросу пароля отправлены на вашу почту"
								: "Введите ваш email для получения инструкций по сбросу пароля"}
						</CardDescription>
					</CardHeader>
					<CardContent>
						{token ? (
							<form
								onSubmit={resetPasswordForm.handleSubmit(
									handleResetPassword
								)}
								className="space-y-4"
							>
								<div className="space-y-2">
									<Label htmlFor="password">
										Новый пароль
									</Label>
									<Input
										id="password"
										type="password"
										{...resetPasswordForm.register(
											"password"
										)}
										className="light-input hover-scale"
									/>
									{resetPasswordForm.formState.errors
										.password && (
										<p className="text-sm text-red-500">
											{
												resetPasswordForm.formState
													.errors.password.message
											}
										</p>
									)}
								</div>

								<div className="space-y-2">
									<Label htmlFor="confirmPassword">
										Подтвердите пароль
									</Label>
									<Input
										id="confirmPassword"
										type="password"
										{...resetPasswordForm.register(
											"confirmPassword"
										)}
										className="light-input hover-scale"
									/>
									{resetPasswordForm.formState.errors
										.confirmPassword && (
										<p className="text-sm text-red-500">
											{
												resetPasswordForm.formState
													.errors.confirmPassword
													.message
											}
										</p>
									)}
								</div>

								<Button
									type="submit"
									className="gradient-button w-full"
									disabled={isLoading}
									aria-disabled={isLoading}
								>
									{isLoading ? (
										<span className="flex items-center gap-2">
											<span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
											Изменение пароля...
										</span>
									) : (
										<span className="flex items-center gap-2">
											<Send size={18} />
											Изменить пароль
										</span>
									)}
								</Button>
							</form>
						) : isSubmitted ? (
							<div className="text-center">
								<div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-green-100">
									<Send className="h-6 w-6 text-green-600" />
								</div>
								<p className="text-slate-600">
									Мы отправили инструкции по сбросу пароля на
									адрес{" "}
									<strong>
										{requestResetForm.getValues("email")}
									</strong>
									. Пожалуйста, проверьте вашу электронную
									почту.
								</p>
								<p className="mt-4 text-sm text-slate-500">
									Не получили письмо?{" "}
									<button
										type="button"
										className="text-blue-600 hover:text-blue-800 hover:underline"
										onClick={() => setIsSubmitted(false)}
									>
										Отправить повторно
									</button>
								</p>
							</div>
						) : (
							<form
								onSubmit={requestResetForm.handleSubmit(
									handleRequestReset
								)}
								className="space-y-4"
							>
								<div className="space-y-2">
									<Label htmlFor="email">Email</Label>
									<Input
										id="email"
										type="email"
										placeholder="email@example.com"
										{...requestResetForm.register("email")}
										className="light-input hover-scale"
									/>
									{requestResetForm.formState.errors
										.email && (
										<p className="text-sm text-red-500">
											{
												requestResetForm.formState
													.errors.email.message
											}
										</p>
									)}
								</div>

								<Button
									type="submit"
									className="gradient-button w-full"
									disabled={isLoading}
									aria-disabled={isLoading}
								>
									{isLoading ? (
										<span className="flex items-center gap-2">
											<span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
											Отправка...
										</span>
									) : (
										<span className="flex items-center gap-2">
											<Send size={18} />
											Отправить инструкции
										</span>
									)}
								</Button>
							</form>
						)}
					</CardContent>
					<CardFooter className="flex flex-col space-y-4">
						<Link
							to="/auth/login"
							className="flex items-center gap-1 text-sm text-blue-600 hover:text-blue-800 hover:underline"
						>
							<ArrowLeft size={16} />
							Вернуться на страницу входа
						</Link>
					</CardFooter>
				</Card>
			</div>
		</div>
	);
}
