"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff, LogIn } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { z } from "zod";

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
import { useAuth } from "@/contexts/AuthContext";
import { toast } from "sonner";

const loginSchema = z.object({
	email: z.string().email("Введите корректный email адрес"),
	password: z.string().min(1, "Пароль обязателен"),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function LoginPage() {
	const navigate = useNavigate();
	const [isLoading, setIsLoading] = useState(false);
	const [showPassword, setShowPassword] = useState(false);

	const { login } = useAuth();

	const {
		register,
		handleSubmit,
		formState: { errors },
	} = useForm<LoginFormData>({
		resolver: zodResolver(loginSchema),
		defaultValues: {
			email: "",
			password: "",
		},
	});

	const onSubmit = async (data: LoginFormData) => {
		setIsLoading(true);

		try {
			const result = await login(data.email, data.password);
			
			if (result.success) {
				toast.success("Вход выполнен успешно", {
					description: "Добро пожаловать в систему Клиника Менеджмент",
				});
				navigate("/");
			} else {
				toast.error("Ошибка входа", {
					description: "Неверное имя пользователя или пароль",
				});
			}
		} catch (error) {
			toast.error("Ошибка входа", {
			});
		} finally {
			setIsLoading(false);
		}
	};

	const handleFormSubmit = (e: React.FormEvent) => {
		e.preventDefault();
		handleSubmit(onSubmit)(e);
	};

	const toggleShowPassword = () => {
		setShowPassword(!showPassword);
	};

	return (
		<div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-blue-50 via-white to-purple-50 p-4">
			<div className="w-full max-w-md fade-in">
				<div className="mb-8 text-center">
					<h1 className="text-3xl font-bold gradient-heading">
						Клиника Менеджмент
					</h1>
					<p className="mt-2 text-slate-600">
						Войдите в свою учетную запись
					</p>
				</div>

				<Card className="gradient-card hover-glow">
					<div className="h-1 w-full bg-gradient-to-r from-blue-500 via-purple-500 to-teal-500"></div>
					<CardHeader>
						<CardTitle className="text-xl">
							Вход в систему
						</CardTitle>
						<CardDescription>
							Введите свои учетные данные для входа
						</CardDescription>
					</CardHeader>
					<CardContent>
						<form
							onSubmit={handleFormSubmit}
							className="space-y-4"
						>
							<div className="space-y-2">
								<Label htmlFor="email">
									Email
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
									<p className="text-sm text-red-500">
										{errors.email.message}
									</p>
								)}
							</div>

							<div className="space-y-2">
								<div className="flex items-center justify-between">
									<Label htmlFor="password">Пароль</Label>
									{/* <Link
										to="/auth/forgot-password"
										className="text-xs text-blue-600 hover:text-blue-800 hover:underline"
									>
										Забыли пароль?
									</Link> */}
								</div>
								<div className="relative">
									<Input
										id="password"
										{...register("password")}
										type={
											showPassword ? "text" : "password"
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
									<p className="text-sm text-red-500">
										{errors.password.message}
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
										Вход...
									</span>
								) : (
									<span className="flex items-center gap-2">
										<LogIn size={18} />
										Войти
									</span>
								)}
							</Button>
						</form>
					</CardContent>
					<CardFooter className="flex flex-col space-y-4">
						<div className="relative flex w-full items-center justify-center">
							<div className="absolute inset-x-0 top-1/2 h-px -translate-y-1/2 bg-slate-200"></div>
							<span className="relative bg-white px-2 text-sm text-slate-500">
								или
							</span>
						</div>

						<p className="text-center text-sm text-slate-600">
							Нет учетной записи?{" "}
							<Link
								to="/auth/register"
								className="text-blue-600 hover:text-blue-800 hover:underline"
							>
								Зарегистрироваться
							</Link>
						</p>
					</CardFooter>
				</Card>
			</div>
		</div>
	);
}
