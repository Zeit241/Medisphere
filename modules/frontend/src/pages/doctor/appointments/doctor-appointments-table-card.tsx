import type { Appointment } from "@/api/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import { format } from "date-fns";
import { Loader2 } from "lucide-react";
import {
	formatDiagnosisLabel,
	patientShortName,
	statusLabels,
	terminalStatuses,
} from "./doctor-appointments-utils";

interface DoctorAppointmentsTableCardProps {
	loading: boolean;
	appointments: Appointment[];
	onRowNavigate: (appointmentId: number) => void;
	onOpenComplete: (a: Appointment) => void;
}

export function DoctorAppointmentsTableCard({
	loading,
	appointments,
	onRowNavigate,
	onOpenComplete,
}: DoctorAppointmentsTableCardProps) {
	return (
		<Card className="lg:col-span-2">
			<CardHeader>
				<CardTitle>Записи на выбранный день</CardTitle>
				<CardDescription>Только слоты с назначенным пациентом</CardDescription>
			</CardHeader>
			<CardContent>
				{loading ? (
					<div className="flex justify-center py-12">
						<Loader2 className="h-8 w-8 animate-spin text-slate-400" />
					</div>
				) : appointments.length === 0 ? (
					<p className="text-slate-500">Нет приёмов на эту дату</p>
				) : (
					<div className="rounded-md border">
						<Table>
							<TableHeader>
								<TableRow>
									<TableHead>Время</TableHead>
									<TableHead>Пациент</TableHead>
									<TableHead>Статус</TableHead>
									<TableHead>Диагноз</TableHead>
									<TableHead className="w-[140px]" />
								</TableRow>
							</TableHeader>
							<TableBody>
								{appointments.map((a) => (
									<TableRow
										key={a.id}
										className="cursor-pointer hover:bg-slate-50/80"
										onClick={() => onRowNavigate(a.id)}
									>
										<TableCell className="whitespace-nowrap">
											{format(new Date(a.startTime), "HH:mm")}
											{" — "}
											{format(new Date(a.endTime), "HH:mm")}
										</TableCell>
										<TableCell>{patientShortName(a)}</TableCell>
										<TableCell>
											<Badge variant="secondary">
												{statusLabels[a.status] ?? a.status}
											</Badge>
										</TableCell>
										<TableCell className="max-w-[220px] truncate text-sm">
											{formatDiagnosisLabel(a)}
										</TableCell>
										<TableCell>
											{!terminalStatuses.has(a.status) ? (
												<Button
													size="sm"
													onClick={(e) => {
														e.stopPropagation();
														onOpenComplete(a);
													}}
												>
													Завершить
												</Button>
											) : (
												<span className="text-xs text-slate-400">—</span>
											)}
										</TableCell>
									</TableRow>
								))}
							</TableBody>
						</Table>
					</div>
				)}
			</CardContent>
		</Card>
	);
}
