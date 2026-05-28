"use client";

import type { Appointment, Diagnosis, UpdateAppointmentRequest } from "@/api/types";
import { Button } from "@/components/ui/button";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import {
	Command,
	CommandEmpty,
	CommandGroup,
	CommandInput,
	CommandItem,
	CommandList,
} from "@/components/ui/command";
import { Label } from "@/components/ui/label";
import {
	Popover,
	PopoverContent,
	PopoverTrigger,
} from "@/components/ui/popover";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import { format } from "date-fns";
import { ArrowLeft, Check, ChevronsUpDown, Loader2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";

import {
	useGetAppointmentByIdQuery,
	useGetDiagnosesQuery,
	useUpdateAppointmentMutation,
} from "@/store/api/apiSlice";

const statusLabels: Record<string, string> = {
	scheduled: "Запланирован",
	confirmed: "Подтверждён",
	in_progress: "В процессе",
	completed: "Завершён",
	cancelled: "Отменён",
	no_show: "Неявка",
	available: "Слот",
};

const statusValues = [
	"scheduled",
	"confirmed",
	"in_progress",
	"completed",
	"cancelled",
	"no_show",
] as const;

function patientDisplayName(a: Appointment): string {
	const p = a.patient as
		| {
				user?: { firstName?: string; lastName?: string; middleName?: string | null };
				firstName?: string;
				lastName?: string;
				middleName?: string | null;
		  }
		| null
		| undefined;
	if (!p) return a.patientId != null ? `Пациент #${a.patientId}` : "—";
	if (p.user) {
		return [p.user.lastName, p.user.firstName, p.user.middleName].filter(Boolean).join(" ");
	}
	return [p.lastName, p.firstName, p.middleName].filter(Boolean).join(" ");
}

function diagnosisFromAppointment(a: Appointment): Diagnosis | null {
	const d = a.diagnosis;
	if (d == null || typeof d === "string") return null;
	if (typeof d === "object" && "id" in d && "code" in d) {
		return {
			id: d.id,
			code: d.code,
			name: d.name,
			category: d.category ?? null,
		};
	}
	return null;
}

export default function DoctorAppointmentDetailPage() {
	const { id } = useParams<{ id: string }>();
	const navigate = useNavigate();
	const appointmentId = id ? Number.parseInt(id, 10) : NaN;

	const [saving, setSaving] = useState(false);
	const [appointment, setAppointment] = useState<Appointment | null>(null);
	const finiteId = Number.isFinite(appointmentId);
	const {
		data: apptFromQuery,
		isLoading: loading,
		refetch,
	} = useGetAppointmentByIdQuery(appointmentId, { skip: !finiteId });
	const { data: diagnoses = [] } = useGetDiagnosesQuery();
	const [updateAppointmentMut] = useUpdateAppointmentMutation();

	const [status, setStatus] = useState<string>("scheduled");
	const [complaints, setComplaints] = useState("");
	const [anamnesis, setAnamnesis] = useState("");
	const [recommendations, setRecommendations] = useState("");
	const [cancelReason, setCancelReason] = useState("");
	const [selectedDiagnosis, setSelectedDiagnosis] = useState<Diagnosis | null>(null);
	const [comboOpen, setComboOpen] = useState(false);

	useEffect(() => {
		if (!finiteId || loading) return;
		if (!apptFromQuery) {
			setAppointment(null);
			return;
		}
		const a = apptFromQuery;
		setAppointment(a);
		setStatus(a.status);
		setComplaints(a.complaints ?? "");
		setAnamnesis(a.anamnesis ?? "");
		setRecommendations(a.recommendations ?? "");
		setCancelReason(a.cancelReason ?? "");
		const d0 = diagnosisFromAppointment(a);
		if (d0) {
			setSelectedDiagnosis(d0);
		} else if (a.diagnosisId != null) {
			const found = diagnoses.find((x) => x.id === a.diagnosisId);
			setSelectedDiagnosis(found ?? null);
		} else {
			setSelectedDiagnosis(null);
		}
	}, [apptFromQuery, diagnoses, finiteId, loading]);

	const handleSave = async () => {
		if (!Number.isFinite(appointmentId) || !appointment) return;
		try {
			setSaving(true);
			const updated = await updateAppointmentMut({
				id: appointmentId,
				body: {
					status: status as UpdateAppointmentRequest["status"],
					complaints: complaints.trim() || null,
					anamnesis: anamnesis.trim() || null,
					recommendations: recommendations.trim() || null,
					cancelReason: cancelReason.trim() || null,
					diagnosisId: selectedDiagnosis?.id ?? null,
				},
			}).unwrap();
			setAppointment(updated);
			void refetch();
			toast.success("Сохранено");
		} catch (e) {
			console.error(e);
			toast.error("Не удалось сохранить");
		} finally {
			setSaving(false);
		}
	};

	if (!Number.isFinite(appointmentId)) {
		return (
			<main className="flex-1 py-8">
				<div className="container mx-auto px-4">
					<p className="text-slate-600">Некорректный идентификатор</p>
					<Button asChild variant="link" className="mt-2 px-0">
						<Link to="/doctor/appointments">К списку</Link>
					</Button>
				</div>
			</main>
		);
	}

	if (loading) {
		return (
			<main className="flex-1 py-8">
				<div className="container mx-auto flex justify-center py-24">
					<Loader2 className="h-10 w-10 animate-spin text-slate-400" />
				</div>
			</main>
		);
	}

	if (!appointment) {
		return (
			<main className="flex-1 py-8">
				<div className="container mx-auto px-4">
					<p className="text-slate-600">Приём не найден или нет доступа</p>
					<Button asChild variant="link" className="mt-2 px-0">
						<Link to="/doctor/appointments">К списку</Link>
					</Button>
				</div>
			</main>
		);
	}

	return (
		<main className="flex-1 py-8">
			<div className="container mx-auto max-w-3xl px-4 space-y-6">
				<div className="flex flex-wrap items-center gap-3">
					<Button
						type="button"
						variant="ghost"
						size="sm"
						className="gap-1 pl-0"
						onClick={() => navigate("/doctor/appointments")}
					>
						<ArrowLeft className="h-4 w-4" />
						К списку
					</Button>
				</div>

				<div>
					<h1 className="text-2xl font-bold gradient-heading">Карточка приёма</h1>
					<p className="mt-1 text-sm text-slate-600">
						{format(new Date(appointment.startTime), "dd.MM.yyyy HH:mm")}
						{" — "}
						{format(new Date(appointment.endTime), "HH:mm")}
					</p>
				</div>

				<Card>
					<CardHeader>
						<CardTitle>Пациент и слот</CardTitle>
						<CardDescription>Только для просмотра</CardDescription>
					</CardHeader>
					<CardContent className="space-y-2 text-sm">
						<p>
							<span className="text-slate-500">Пациент: </span>
							{patientDisplayName(appointment)}
						</p>
						{appointment.service?.name && (
							<p>
								<span className="text-slate-500">Услуга: </span>
								{appointment.service.name}
							</p>
						)}
						{appointment.room?.code && (
							<p>
								<span className="text-slate-500">Кабинет: </span>
								{appointment.room.code}
							</p>
						)}
					</CardContent>
				</Card>

				<Card>
					<CardHeader>
						<CardTitle>Статус и диагноз</CardTitle>
					</CardHeader>
					<CardContent className="space-y-4">
						<div className="space-y-2">
							<Label>Статус</Label>
							<Select value={status} onValueChange={setStatus}>
								<SelectTrigger>
									<SelectValue />
								</SelectTrigger>
								<SelectContent>
									{statusValues.map((s) => (
										<SelectItem key={s} value={s}>
											{statusLabels[s] ?? s}
										</SelectItem>
									))}
								</SelectContent>
							</Select>
						</div>

						<div className="space-y-2">
							<Label>Диагноз (МКБ-10)</Label>
							<Popover open={comboOpen} onOpenChange={setComboOpen}>
								<PopoverTrigger asChild>
									<Button
										variant="outline"
										role="combobox"
										aria-expanded={comboOpen}
										className="w-full justify-between font-normal"
									>
										{selectedDiagnosis
											? `${selectedDiagnosis.code} — ${selectedDiagnosis.name}`
											: "Выберите или найдите…"}
										<ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
									</Button>
								</PopoverTrigger>
								<PopoverContent className="w-[var(--radix-popover-trigger-width)] p-0" align="start">
									<Command>
										<CommandInput placeholder="Поиск…" />
										<CommandList>
											<CommandEmpty>Не найдено</CommandEmpty>
											<CommandGroup>
												<CommandItem
													value="__clear__"
													onSelect={() => {
														setSelectedDiagnosis(null);
														setComboOpen(false);
													}}
												>
													<span className="text-slate-500">Без диагноза</span>
												</CommandItem>
												{diagnoses.map((d) => (
													<CommandItem
														key={d.id}
														value={`${d.code} ${d.name}`}
														onSelect={() => {
															setSelectedDiagnosis(d);
															setComboOpen(false);
														}}
													>
														<Check
															className={cn(
																"mr-2 h-4 w-4",
																selectedDiagnosis?.id === d.id
																	? "opacity-100"
																	: "opacity-0"
															)}
														/>
														{d.code} — {d.name}
													</CommandItem>
												))}
											</CommandGroup>
										</CommandList>
									</Command>
								</PopoverContent>
							</Popover>
						</div>

						<div className="space-y-2">
							<Label htmlFor="cancel-reason">Причина отмены (если отменён)</Label>
							<Textarea
								id="cancel-reason"
								value={cancelReason}
								onChange={(e) => setCancelReason(e.target.value)}
								rows={2}
								placeholder="Необязательно"
							/>
						</div>
					</CardContent>
				</Card>

				<Card>
					<CardHeader>
						<CardTitle>Медицинская информация</CardTitle>
						<CardDescription>Жалобы, анамнез, назначения</CardDescription>
					</CardHeader>
					<CardContent className="space-y-4">
						<div className="space-y-2">
							<Label htmlFor="complaints">Жалобы (со слов пациента)</Label>
							<Textarea
								id="complaints"
								value={complaints}
								onChange={(e) => setComplaints(e.target.value)}
								rows={4}
								placeholder="Текст жалоб…"
							/>
						</div>
						<div className="space-y-2">
							<Label htmlFor="anamnesis">Анамнез</Label>
							<Textarea
								id="anamnesis"
								value={anamnesis}
								onChange={(e) => setAnamnesis(e.target.value)}
								rows={4}
								placeholder="Заболевание, жизнь, перенесённые заболевания…"
							/>
						</div>
						<div className="space-y-2">
							<Label htmlFor="recommendations">Назначения и план лечения</Label>
							<Textarea
								id="recommendations"
								value={recommendations}
								onChange={(e) => setRecommendations(e.target.value)}
								rows={4}
								placeholder="Рекомендации, препараты, контроль…"
							/>
						</div>
					</CardContent>
				</Card>

				<div className="flex flex-wrap gap-3 pb-8">
					<Button type="button" onClick={handleSave} disabled={saving}>
						{saving ? (
							<>
								<Loader2 className="mr-2 h-4 w-4 animate-spin" />
								Сохранение…
							</>
						) : (
							"Сохранить"
						)}
					</Button>
					<Button type="button" variant="outline" onClick={() => navigate("/doctor/appointments")}>
						Отмена
					</Button>
				</div>
			</div>
		</main>
	);
}
