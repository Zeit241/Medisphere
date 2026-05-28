"use client";

import type { Appointment, Diagnosis } from "@/api/types";
import { Button } from "@/components/ui/button";
import {
	Card,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/contexts/AuthContext";
import { format } from "date-fns";
import { Loader2 } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

import {
	useCompleteAppointmentMutation,
	useGetDiagnosesQuery,
	useGetLiveQueueByDoctorQuery,
	useGetMyDoctorAppointmentsQuery,
} from "@/store/api/apiSlice";

import { CompleteAppointmentDialog } from "./complete-appointment-dialog";
import { DoctorAppointmentsTableCard } from "./doctor-appointments-table-card";
import { DoctorLiveQueueCard } from "./doctor-live-queue-card";

export default function DoctorAppointmentsPage() {
	const navigate = useNavigate();
	const { user, isLoading: authLoading } = useAuth();
	const [doctorId, setDoctorId] = useState<number | null>(null);
	const [dateStr, setDateStr] = useState(() =>
		format(new Date(), "yyyy-MM-dd")
	);
	const {
		data: rawAppointments = [],
		isLoading: appointmentsLoading,
		refetch: refetchAppointments,
	} = useGetMyDoctorAppointmentsQuery({ date: dateStr }, { skip: !doctorId });
	const appointments = useMemo(
		() => rawAppointments.filter((x) => x.patientId != null),
		[rawAppointments]
	);
	const {
		data: liveQueue = [],
		isFetching: queueLoading,
		refetch: refetchQueue,
	} = useGetLiveQueueByDoctorQuery(
		{ doctorId: doctorId!, date: dateStr },
		{ skip: !doctorId }
	);
	const { data: diagnoses = [] } = useGetDiagnosesQuery();
	const [completeAppointmentMut] = useCompleteAppointmentMutation();
	const loading = appointmentsLoading;
	const [completeOpen, setCompleteOpen] = useState(false);
	const [selectedAppointment, setSelectedAppointment] =
		useState<Appointment | null>(null);
	const [selectedDiagnosis, setSelectedDiagnosis] = useState<Diagnosis | null>(
		null
	);
	const [comboOpen, setComboOpen] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [completeDialogEl, setCompleteDialogEl] = useState<HTMLDivElement | null>(
		null
	);

	useEffect(() => {
		if (authLoading) return;
		if (import.meta.env.DEV) {
			console.debug("[doctor-cabinet]", {
				userId: user?.id,
				email: user?.email,
				role: user?.role,
				doctorId: user?.doctorId,
				hasDoctorId: user != null && user.doctorId != null && user.doctorId !== undefined,
			});
		}
		if (!user?.doctorId) {
			if (user) {
				toast.error("У учётной записи нет профиля врача");
			}
			setDoctorId(null);
			return;
		}
		setDoctorId(user.doctorId);
	}, [user, authLoading]);

	const openComplete = (a: Appointment) => {
		setSelectedAppointment(a);
		const d = a.diagnosis;
		if (typeof d === "object" && d !== null && "id" in d) {
			const found = diagnoses.find((x) => x.id === d.id);
			setSelectedDiagnosis(found ?? null);
		} else {
			setSelectedDiagnosis(null);
		}
		setCompleteOpen(true);
	};

	const handleComplete = async () => {
		if (!selectedAppointment || !selectedDiagnosis) {
			toast.error("Выберите диагноз из справочника МКБ");
			return;
		}
		try {
			setSubmitting(true);
			const res = await completeAppointmentMut({
				id: selectedAppointment.id,
				body: { diagnosis: selectedDiagnosis.code },
			}).unwrap();
			toast.success(res.message || "Приём завершён");
			setCompleteOpen(false);
			setSelectedAppointment(null);
			setSelectedDiagnosis(null);
			await refetchAppointments();
			await refetchQueue();
		} catch (e) {
			console.error(e);
			toast.error("Не удалось завершить приём");
		} finally {
			setSubmitting(false);
		}
	};

	if (authLoading) {
		return (
			<main className="flex-1 py-8">
				<div className="container mx-auto flex justify-center py-24">
					<Loader2 className="h-10 w-10 animate-spin text-slate-400" />
				</div>
			</main>
		);
	}

	if (!loading && doctorId === null) {
		return (
			<main className="flex-1 py-8">
				<div className="container mx-auto px-4">
					<Card>
						<CardHeader>
							<CardTitle>Нет доступа</CardTitle>
							<CardDescription>
								Войдите под учётной записью врача, привязанной к карточке
								специалиста.
							</CardDescription>
						</CardHeader>
					</Card>
				</div>
			</main>
		);
	}

	return (
		<main className="flex-1 py-8">
			<div className="container mx-auto px-4 space-y-8">
				<div>
					<h1 className="text-3xl font-bold gradient-heading">
						Приёмы врача
					</h1>
					<p className="mt-2 text-slate-600">
						Завершение приёма с диагнозом сдвигает живую очередь (Redis).
					</p>
				</div>

				<div className="flex flex-wrap items-end gap-4">
					<div className="space-y-2">
						<Label htmlFor="doctor-date">Дата</Label>
						<Input
							id="doctor-date"
							type="date"
							value={dateStr}
							onChange={(e) => setDateStr(e.target.value)}
						/>
					</div>
					<Button
						variant="secondary"
						onClick={() => {
							void refetchAppointments();
							void refetchQueue();
						}}
						disabled={loading || !doctorId}
					>
						Обновить
					</Button>
				</div>

				<div className="grid gap-6 lg:grid-cols-3">
					<DoctorAppointmentsTableCard
						loading={loading}
						appointments={appointments}
						onRowNavigate={(id) => navigate(`/doctor/appointments/${id}`)}
						onOpenComplete={openComplete}
					/>
					<DoctorLiveQueueCard queueLoading={queueLoading} liveQueue={liveQueue} />
				</div>
			</div>

			<CompleteAppointmentDialog
				open={completeOpen}
				onOpenChange={setCompleteOpen}
				completeDialogEl={completeDialogEl}
				dialogContentRef={setCompleteDialogEl}
				selectedAppointment={selectedAppointment}
				selectedDiagnosis={selectedDiagnosis}
				onSelectDiagnosis={setSelectedDiagnosis}
				comboOpen={comboOpen}
				onComboOpenChange={setComboOpen}
				diagnoses={diagnoses}
				submitting={submitting}
				onCancel={() => setCompleteOpen(false)}
				onConfirm={handleComplete}
			/>
		</main>
	);
}
