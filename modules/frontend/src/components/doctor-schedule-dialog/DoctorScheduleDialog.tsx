import type React from "react";

import { zodResolver } from "@hookform/resolvers/zod";
import {
	format,
	isAfter,
	isBefore,
	isEqual,
	parseISO,
	set,
	startOfDay,
} from "date-fns";
import { ru } from "date-fns/locale";
import { CalendarIcon, Clock, Loader2, Plus, Trash2 } from "lucide-react";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";

import {
	useCreateScheduleMutation,
	useDeleteScheduleMutation,
	useGetRoomsQuery,
	useGetSchedulesByDoctorQuery,
} from "@/store/api/apiSlice";

import { Room } from "@/api/types";
import { CreateRoomDialog } from "@/components/create-room-dialog";
import { scheduleFormSchema, type ScheduleFormData } from "./schedule-form-schema";
import { sortWorkingHoursSchedules } from "./sort-working-hours-schedules";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
	Dialog,
	DialogContent,
	DialogDescription,
	DialogFooter,
	DialogHeader,
	DialogTitle,
	DialogTrigger,
} from "@/components/ui/dialog";
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
import { Separator } from "@/components/ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

interface DoctorScheduleDialogProps {
	doctorId: string;
	doctorName: string;
	open?: boolean;
	onOpenChange?: (open: boolean) => void;
	trigger?: React.ReactNode;
	onSave?: (scheduleData: any) => void;
	onDelete?: (scheduleId: string) => void;
}

