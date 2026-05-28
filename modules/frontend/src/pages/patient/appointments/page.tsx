import { format, isPast } from "date-fns";
import { ru } from "date-fns/locale";
import {
	ArrowLeft,
	Calendar,
	Clock,
	FileText,
	Loader2,
	MapPin,
} from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";

import { Appointment } from "@/api/types";
import { formatClinicServicePriceFromFields } from "@/lib/format-clinic-service-price";
import { AppointmentDetailsDialog } from "@/components/appointment-details-dialog";
import Footer from "@/components/Footer";
import { useAuth } from "@/contexts/AuthContext";
import {
	AlertDialog,
	AlertDialogAction,
	AlertDialogCancel,
	AlertDialogContent,
	AlertDialogDescription,
	AlertDialogFooter,
	AlertDialogHeader,
	AlertDialogTitle,
	AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { toast } from "sonner";

import {
	useCancelAppointmentMutation,
	useGetAppointmentsByPatientQuery,
} from "@/store/api/apiSlice";

export default function PatientAppointmentsPage() {
	const { user } = useAuth();
	const patientId = user?.patientId;
	const {
		data: appointments = [],
		isLoading,
		isError,
		refetch,
	} = useGetAppointmentsByPatientQuery(patientId!, { skip: !patientId });
	const [cancelAppointment] = useCancelAppointmentMutation();

	const error = !patientId
		? "Пользователь не является пациентом"
		: isError
			? "Ошибка при загрузке приёмов"
			: null;

	const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);
	const [isDetailsDialogOpen, setIsDetailsDialogOpen] = useState(false);

	const handleCancelAppointment = async (appointmentId: number) => {
		try {
			await cancelAppointment({ id: appointmentId, body: {} }).unwrap();
			toast.success("Приём успешно отменён");
			refetch();
		} catch {
			toast.error("Не удалось отменить приём");
		}
	};

	// Разделяем приёмы на предстоящие и прошедшие
	const upcomingAppointments = appointments.filter(
		(appointment) => {
			const isFuture = !isPast(new Date(appointment.startTime));
			const isActive = ["scheduled", "confirmed", "in_progress"].includes(appointment.status);
			return isFuture && isActive;
		}
	);

	const pastAppointments = appointments.filter(
		(appointment) => {
			const isPastAppointment = isPast(new Date(appointment.startTime));
			const isCompleted = ["completed", "cancelled", "no_show"].includes(appointment.status);
			return isPastAppointment || isCompleted;
		}
	);

	return (
		<div className="flex min-h-screen flex-col gradient-bg">
			<main className="flex-1 py-8">
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
							Мои приёмы
						</h2>
						<p className="mt-2 text-slate-600">
							История и предстоящие приёмы
						</p>
					</div>

					<Tabs defaultValue="upcoming" className="mb-8 slide-up">
						<TabsList className="grid w-full grid-cols-2 bg-slate-100">
							<TabsTrigger value="upcoming">
								Предстоящие
							</TabsTrigger>
							<TabsTrigger value="past">История</TabsTrigger>
						</TabsList>
						<TabsContent value="upcoming" className="mt-6">
							{isLoading ? (
								<div className="flex justify-center py-8">
									<Loader2 className="h-8 w-8 animate-spin text-blue-600" />
								</div>
							) : error ? (
								<div className="text-center text-red-600 py-8">
									{error}
								</div>
							) : upcomingAppointments.length === 0 ? (
								<div className="text-center text-slate-600 py-8">
									У вас нет предстоящих приёмов
								</div>
							) : (
								<div className="space-y-6">
									{upcomingAppointments.map(
										(appointment, index) => (
											<AppointmentCard
												key={appointment.id}
												date={format(
													new Date(appointment.startTime),
													"d MMMM yyyy",
													{ locale: ru }
												)}
												time={format(
													new Date(appointment.startTime),
													"HH:mm"
												)}
												doctor={
													appointment.doctor?.displayName ||
													`${appointment.doctor?.user?.lastName || ""} ${appointment.doctor?.user?.firstName || ""}`.trim() ||
													"Врач не указан"
												}
												specialty={
													appointment.doctor?.specialization ||
													""
												}
												room={
													appointment.room?.code ||
													appointment.room?.name ||
													"Не указан"
												}
												serviceName={appointment.service?.name ?? null}
												servicePriceLabel={
													appointment.service
														? formatClinicServicePriceFromFields(
																appointment.service.price
															)
														: null
												}
												status={appointment.status as "scheduled" | "completed" | "cancelled"}
												animationDelay={`stagger-${
													index + 1
												}`}
												onCancel={() =>
													handleCancelAppointment(
														appointment.id
													)
												}
											/>
										)
									)}
								</div>
							)}
						</TabsContent>
						<TabsContent value="past" className="mt-6">
							{isLoading ? (
								<div className="flex justify-center py-8">
									<Loader2 className="h-8 w-8 animate-spin text-blue-600" />
								</div>
							) : error ? (
								<div className="text-center text-red-600 py-8">
									{error}
								</div>
							) : pastAppointments.length === 0 ? (
								<div className="text-center text-slate-600 py-8">
									У вас нет завершённых приёмов
								</div>
							) : (
								<div className="space-y-6">
									{pastAppointments.map(
										(appointment, index) => (
											<AppointmentCard
												key={appointment.id}
												appointment={appointment}
												date={format(
													new Date(appointment.startTime),
													"d MMMM yyyy",
													{ locale: ru }
												)}
												time={format(
													new Date(appointment.startTime),
													"HH:mm"
												)}
												doctor={
													appointment.doctor?.displayName ||
													`${appointment.doctor?.user?.lastName || ""} ${appointment.doctor?.user?.firstName || ""}`.trim() ||
													"Врач не указан"
												}
												specialty={
													appointment.doctor?.specializations?.[0]?.name ||
													""
												}
												room={
													appointment.room?.code ||
													appointment.room?.name ||
													"Не указан"
												}
												serviceName={appointment.service?.name ?? null}
												servicePriceLabel={
													appointment.service
														? formatClinicServicePriceFromFields(
																appointment.service.price
															)
														: null
												}
												status={
													appointment.status === "completed"
														? "completed"
														: appointment.status === "cancelled"
														? "cancelled"
														: "completed"
												}
												animationDelay={`stagger-${
													index + 1
												}`}
												onViewDetails={() => {
													if (appointment) {
														setSelectedAppointment(appointment);
														setIsDetailsDialogOpen(true);
													}
												}}
											/>
										)
									)}
								</div>
							)}
						</TabsContent>
					</Tabs>

					<div className="flex justify-center slide-up stagger-5">
						<Button asChild className="gradient-button hover-scale">
							<Link to="/patient/book">
								Записаться на новый приём
							</Link>
						</Button>
					</div>
				</div>
			</main>
			<Footer />
			<AppointmentDetailsDialog
				appointment={selectedAppointment}
				open={isDetailsDialogOpen}
				onOpenChange={setIsDetailsDialogOpen}
				onUpdate={() => void refetch()}
				isPatientView={true}
			/>
		</div>
	);
}

