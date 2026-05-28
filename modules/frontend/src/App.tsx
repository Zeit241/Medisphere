import React from "react";
import {
	Navigate,
	Outlet,
	Route,
	BrowserRouter as Router,
	Routes,
} from "react-router-dom";

import { AppToaster } from "./components/app-toaster";
import Footer from "./components/Footer";
import { Navigation } from "./components/Header";
import { AuthProvider, useAuth } from "./contexts/AuthContext";
import AppointmentsPage from "./pages/admin/appointments/page";
import AdminCategoriesPage from "./pages/admin/categories/page";
import AdminDashboard from "./pages/admin/Dashboard";
import NewDoctorPage from "./pages/admin/doctors/new/page";
import DoctorsPage from "./pages/admin/doctors/page";
import NewPatientPage from "./pages/admin/patients/new/page";
import PatientsPage from "./pages/admin/patients/page";
import AdminServicesPage from "./pages/admin/services/page";
import StatisticsPage from "./pages/admin/statistics/page";
import ForgotPasswordPage from "./pages/auth/forgot-password/page";
import LoginPage from "./pages/auth/login/page";
import RegisterPage from "./pages/auth/register/page";
import DoctorAppointmentDetailPage from "./pages/doctor/appointments/detail/page";
import DoctorAppointmentsPage from "./pages/doctor/appointments/page";
import PatientAppointmentsPage from "./pages/patient/appointments/page";
import BookAppointmentPage from "./pages/patient/book/page";
import PatientDashboard from "./pages/patient/Dashboard";
import PatientProfilePage from "./pages/patient/profile/page";

const RootRedirect: React.FC = () => {
	const { user, isLoading } = useAuth();

	if (isLoading) {
		return (
			<div className="min-h-screen flex items-center justify-center">
				<div className="text-xl">Загрузка...</div>
			</div>
		);
	}

	if (!user) {
		return <Navigate to="/auth/login" />;
	}

	if (user.role === "admin") {
		return <Navigate to="/admin/dashboard" />;
	}

	if (user.role === "doctor") {
		return <Navigate to="/doctor/appointments" />;
	}

	return <Navigate to="/patient/dashboard" />;
};

const AuthRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
	const { user, isLoading } = useAuth();

	if (isLoading) {
		return (
			<div className="min-h-screen flex items-center justify-center">
				<div className="text-xl">Загрузка...</div>
			</div>
		);
	}

	if (user) {
		return <Navigate to="/" />;
	}

	return <>{children}</>;
};

const PrivateRoute: React.FC<{ children: React.ReactNode }> = ({
	children,
}) => {
	const { user, isLoading } = useAuth();

	if (isLoading) {
		return (
			<div className="min-h-screen flex items-center justify-center">
				<div className="text-xl">Загрузка...</div>
			</div>
		);
	}

	if (!user) {
		return <Navigate to="/auth/login" />;
	}

	return <>{children}</>;
};

const AdminRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
	const { user } = useAuth();

	if (!user || user.role !== "admin") {
		return <Navigate to="/" />;
	}

	return <>{children}</>;
};

const DoctorRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
	const { user } = useAuth();

	if (!user || user.role !== "doctor") {
		return <Navigate to="/" />;
	}

	return <>{children}</>;
};

export const App: React.FC = () => {
	return (
		<>
			<Router>
				<AuthProvider>
					<div className="min-h-screen flex flex-col bg-background">
						<Navigation />
						<Routes>
							<Route path="/" element={<RootRedirect />} />

							<Route
								path="/auth"
								element={
									<AuthRoute>
										<Outlet />
									</AuthRoute>
								}
							>
								<Route path="login" element={<LoginPage />} />
								<Route
									path="register"
									element={<RegisterPage />}
								/>
								<Route
									path="forgot-password"
									element={<ForgotPasswordPage />}
								/>
							</Route>

							<Route
								path="/patient"
								element={
									<PrivateRoute>
										<Outlet />
									</PrivateRoute>
								}
							>
								<Route
									index
									element={
										<Navigate to="/patient/dashboard" />
									}
								/>
								<Route
									path="dashboard"
									element={<PatientDashboard />}
								/>
								<Route
									path="appointments"
									element={<PatientAppointmentsPage />}
								/>
								<Route path="book">
									<Route
										index
										element={<BookAppointmentPage />}
									/>
									<Route
										path="new"
										element={<BookAppointmentPage />}
									/>
								</Route>
								<Route
									path="profile"
									element={<PatientProfilePage />}
								/>
							</Route>

							<Route
								path="/doctor"
								element={
									<PrivateRoute>
										<DoctorRoute>
											<Outlet />
										</DoctorRoute>
									</PrivateRoute>
								}
							>
								<Route
									index
									element={<Navigate to="/doctor/appointments" />}
								/>
								<Route
									path="appointments"
									element={<DoctorAppointmentsPage />}
								/>
								<Route
									path="appointments/:id"
									element={<DoctorAppointmentDetailPage />}
								/>
							</Route>

							<Route
								path="/admin"
								element={
									<PrivateRoute>
										<AdminRoute>
											<Outlet />
										</AdminRoute>
									</PrivateRoute>
								}
							>
								<Route
									index
									element={<Navigate to="/admin/dashboard" />}
								/>
								<Route
									path="dashboard"
									element={<AdminDashboard />}
								/>
								<Route path="appointments">
									<Route
										index
										element={<AppointmentsPage />}
									/>
								</Route>
								<Route path="doctors">
									<Route index element={<DoctorsPage />} />
									<Route
										path="new"
										element={<NewDoctorPage />}
									/>
								</Route>
								<Route
									path="statistics"
									element={<StatisticsPage />}
								/>
								<Route
									path="categories"
									element={<AdminCategoriesPage />}
								/>
								<Route path="services" element={<AdminServicesPage />} />
								<Route path="patients">
									<Route index element={<PatientsPage />} />
									<Route
										path="new"
										element={<NewPatientPage />}
									/>
								</Route>
							</Route>
						</Routes>
						<Footer />
					</div>
				</AuthProvider>
			</Router>
			<AppToaster />
		</>
	);
};

export default App;