export function DoctorScheduleDialog({
	doctorId,
	doctorName,
	open: controlledOpen,
	onOpenChange,
	trigger,
}: DoctorScheduleDialogProps) {
	const [internalOpen, setInternalOpen] = useState(false);
	const open = controlledOpen ?? internalOpen;
	const setOpen = onOpenChange ?? setInternalOpen;
	const doctorIdNum = Number.parseInt(doctorId, 10);
	const {
		data: schedulesRaw = [],
		isLoading,
		refetch: refetchSchedules,
	} = useGetSchedulesByDoctorQuery(doctorIdNum, {
		skip: Number.isNaN(doctorIdNum),
	});
	const { data: rooms = [], isLoading: isLoadingRooms, refetch: refetchRooms } =
		useGetRoomsQuery();
	const [createSchedule] = useCreateScheduleMutation();
	const [deleteSchedule] = useDeleteScheduleMutation();

	const activeWorkingHours = useMemo(
		() => sortWorkingHoursSchedules(schedulesRaw),
		[schedulesRaw]
	);

	const [isSubmitting, setIsSubmitting] = useState(false);
	const [activeTab, setActiveTab] = useState("manage");
	const [isCalendarOpen, setIsCalendarOpen] = useState(false);
	const [deletingIds, setDeletingIds] = useState<number[]>([]);
	const [isCreateRoomDialogOpen, setIsCreateRoomDialogOpen] = useState(false);

	const form = useForm<ScheduleFormData>({
		resolver: zodResolver(scheduleFormSchema),
		defaultValues: {
			startTime: "09:00",
			endTime: "18:00",
			appointmentDuration: "30",
			roomId: undefined,
		},
	});

	const {
		watch,
		setValue,
		formState: { errors },
	} = form;

	// Расчет временных слотов на основе данных формы
	const calculateTimeSlots = () => {
		const startTime = watch("startTime");
		const endTime = watch("endTime");
		const appointmentDuration = watch("appointmentDuration");

		if (!startTime || !endTime || !appointmentDuration) {
			return [];
		}

		const slots = [];
		const [startHour, startMinute] = startTime.split(":").map(Number);
		const [endHour, endMinute] = endTime.split(":").map(Number);
		const duration = Number.parseInt(appointmentDuration);

		if (
			isNaN(startHour) ||
			isNaN(startMinute) ||
			isNaN(endHour) ||
			isNaN(endMinute) ||
			isNaN(duration)
		) {
			return [];
		}

		let currentHour = startHour;
		let currentMinute = startMinute;

		while (
			currentHour < endHour ||
			(currentHour === endHour && currentMinute < endMinute)
		) {
			const formattedHour = currentHour.toString().padStart(2, "0");
			const formattedMinute = currentMinute.toString().padStart(2, "0");
			slots.push(`${formattedHour}:${formattedMinute}`);

			currentMinute += duration;
			while (currentMinute >= 60) {
				currentHour += 1;
				currentMinute -= 60;
			}
		}

		return slots;
	};

	const timeSlots = calculateTimeSlots();

	// Функция для проверки, является ли дата прошедшей или текущей
	const isPastDate = (dateStr: string | null | undefined): boolean => {
		if (!dateStr) return false;
		try {
			const date = parseISO(dateStr);
			return (
				isBefore(startOfDay(date), startOfDay(new Date())) ||
				isEqual(startOfDay(date), startOfDay(new Date()))
			);
		} catch (error) {
			console.error("Error parsing date:", dateStr, error);
			return false;
		}
	};

	// Функция для получения всех дат, на которые уже есть расписание
	const getDisabledDates = () => {
		const disabledDates = new Set<string>();

		activeWorkingHours.forEach((schedule) => {
			// Для одиночной даты
			if (schedule.dateAt) {
				disabledDates.add(schedule.dateAt);
			}
		});

		return disabledDates;
	};

	// Функция для проверки, заблокирована ли дата
	const isDateDisabled = (date: Date) => {
		const disabledDates = getDisabledDates();
		return (
			date < startOfDay(new Date()) ||
			disabledDates.has(format(date, "yyyy-MM-dd"))
		);
	};

	const formatDateIfExists = (date: Date | undefined | null): string => {
		if (!date) return "Выберите дату";
		return format(date, "dd MMMM yyyy", { locale: ru });
	};

	const handleRoomCreated = (room: Room) => {
		void refetchRooms();
		setValue("roomId", room.id.toString());
	};

	const isToday = (date: Date | undefined) => {
		if (!date) return false;
		return isEqual(startOfDay(date), startOfDay(new Date()));
	};

	// Обновляем схему валидации с использованием date-fns
	const validateTimeForCurrentDay = (
		startTime: string,
		date: Date | undefined
	) => {
		if (!date || !isToday(date)) return true;

		const [hours, minutes] = startTime.split(":").map(Number);
		const scheduleTime = set(new Date(), {
			hours,
			minutes,
			seconds: 0,
			milliseconds: 0,
		});
		const currentTime = new Date();

		return isAfter(scheduleTime, currentTime);
	};

	const validateTimeRange = (startTime: string, endTime: string) => {
		const [startHours, startMinutes] = startTime.split(":").map(Number);
		const [endHours, endMinutes] = endTime.split(":").map(Number);

		const baseDate = new Date();
		const start = set(baseDate, {
			hours: startHours,
			minutes: startMinutes,
			seconds: 0,
			milliseconds: 0,
		});
		const end = set(baseDate, {
			hours: endHours,
			minutes: endMinutes,
			seconds: 0,
			milliseconds: 0,
		});

		return isAfter(end, start);
	};

	// Функция для обработки ошибок валидации от API
	const handleApiError = (error: any): string => {
		if (!error.response?.data) {
			return "Не удалось выполнить операцию. Пожалуйста, попробуйте позже.";
		}

		const errorData = error.response.data;

		// Если ошибка в виде массива строк
		if (Array.isArray(errorData)) {
			return errorData[0] || "Ошибка валидации";
		}

		// Если ошибка в виде объекта с полями
		if (typeof errorData === "object" && errorData !== null) {
			// Проверяем наличие поля errors
			if (errorData.errors && typeof errorData.errors === "object") {
				const firstError = Object.values(errorData.errors)[0];
				if (Array.isArray(firstError)) {
					return firstError[0] as string;
				}
				return firstError as string;
			}

			// Проверяем массив в корне объекта
			const firstValue = Object.values(errorData)[0];
			if (Array.isArray(firstValue)) {
				return firstValue[0] as string;
			}
			if (typeof firstValue === "string") {
				return firstValue;
			}

			// Проверяем поле message
			if (errorData.message) {
				return errorData.message;
			}
		}

		// Если ошибка в виде строки
		if (typeof errorData === "string") {
			return errorData;
		}

		return "Ошибка валидации. Проверьте введенные данные.";
	};

	const onSubmit = async (data: ScheduleFormData) => {
		setIsSubmitting(true);
		try {
			if (!data.singleDate) {
				toast.error("Ошибка валидации", {
					description: "Пожалуйста, выберите дату",
				});
				setIsSubmitting(false);
				return;
			}

			// Проверяем корректность времени
			if (!validateTimeRange(data.startTime, data.endTime)) {
				toast.error("Ошибка валидации", {
					description:
						"Время окончания должно быть позже времени начала",
				});
				setIsSubmitting(false);
				return;
			}

			// Проверяем время для текущего дня
			if (!validateTimeForCurrentDay(data.startTime, data.singleDate)) {
				toast.error("Ошибка валидации", {
					description:
						"Время начала не может быть меньше текущего времени",
				});
				setIsSubmitting(false);
				return;
			}

			// Создаем расписание
			const scheduleData = {
				doctor: {
					id: parseInt(doctorId),
				},
				room: data.roomId
					? {
							id: parseInt(data.roomId),
					  }
					: null,
				dateAt: format(data.singleDate, "yyyy-MM-dd"),
				startTime: data.startTime + ":00", // Добавляем секунды
				endTime: data.endTime + ":00", // Добавляем секунды
				slotDurationMinutes: parseInt(data.appointmentDuration),
			};

			await createSchedule(scheduleData).unwrap();
			void refetchSchedules();

			toast.success("Расписание создано", {
				description: "Расписание врача успешно сохранено",
			});

			form.reset();
			setActiveTab("manage");
		} catch (error) {
			console.error("Ошибка при создании расписания:", error);
			const errorMessage = handleApiError(error);
			toast.error("Ошибка при создании расписания", {
				description: errorMessage,
			});
		} finally {
			setIsSubmitting(false);
		}
	};

	const handleDelete = async (scheduleId: number) => {
		try {
			setDeletingIds((prev) => [...prev, scheduleId]);
			await deleteSchedule(scheduleId).unwrap();

			void refetchSchedules();

			toast.success("Расписание удалено", {
				description: "Расписание врача успешно удалено",
			});
		} catch (error: any) {
			const errorMessage = handleApiError(error);
			toast.error("Ошибка при удалении расписания", {
				description: errorMessage,
			});
		} finally {
			setDeletingIds((prev) => prev.filter((id) => id !== scheduleId));
		}
	};

	return (
		<>
			<Dialog open={open} onOpenChange={setOpen}>
				{trigger && <DialogTrigger asChild>{trigger}</DialogTrigger>}
				<DialogContent className="sm:max-w-[700px]">
					<DialogHeader>
						<DialogTitle>Управление расписанием врача</DialogTitle>
						<DialogDescription>
							Управляйте расписанием для врача {doctorName}
						</DialogDescription>
					</DialogHeader>

					<Tabs
						value={activeTab}
						onValueChange={setActiveTab}
						className="mt-4"
					>
						<TabsList className="grid w-full grid-cols-2">
							<TabsTrigger value="manage">
								Существующее расписание
							</TabsTrigger>
							<TabsTrigger value="create">
								Создать расписание
							</TabsTrigger>
						</TabsList>

						<TabsContent value="manage" className="space-y-4 py-4">
							{isLoading ? (
								<div className="flex items-center justify-center py-8">
									<Loader2 className="h-8 w-8 animate-spin text-slate-400" />
								</div>
							) : activeWorkingHours.length > 0 ? (
								<div className="space-y-2 max-h-[400px] overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-gray-100 hover:scrollbar-thumb-gray-400">
									{activeWorkingHours
										.filter((schedule) => schedule.dateAt)
										.map((schedule) => {
											const isPast = isPastDate(
												schedule.dateAt
											);
											return (
												<div
													key={schedule.id}
													className={cn(
														"flex items-center justify-between rounded-lg border p-3 transition-all",
														isPast
															? "border-slate-200 bg-slate-50/50"
															: "border-slate-200 bg-slate-50 hover:border-blue-200 hover:bg-blue-50/50"
													)}
												>
													<div className="flex-1">
														<div className="flex items-center gap-2">
															<CalendarIcon
																className={cn(
																	"h-4 w-4",
																	isPast
																		? "text-slate-400"
																		: "text-slate-500"
																)}
															/>
															<span
																className={cn(
																	"font-medium",
																	isPast &&
																		"text-slate-500"
																)}
															>
																{schedule.dateAt && format(
																	parseISO(schedule.dateAt),
																	"EEEE, dd MMMM",
																	{
																		locale: ru,
																	}
																)}
														</span>
													</div>
													<div
														className={cn(
															"mt-1 flex items-center gap-4 text-sm",
															isPast
																? "text-slate-400"
																: "text-slate-600"
														)}
													>
														<span className="flex items-center gap-1">
															<Clock className="h-3.5 w-3.5" />
															{schedule.startTime.substring(0, 5)}{" "}
															-{" "}
															{schedule.endTime.substring(0, 5)}
														</span>
														<span>
															Длительность приема:{" "}
															{schedule.slotDurationMinutes}{" "}
															мин.
														</span>
													</div>
													<div className="mt-2 flex flex-wrap gap-1">
														{isPast && (
															<Badge className="bg-slate-100 text-slate-500 border-slate-200">
																Прошедший день
															</Badge>
														)}
													</div>
												</div>
												<div className="ml-4 flex items-center">
													<Button
														variant="ghost"
														size="icon"
														className={cn(
															"h-8 w-8",
															isPast
																? "text-slate-400 hover:bg-slate-50 hover:text-slate-500 cursor-not-allowed"
																: "text-red-500 hover:bg-red-50 hover:text-red-600"
														)}
														onClick={() =>
															handleDelete(
																schedule.id
															)
														}
														disabled={
															deletingIds.includes(
																schedule.id
															) || isPast
														}
													>
														{deletingIds.includes(
															schedule.id
														) ? (
															<Loader2 className="h-4 w-4 animate-spin" />
														) : (
															<Trash2 className="h-4 w-4" />
														)}
													</Button>
												</div>
											</div>
										);
									})}
								</div>
							) : (
								<div className="flex flex-col items-center justify-center py-8 text-center">
									<CalendarIcon className="mb-2 h-12 w-12 text-slate-300" />
									<h3 className="text-lg font-medium">
										Нет расписания
									</h3>
									<p className="text-sm text-slate-500">
										У этого врача еще не создано расписание.
										Создайте его на вкладке "Создать
										расписание".
									</p>
									<Button
										className="mt-4 gradient-button"
										onClick={() => setActiveTab("create")}
									>
										Создать расписание
									</Button>
								</div>
							)}
						</TabsContent>

						<TabsContent value="create">
							<form onSubmit={form.handleSubmit(onSubmit)}>
								<div className="grid gap-4 py-4">
									<div className="space-y-2 flex flex-col">
										<Label>Выберите дату</Label>
										<Popover
											modal={true}
											open={isCalendarOpen}
											onOpenChange={setIsCalendarOpen}
										>
											<PopoverTrigger asChild>
												<Button
													type="button"
													variant={"outline"}
													className={cn(
														"w-full justify-start text-left font-normal",
														!watch(
															"singleDate"
														) &&
															"text-muted-foreground"
													)}
												>
													<CalendarIcon className="mr-2 h-4 w-4" />
													{formatDateIfExists(
														watch("singleDate")
													)}
												</Button>
											</PopoverTrigger>
											<PopoverContent
												className="w-auto p-0"
												align="start"
											>
												<Calendar
													mode="single"
													selected={watch(
														"singleDate"
													)}
													onSelect={(date) => {
														if (date)
															setValue(
																"singleDate",
																date
															);
														setIsCalendarOpen(
															false
														);
													}}
													disabled={
														isDateDisabled
													}
													locale={ru}
												/>
											</PopoverContent>
										</Popover>
										{errors.singleDate && (
											<p className="text-sm text-red-500">
												{errors.singleDate.message}
											</p>
										)}
									</div>

									<div className="space-y-2">
										<div className="flex items-center justify-between">
											<Label htmlFor="roomId">Кабинет</Label>
											<CreateRoomDialog
												open={isCreateRoomDialogOpen}
												onOpenChange={setIsCreateRoomDialogOpen}
												onRoomCreated={handleRoomCreated}
												trigger={
													<Button
														type="button"
														variant="ghost"
														size="sm"
														className="h-8 gap-1 text-xs"
													>
														<Plus className="h-3 w-3" />
														Добавить
													</Button>
												}
											/>
										</div>
										<Select
											value={watch("roomId") || ""}
											onValueChange={(value) =>
												setValue("roomId", value || undefined)
											}
											disabled={isLoadingRooms}
										>
											<SelectTrigger className="w-full">
												<SelectValue placeholder="Выберите кабинет (необязательно)" />
											</SelectTrigger>
											<SelectContent>
												{rooms.map((room) => (
													<SelectItem
														key={room.id}
														value={room.id.toString()}
													>
														{room.code}
														{room.name && ` - ${room.name}`}
													</SelectItem>
												))}
												{rooms.length === 0 && !isLoadingRooms && (
													<SelectItem value="no-rooms" disabled>
														Нет доступных кабинетов
													</SelectItem>
												)}
											</SelectContent>
										</Select>
									</div>

									<Separator />

									<div className="grid grid-cols-2 gap-4">
										<div className="space-y-2">
											<Label htmlFor="startTime">
												Время начала работы
											</Label>
											<input
												type="time"
												id="startTime"
												{...form.register("startTime")}
												className="w-full rounded-md border border-slate-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-slate-400"
											/>
											{errors.startTime && (
												<p className="text-sm text-red-500">
													{errors.startTime.message}
												</p>
											)}
										</div>
										<div className="space-y-2">
											<Label htmlFor="endTime">
												Время окончания работы
											</Label>
											<input
												type="time"
												id="endTime"
												{...form.register("endTime")}
												className="w-full rounded-md border border-slate-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-slate-400"
											/>
											{errors.endTime && (
												<p className="text-sm text-red-500">
													{errors.endTime.message}
												</p>
											)}
										</div>
									</div>

									<div className="space-y-2">
										<Label htmlFor="appointmentDuration">
											Длительность одного приема (минут)
										</Label>
										<input
											type="number"
											id="appointmentDuration"
											{...form.register(
												"appointmentDuration"
											)}
											min="5"
											max="120"
											step="5"
											className="w-full rounded-md border border-slate-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-slate-400"
										/>
										{errors.appointmentDuration && (
											<p className="text-sm text-red-500">
												{
													errors.appointmentDuration
														.message
												}
											</p>
										)}
									</div>

									<Separator />

									<div className="space-y-2">
										<div className="flex items-center justify-between">
											<Label>
												Предварительный просмотр слотов
											</Label>
											<span className="text-sm text-slate-500">
												Всего слотов: {timeSlots.length}
											</span>
										</div>
										<div className="max-h-[150px] overflow-y-auto rounded-md border border-slate-200 p-2">
											<div className="flex flex-wrap gap-2">
												{timeSlots.map((slot) => (
													<div
														key={slot}
														className="flex items-center rounded-md border border-slate-200 bg-slate-50 px-2 py-1 text-sm"
													>
														<Clock className="mr-1 h-3 w-3 text-slate-500" />
														{slot}
													</div>
												))}
											</div>
										</div>
									</div>
								</div>
								<DialogFooter>
									<Button
										type="button"
										variant="outline"
										onClick={() => setActiveTab("manage")}
									>
										Отмена
									</Button>
									<Button
										type="submit"
										className="gradient-button"
										disabled={isSubmitting}
									>
										{isSubmitting ? (
											<span className="flex items-center gap-2">
												<span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
												Сохранение...
											</span>
										) : (
											"Создать расписание"
										)}
									</Button>
								</DialogFooter>
							</form>
						</TabsContent>
					</Tabs>
				</DialogContent>
			</Dialog>
		</>
	);
}
