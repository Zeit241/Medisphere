import { ArrowLeft } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";

import { Patient } from "@/api/types";
import { BookAppointmentForm } from "@/components/book-appointment-form";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/AuthContext";
import { useEffect, useState } from "react";
import { toast } from "sonner";

import {
	useLazyGetPatientByIdQuery,
	useLazyGetPatientsQuery,
} from "@/store/api/apiSlice";

export default function BookAppointmentPage() {
	const { user } = useAuth();
	const navigate = useNavigate();
	const [fullUser, setFullUser] = useState<Patient | null>(null);
	const [isLoading, setIsLoading] = useState(true);
	const [fetchPatient] = useLazyGetPatientByIdQuery();
	const [fetchPatients] = useLazyGetPatientsQuery();

	useEffect(() => {
		const fetchFullUser = async () => {
			if (!user?.id) {
				setIsLoading(false);
				return;
			}

			try {
				if (user.patientId) {
					const patient = await fetchPatient(user.patientId).unwrap();
					setFullUser(patient);
				} else {
					toast.error("Пользователь не является пациентом");
					navigate("/patient");
				}
			} catch (error) {
				console.error("Ошибка при загрузке данных пациента:", error);
				toast.error("Ошибка при загрузке данных");
				try {
					const allPatients = await fetchPatients().unwrap();
					const patient = allPatients.find((p) => p.user.id === user.id);
					if (patient) {
						setFullUser(patient);
					}
				} catch (fallbackError) {
					console.error("Ошибка при загрузке пациентов:", fallbackError);
				}
			} finally {
				setIsLoading(false);
			}
		};
		void fetchFullUser();
	}, [user, navigate, fetchPatient, fetchPatients]);

	return (
		<div className="flex flex-1 flex-col">
			<main className="py-8">
				<div className="container mx-auto px-4">
					<div className="mb-8 fade-in">
						<Button
							variant="ghost"
							asChild
							className="mb-2 hover:bg-slate-100 hover-scale"
						>
							<Link to="/patient">
								<ArrowLeft className="mr-2 h-4 w-4" />
								Назад на главную
							</Link>
						</Button>
						<h2 className="text-3xl font-bold gradient-heading">
							Запись на приём
						</h2>
						<p className="mt-2 text-slate-600">
							Выберите специалиста и удобное время
						</p>
					</div>

					{isLoading ? (
						<div className="flex justify-center py-8">
							<div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
						</div>
					) : fullUser ? (
						<BookAppointmentForm
							patient={fullUser}
							onClose={() => {
								navigate("/patient/");
							}}
						/>
					) : (
						<div className="text-center py-8 text-slate-600">
							Не удалось загрузить данные пациента
						</div>
					)}
				</div>
			</main>
		</div>
	);
}