interface AppointmentCardProps {
	appointment?: Appointment;
	date: string;
	time: string;
	doctor: string;
	specialty: string;
	room: string;
	status: "scheduled" | "completed" | "cancelled";
	/** Название услуги (если была выбрана при записи) */
	serviceName?: string | null;
	/** Уже отформатированная цена или null, если услуги нет */
	servicePriceLabel?: string | null;
	animationDelay?: string;
	onCancel?: () => void;
	onViewDetails?: () => void;
}

function AppointmentCard({
	appointment: _appointment,
	date,
	time,
	doctor,
	specialty,
	room,
	status,
	serviceName,
	servicePriceLabel,
	animationDelay,
	onCancel,
	onViewDetails,
}: AppointmentCardProps) {
	return (
		<Card
			className={`gradient-card overflow-hidden slide-up ${animationDelay} hover-lift`}
		>
			<div
				className={`h-1 w-full ${
					status === "scheduled"
						? "bg-gradient-to-r from-blue-500 to-purple-500"
						: status === "completed"
						? "bg-gradient-to-r from-emerald-500 to-teal-500"
						: "bg-gradient-to-r from-red-500 to-orange-500"
				}`}
			></div>
			<CardContent className="p-6">
				<div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
					<div className="flex flex-col gap-4 md:flex-row md:items-center">
						<div
							className={`rounded-full p-3 ${
								status === "scheduled"
									? "bg-blue-100 text-blue-600"
									: status === "completed"
									? "bg-emerald-100 text-emerald-600"
									: "bg-red-100 text-red-600"
							} ${status === "scheduled" ? "pulse" : ""}`}
						>
							{status === "scheduled" ? (
								<Calendar className="h-6 w-6" />
							) : status === "completed" ? (
								<FileText className="h-6 w-6" />
							) : (
								<Calendar className="h-6 w-6" />
							)}
						</div>
						<div>
							<div className="flex items-center gap-2">
								<Badge
									className={
										status === "scheduled"
											? "status-badge-upcoming"
											: status === "completed"
											? "status-badge-completed"
											: "status-badge-canceled"
									}
								>
									{status === "scheduled"
										? "Запланировано"
										: status === "completed"
										? "Завершено"
										: "Отменено"}
								</Badge>
							</div>
							<h3 className="mt-2 text-lg font-medium">
								{specialty} {doctor}
							</h3>
							<div className="mt-2 flex flex-col gap-1 text-sm text-slate-600">
								<div className="flex items-center gap-1">
									<Clock className="h-4 w-4" />
									<span>
										{date}, {time}
									</span>
								</div>
								<div className="flex items-center gap-1">
									<MapPin className="h-4 w-4" />
									<span>Кабинет {room}</span>
								</div>
								{serviceName ? (
									<div className="flex flex-wrap items-baseline gap-x-1 gap-y-0.5 text-slate-600">
										<span>Услуга:</span>
										<span className="font-medium text-foreground">
											{serviceName}
										</span>
										{servicePriceLabel &&
										servicePriceLabel !== "—" ? (
											<span className="font-semibold text-foreground">
												{servicePriceLabel}
											</span>
										) : (
											<span className="text-muted-foreground text-xs">
												(стоимость уточняйте в клинике)
											</span>
										)}
									</div>
								) : null}
							</div>
						</div>
					</div>
					<div className="flex flex-wrap gap-2">
						{status === "scheduled" ? (
							<AlertDialog>
								<AlertDialogTrigger asChild>
									<Button
										variant="outline"
										size="sm"
										className="border-slate-300 hover:bg-slate-100 hover-scale"
									>
										Отменить
									</Button>
								</AlertDialogTrigger>
								<AlertDialogContent>
									<AlertDialogHeader>
										<AlertDialogTitle>
											Отмена приёма
										</AlertDialogTitle>
										<AlertDialogDescription>
											Вы уверены, что хотите отменить
											приём к {doctor} ({specialty}) на{" "}
											{date} в {time}? Это действие нельзя
											будет отменить.
										</AlertDialogDescription>
									</AlertDialogHeader>
									<AlertDialogFooter>
										<AlertDialogCancel>
											Нет, оставить
										</AlertDialogCancel>
										<AlertDialogAction
											onClick={onCancel}
											className="bg-red-500 hover:bg-red-600"
										>
											Да, отменить
										</AlertDialogAction>
									</AlertDialogFooter>
								</AlertDialogContent>
							</AlertDialog>
						) : (
							<Button
								variant="outline"
								size="sm"
								className="border-slate-300 hover:bg-slate-100 hover-scale"
								onClick={onViewDetails}
							>
								Просмотреть детали
							</Button>
						)}
					</div>
				</div>
			</CardContent>
		</Card>
	);
}
