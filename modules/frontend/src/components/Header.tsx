import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";
import { Menu, X } from "lucide-react";
import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

export const Navigation: React.FC = () => {
	const { user, logout } = useAuth();
	const navigate = useNavigate();
	const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

	const handleLogout = () => {
		logout();
		navigate("/auth/login");
	};

	const toggleMobileMenu = () => {
		setIsMobileMenuOpen(!isMobileMenuOpen);
	};

	return (
		<header className="sticky top-0 z-10 border-b border-border bg-background/80 backdrop-blur-md">
			<div className="container mx-auto flex h-16 items-center justify-between py-4 px-4">
				<h1 className="text-xl md:text-2xl font-bold text-foreground">
					Клиника Менеджмент
				</h1>

				{/* Бургер-меню для мобильных устройств */}
				<button
					className="md:hidden p-2"
					onClick={toggleMobileMenu}
					aria-label="Открыть меню"
				>
					{isMobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
				</button>

				{/* Десктопное меню */}
				<nav className="hidden md:flex items-center gap-3">
					{user ? (
						<>
							{user.role === "admin" ? (
								<>
									<Link to="/admin" className="nav-item">
										Главная
									</Link>
									<Link
										to="/admin/doctors"
										className="nav-item"
									>
										Врачи
									</Link>
									<Link
										to="/admin/patients"
										className="nav-item"
									>
										Пациенты
									</Link>
									<Link
										to="/admin/categories"
										className="nav-item"
									>
										Категории
									</Link>
									<Link
										to="/admin/services"
										className="nav-item"
									>
										Услуги
									</Link>
									<Link
										to="/admin/appointments"
										className="nav-item nav-item-active"
									>
										Приёмы
									</Link>
									<Link
										to="/admin/statistics"
										className="nav-item"
									>
										Статистика
									</Link>
								</>
							) : user.role === "doctor" ? (
								<>
									<Link
										to="/doctor/appointments"
										className="nav-item nav-item-active"
									>
										Мои приёмы
									</Link>
								</>
							) : (
								<>
									<Link to="/patient" className="nav-item">
										Главная
									</Link>
									<Link
										to="/patient/appointments"
										className="nav-item nav-item-active"
									>
										Мои приёмы
									</Link>
									<Link
										to="/patient/book"
										className="nav-item"
									>
										Запись на приём
									</Link>
									<Link
										to="/patient/profile"
										className="nav-item"
									>
										Профиль
									</Link>
								</>
							)}
							<div className="flex items-center gap-2">
								<ThemeToggle />
								<Button onClick={handleLogout}>Выйти</Button>
							</div>
						</>
					) : (
						<div className="flex items-center gap-2">
							<ThemeToggle />
							<Link to="/auth/login" className="nav-item">
								Вход
							</Link>
							<Link to="/auth/register" className="nav-item">
								Регистрация
							</Link>
						</div>
					)}
				</nav>

				{/* Мобильное меню */}
				{isMobileMenuOpen && (
					<nav className="absolute top-16 left-0 right-0 bg-background border-b border-border p-4 md:hidden">
						<div className="flex flex-col gap-4">
							{user ? (
								<>
									{user.role === "admin" ? (
										<>
											<Link
												to="/admin"
												className="nav-item"
												onClick={toggleMobileMenu}
											>
												Главная
											</Link>
											<Link
												to="/admin/doctors"
												className="nav-item"
												onClick={toggleMobileMenu}
											>
												Врачи
											</Link>
											<Link
												to="/admin/patients"
												className="nav-item"
												onClick={toggleMobileMenu}
											>
												Пациенты
											</Link>
											<Link
												to="/admin/categories"
												className="nav-item"
												onClick={toggleMobileMenu}
											>
												Категории
											</Link>
											<Link
												to="/admin/services"
												className="nav-item"
												onClick={toggleMobileMenu}
											>
												Услуги
											</Link>
											<Link
												to="/admin/appointments"
												className="nav-item"
												onClick={toggleMobileMenu}
											>
												Приёмы
											</Link>
											<Link
												to="/admin/statistics"
												className="nav-item"
												onClick={toggleMobileMenu}
											>
												Статистика
											</Link>
										</>
									) : user.role === "doctor" ? (
										<Link
											to="/doctor/appointments"
											className="nav-item"
											onClick={toggleMobileMenu}
										>
											Мои приёмы
										</Link>
									) : (
										<>
											<Link
												to="/patient"
												className="nav-item"
												onClick={toggleMobileMenu}
											>
												Главная
											</Link>
											<Link
												to="/patient/appointments"
												className="nav-item"
												onClick={toggleMobileMenu}
											>
												Мои приёмы
											</Link>
											<Link
												to="/patient/book"
												className="nav-item"
												onClick={toggleMobileMenu}
											>
												Запись на приём
											</Link>
											<Link
												to="/patient/profile"
												className="nav-item"
												onClick={toggleMobileMenu}
											>
												Профиль
											</Link>
										</>
									)}
									<div className="flex flex-col gap-2">
										<ThemeToggle />
										<Button onClick={handleLogout}>
											Выйти
										</Button>
									</div>
								</>
							) : (
								<div className="flex flex-col gap-2">
									<ThemeToggle />
									<Link
										to="/auth/login"
										className="nav-item"
										onClick={toggleMobileMenu}
									>
										Вход
									</Link>
									<Link
										to="/auth/register"
										className="nav-item"
										onClick={toggleMobileMenu}
									>
										Регистрация
									</Link>
								</div>
							)}
						</div>
					</nav>
				)}
			</div>
		</header>
	);
};
