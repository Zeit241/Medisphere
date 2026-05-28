import type { Appointment, Diagnosis } from "@/api/types";
import { Button } from "@/components/ui/button";
import {
	Command,
	CommandEmpty,
	CommandGroup,
	CommandInput,
	CommandItem,
	CommandList,
} from "@/components/ui/command";
import {
	Dialog,
	DialogContent,
	DialogDescription,
	DialogFooter,
	DialogHeader,
	DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import {
	Popover,
	PopoverContent,
	PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import { format } from "date-fns";
import { Check, ChevronsUpDown, Loader2 } from "lucide-react";
import { patientShortName } from "./doctor-appointments-utils";

interface CompleteAppointmentDialogProps {
	open: boolean;
	onOpenChange: (open: boolean) => void;
	completeDialogEl: HTMLDivElement | null;
	dialogContentRef: (el: HTMLDivElement | null) => void;
	selectedAppointment: Appointment | null;
	selectedDiagnosis: Diagnosis | null;
	onSelectDiagnosis: (d: Diagnosis) => void;
	comboOpen: boolean;
	onComboOpenChange: (open: boolean) => void;
	diagnoses: Diagnosis[];
	submitting: boolean;
	onCancel: () => void;
	onConfirm: () => void;
}

export function CompleteAppointmentDialog({
	open,
	onOpenChange,
	completeDialogEl,
	dialogContentRef,
	selectedAppointment,
	selectedDiagnosis,
	onSelectDiagnosis,
	comboOpen,
	onComboOpenChange,
	diagnoses,
	submitting,
	onCancel,
	onConfirm,
}: CompleteAppointmentDialogProps) {
	return (
		<Dialog open={open} onOpenChange={onOpenChange}>
			<DialogContent ref={dialogContentRef} className="sm:max-w-lg">
				<DialogHeader>
					<DialogTitle>Завершить приём</DialogTitle>
					<DialogDescription>
						Выберите диагноз по МКБ-10. Статус будет установлен «Завершён», пациент
						удалится из очереди.
					</DialogDescription>
				</DialogHeader>
				{selectedAppointment && (
					<div className="space-y-2 text-sm text-slate-600">
						<p>
							<strong>Время:</strong>{" "}
							{format(new Date(selectedAppointment.startTime), "dd.MM HH:mm")}
						</p>
						<p>
							<strong>Пациент:</strong> {patientShortName(selectedAppointment)}
						</p>
					</div>
				)}
				<div className="space-y-2">
					<Label>Диагноз</Label>
					<Popover open={comboOpen} onOpenChange={onComboOpenChange}>
						<PopoverTrigger asChild>
							<Button
								variant="outline"
								role="combobox"
								aria-expanded={comboOpen}
								className="w-full justify-between font-normal"
							>
								{selectedDiagnosis
									? `${selectedDiagnosis.code} — ${selectedDiagnosis.name}`
									: "Найти по коду или названию…"}
								<ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
							</Button>
						</PopoverTrigger>
						<PopoverContent
							container={completeDialogEl}
							className="w-[var(--radix-popover-trigger-width)] p-0"
						>
							<Command>
								<CommandInput placeholder="Поиск…" />
								<CommandList>
									<CommandEmpty>Не найдено</CommandEmpty>
									<CommandGroup>
										{diagnoses.map((d) => (
											<CommandItem
												key={d.id}
												value={`${d.code} ${d.name}`}
												onSelect={() => {
													onSelectDiagnosis(d);
													onComboOpenChange(false);
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
												<span className="truncate">
													{d.code} — {d.name}
												</span>
											</CommandItem>
										))}
									</CommandGroup>
								</CommandList>
							</Command>
						</PopoverContent>
					</Popover>
				</div>
				<DialogFooter>
					<Button variant="outline" onClick={onCancel} disabled={submitting}>
						Отмена
					</Button>
					<Button onClick={onConfirm} disabled={submitting}>
						{submitting ? (
							<Loader2 className="h-4 w-4 animate-spin" />
						) : (
							"Завершить приём"
						)}
					</Button>
				</DialogFooter>
			</DialogContent>
		</Dialog>
	);
}
