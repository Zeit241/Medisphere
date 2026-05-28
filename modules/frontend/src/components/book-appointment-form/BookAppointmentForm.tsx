import { zodResolver } from "@hookform/resolvers/zod";
import { format } from "date-fns";
import { ru } from "date-fns/locale";
import { Calendar as CalendarIcon, Check, Loader2, Star, User } from "lucide-react";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";

import { useBookAppointmentMutation } from "@/store/api/apiSlice";
import type { ClinicService, Patient } from "@/api/types";
import { bookingFormSchema, type BookingFormValues } from "./booking-form-schema";
import { useBookingFormData } from "./use-booking-form-data";
import { DoctorReviewsDialog } from "@/components/doctor-reviews-dialog";
import { ServiceBookingPicker } from "@/components/service-booking-picker";
import { Calendar } from "@/components/ui/calendar";
import { useAuth } from "@/contexts/AuthContext";
import { doctorPhotoImgSrc } from "@/lib/doctorPhotoSrc";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import {
	Form,
	FormControl,
	FormField,
	FormItem,
	FormLabel,
	FormMessage,
} from "@/components/ui/form";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { formatClinicServicePriceFromFields } from "@/lib/format-clinic-service-price";

interface BookAppointmentFormProps {
	patient: Patient;
	onClose?: () => void;
}

export function BookAppointmentForm({
	patient,
	onClose,
}: BookAppointmentFormProps) {
	const [bookAppointment] = useBookAppointmentMutation();
	const { user } = useAuth();
	const form = useForm<BookingFormValues>({
		resolver: zodResolver(bookingFormSchema),
		defaultValues: {
			step: 1,
			searchMode: "by_specialty",
			specialty: "",
			service: "",
			doctor: "",
			date: "",
			time: "",
			notificationType: "email",
		},
	});

	const [isSubmitting, setIsSubmitting] = useState(false);
	const {
		specialties,
		allServicesCatalog,
		doctorServices,
		doctors,
		slots,
		isLoadingSpecialties,
		isLoadingDoctors,
		isLoadingSlots,
		selectedDoctorForReviews,
		setSelectedDoctorForReviews,
		isReviewsDialogOpen,
		setIsReviewsDialogOpen,
		availableDates,
		isLoadingDates,
		datePickerOpen,
		setDatePickerOpen,
		availableDateSet,
		datePickerContainerRef,
		step,
		searchMode,
		selectedSpecialty,
		selectedService,
		selectedDoctor,
		selectedDate,
		setSlots,
		setDoctors,
		setAvailableDates,
		handleNextStep,
		handlePrevStep,
		slotServiceLabel,
	} = useBookingFormData(form);

	const serviceField = form.watch("service");
	const selectedServiceForSummary = useMemo((): ClinicService | null => {
		const sid = serviceField?.trim();
		if (!sid) return null;
		return (
			doctorServices.find((s) => String(s.id) === sid) ??
			allServicesCatalog.find((s) => String(s.id) === sid) ??
			null
		);
	}, [serviceField, doctorServices, allServicesCatalog]);

	const handleSubmit = async (data: BookingFormValues) => {
		if (!user?.id || !data.slot_id) {
			toast.error("Ошибка", {
				description: "Не удалось определить пользователя или слот",
			});
			return;
		}

		setIsSubmitting(true);
		try {
			// Находим выбранный слот
			const selectedSlot = slots.find((slot) => slot.id === data.slot_id);
			if (!selectedSlot || selectedSlot.isBooked) {
				toast.error("Ошибка", {
					description: "Выбранный слот недоступен",
				});
				return;
			}

			const bookBody: {
				appointmentId: number;
				userId: number;
				serviceId?: number;
			} = {
				appointmentId: data.slot_id,
				userId: patient?.user?.id || user.id,
			};
			if (data.service?.trim()) {
				bookBody.serviceId = Number(data.service);
			}
			await bookAppointment(bookBody).unwrap();

			const sidAfter = data.service?.trim();
			let successDescription = "Вы успешно записались на прием к врачу";
			if (sidAfter) {
				const svc =
					doctorServices.find((s) => String(s.id) === sidAfter) ??
					allServicesCatalog.find((s) => String(s.id) === sidAfter) ??
					null;
				const svcName = svc?.name ?? `Услуга #${sidAfter}`;
				const priceLabel = svc
					? formatClinicServicePriceFromFields(svc.price)
					: "—";
				successDescription =
					priceLabel !== "—"
						? `Услуга: ${svcName}. Стоимость: ${priceLabel}.`
						: `Услуга: ${svcName}. Стоимость уточняйте в клинике.`;
			}

			toast.success("Запись успешно создана", {
				description: successDescription,
			});

			if (onClose) {
				onClose();
			}
		} catch (error: any) {
			const errorMessage =
				error.response?.data?.message ||
				"Пожалуйста, попробуйте еще раз";
			toast.error("Ошибка при создании записи", {
				description: errorMessage,
			});
		} finally {
			setIsSubmitting(false);
		}
	};

	return (
		<div className="w-full slide-up">
			<div className="mb-8">
				<div className="flex items-center justify-between">
					<div
						className={`flex h-10 w-10 items-center justify-center rounded-full transition-all duration-500 ${
							step >= 1
								? "bg-blue-600 text-white"
								: "bg-slate-200 text-slate-600"
						}`}
					>
						{step > 1 ? (
							<Check className="h-5 w-5" />
						) : (
							<span>1</span>
						)}
					</div>
					<div
						className={`h-1 flex-1 transition-all duration-500 ${
							step >= 2 ? "bg-blue-600" : "bg-slate-200"
						}`}
					></div>
					<div
						className={`flex h-10 w-10 items-center justify-center rounded-full transition-all duration-500 ${
							step >= 2
								? "bg-blue-600 text-white"
								: "bg-slate-200 text-slate-600"
						}`}
					>
						{step > 2 ? (
							<Check className="h-5 w-5" />
						) : (
							<span>2</span>
						)}
					</div>
				</div>
				<div className="mt-2 flex justify-between">
					<span className="text-sm">Выбор специалиста и времени</span>
					<span className="text-sm">Подтверждение записи</span>
				</div>
			</div>

			<Form {...form}>
				<form
					onSubmit={form.handleSubmit(handleSubmit)}
					className="space-y-6"
				>
					{step === 1 && (
						<Card className="gradient-card scale-in hover-glow">
							<div className="h-1 w-full bg-gradient-to-r from-blue-500 via-purple-500 to-teal-500"></div>
							<CardHeader>
								<CardTitle>
									Выберите специалиста и время
								</CardTitle>
								<CardDescription>Шаг 1 из 2</CardDescription>
							</CardHeader>
							<CardContent className="space-y-6">
								<FormField
									control={form.control}
									name="searchMode"
									render={({ field }) => (
										<FormItem>
											<FormLabel>Способ поиска</FormLabel>
											<FormControl>
												<RadioGroup
													onValueChange={(v) => {
														field.onChange(v);
														form.setValue("specialty", "");
														form.setValue("service", "");
														form.setValue("doctor", "");
														form.setValue("date", "");
														form.setValue("time", "");
														form.setValue("slot_id", undefined);
														setSlots([]);
														setDoctors([]);
														setAvailableDates([]);
													}}
													value={field.value}
													className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:gap-6"
												>
													<div className="flex items-center space-x-2">
														<RadioGroupItem value="by_specialty" id="sm_spec" />
														<Label htmlFor="sm_spec" className="font-normal">
															По специальности и врачу
														</Label>
													</div>
													<div className="flex items-center space-x-2">
														<RadioGroupItem value="by_service" id="sm_svc" />
														<Label htmlFor="sm_svc" className="font-normal">
															По услуге
														</Label>
													</div>
												</RadioGroup>
											</FormControl>
											<FormMessage />
										</FormItem>
									)}
								/>

								{searchMode === "by_specialty" && (
									<FormField
										control={form.control}
										name="specialty"
										render={({ field }) => (
											<FormItem>
												<FormLabel>Специальность</FormLabel>
												<Select
													onValueChange={field.onChange}
													value={field.value}
													disabled={isLoadingSpecialties}
												>
													<FormControl>
														<SelectTrigger className="light-input hover-scale">
															<SelectValue
																placeholder={
																	isLoadingSpecialties
																		? "Загрузка..."
																		: "Выберите специальность"
																}
															/>
															{isLoadingSpecialties && (
																<Loader2 className="h-4 w-4 animate-spin ml-2" />
															)}
														</SelectTrigger>
													</FormControl>
													<SelectContent>
														{specialties.map((specialty) => (
															<SelectItem
																key={specialty.id}
																value={specialty.id.toString()}
															>
																{specialty.name}
															</SelectItem>
														))}
													</SelectContent>
												</Select>
												<FormMessage />
											</FormItem>
										)}
									/>
								)}

								{searchMode === "by_service" && (
									<FormField
										control={form.control}
										name="service"
										render={({ field }) => (
											<FormItem>
												<FormLabel>Услуга</FormLabel>
												<FormControl>
													<ServiceBookingPicker
														services={allServicesCatalog}
														value={field.value?.trim() ?? ""}
														onChange={(id) => {
															field.onChange(id);
															form.setValue("doctor", "");
															form.setValue("date", "");
															form.setValue("time", "");
															form.setValue("slot_id", undefined);
															setSlots([]);
															setAvailableDates([]);
														}}
													/>
												</FormControl>
												<FormMessage />
											</FormItem>
										)}
									/>
								)}

								<FormField
									control={form.control}
									name="doctor"
									render={({ field }) => (
										<FormItem>
											<FormLabel>Врач</FormLabel>
											{searchMode === "by_specialty" && !selectedSpecialty ? (
												<div className="text-sm text-slate-500 p-4 border border-slate-200 rounded-md">
													Сначала выберите специальность
												</div>
											) : searchMode === "by_service" && !selectedService?.trim() ? (
												<div className="text-sm text-slate-500 p-4 border border-slate-200 rounded-md">
													Сначала выберите услугу
												</div>
											) : isLoadingDoctors ? (
												<div className="text-sm text-slate-500 p-4 border border-slate-200 rounded-md">
													Загрузка врачей...
												</div>
											) : doctors.length === 0 ? (
												<div className="text-sm text-slate-500 p-4 border border-slate-200 rounded-md">
													{searchMode === "by_service"
														? "Нет врачей с этой услугой (проверьте связи специализация–услуга в БД)"
														: "Нет доступных врачей по выбранной специальности"}
												</div>
											) : (
												<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
													{doctors.map((doctor) => {
														const fullName = `${doctor.user.lastName} ${doctor.user.firstName} ${doctor.user.middleName}`.trim();
														const photoSrc = doctorPhotoImgSrc(doctor.photo);
														const isSelected = field.value === doctor.id.toString();
														return (
															<div
																key={doctor.id}
																onClick={() => {
																	field.onChange(doctor.id.toString());
																	if (searchMode === "by_specialty") {
																		form.setValue("service", "");
																	}
																	form.setValue("date", "");
																	form.setValue("time", "");
																	form.setValue("slot_id", undefined);
																	setSlots([]);
																}}
																className={`relative cursor-pointer rounded-lg border-2 transition-all hover:shadow-lg ${
																	isSelected
																		? "border-blue-500 bg-blue-50"
																		: "border-slate-200 bg-white hover:border-blue-300"
																}`}
															>
																<div className="p-4">
																	<div className="flex items-start gap-3">
																		{photoSrc ? (
																			<img
																				src={photoSrc}
																				alt={fullName}
																				className="w-16 h-16 rounded-full object-cover border-2 border-slate-200"
																				onError={(e) => {
																					// Если изображение не загрузилось, заменяем на placeholder
																					const target = e.target as HTMLImageElement;
																					target.style.display = "none";
																					const placeholder = document.createElement("div");
																					placeholder.className = "w-16 h-16 rounded-full bg-slate-200 flex items-center justify-center border-2 border-slate-300";
																					placeholder.innerHTML = '<svg class="h-8 w-8 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>';
																					target.parentElement?.insertBefore(placeholder, target);
																				}}
																			/>
																		) : (
																			<div className="w-16 h-16 rounded-full bg-slate-200 flex items-center justify-center border-2 border-slate-300">
																				<User className="h-8 w-8 text-slate-400" />
																			</div>
																		)}
																		<div className="flex-1 min-w-0">
																			<h3 className="font-medium text-sm truncate">
																				{fullName}
																			</h3>
																			{doctor.bio && (
																				<p className="text-xs text-slate-500 mt-1 line-clamp-2">
																					{doctor.bio}
																				</p>
																			)}
																			{doctor.experienceYears > 0 && (
																				<p className="text-xs text-slate-500 mt-1">
																					Опыт: {doctor.experienceYears} {doctor.experienceYears === 1 ? "год" : doctor.experienceYears < 5 ? "года" : "лет"}
																				</p>
																			)}
																			{doctor.rating !== null && doctor.reviewCount > 0 && (
																				<div
																					className="flex items-center gap-1 mt-2"
																					onClick={(e) => {
																						e.stopPropagation();
																						setSelectedDoctorForReviews({
																							id: doctor.id,
																							name: fullName,
																						});
																						setIsReviewsDialogOpen(true);
																					}}
																				>
																					<div className="flex items-center gap-0.5">
																						{[1, 2, 3, 4, 5].map((star) => (
																							<Star
																								key={star}
																								className={`h-3.5 w-3.5 ${
																									star <= Math.round(doctor.rating!)
																										? "fill-yellow-400 text-yellow-400"
																										: "text-slate-300"
																								}`}
																							/>
																						))}
																					</div>
																					<span className="text-xs text-slate-600 ml-1 hover:text-blue-600">
																						{doctor.rating.toFixed(1)} ({doctor.reviewCount})
																					</span>
																				</div>
																			)}
																		</div>
																	</div>
																</div>
																{isSelected && (
																	<div className="absolute top-2 right-2">
																		<div className="w-6 h-6 rounded-full bg-blue-500 flex items-center justify-center">
																			<Check className="h-4 w-4 text-white" />
																		</div>
																	</div>
																)}
															</div>
														);
													})}
												</div>
											)}
											<FormMessage />
										</FormItem>
									)}
								/>

								{searchMode === "by_specialty" && selectedDoctor && (
									<FormField
										control={form.control}
										name="service"
										render={({ field }) => (
											<FormItem>
												<FormLabel>Услуга (необязательно)</FormLabel>
												<Select
													onValueChange={(v) => {
														field.onChange(v === "_none_" ? "" : v);
														form.setValue("time", "");
														form.setValue("slot_id", undefined);
													}}
													value={
														field.value && field.value.trim() !== ""
															? field.value
															: "_none_"
													}
												>
													<FormControl>
														<SelectTrigger className="light-input hover-scale">
															<SelectValue placeholder="Без услуги" />
														</SelectTrigger>
													</FormControl>
													<SelectContent>
														<SelectItem value="_none_">Без услуги</SelectItem>
														{doctorServices.map((s) => (
															<SelectItem key={s.id} value={String(s.id)}>
																{s.name}
																{s.durationMinutes != null
																	? ` · ${s.durationMinutes} мин`
																	: ""}
															</SelectItem>
														))}
													</SelectContent>
												</Select>
												<FormMessage />
											</FormItem>
										)}
									/>
								)}

								<FormField
									control={form.control}
									name="date"
									render={({ field }) => (
										<FormItem className="flex flex-col">
											<FormLabel>Дата</FormLabel>
											{/* Без Radix Popover: DismissableLayer конфликтует с DayPicker (закрытие до выбора) */}
											<div ref={datePickerContainerRef} className="relative w-full">
												<FormControl>
													<Button
														type="button"
														variant="outline"
														disabled={!selectedDoctor}
														aria-expanded={datePickerOpen}
														onClick={() =>
															selectedDoctor && setDatePickerOpen((o) => !o)
														}
														className={cn(
															"light-input hover-scale w-full justify-start text-left font-normal",
															!field.value && "text-muted-foreground"
														)}
													>
														<CalendarIcon className="mr-2 h-4 w-4" />
														{isLoadingDates ? (
															<span>Загрузка дат…</span>
														) : field.value ? (
															format(
																new Date(field.value + "T12:00:00"),
																"d MMMM yyyy",
																{ locale: ru }
															)
														) : (
															"Выберите дату"
														)}
													</Button>
												</FormControl>
												{datePickerOpen && selectedDoctor && (
													<div
														className="absolute left-0 top-full z-50 mt-1 rounded-md border bg-popover text-popover-foreground shadow-md outline-none"
														role="dialog"
														aria-label="Календарь"
													>
														{isLoadingDates ? (
															<div className="flex justify-center py-8 px-12">
																<Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
															</div>
														) : (
															<div className="p-3">
																<Calendar
																	mode="single"
																	locale={ru}
																	selected={
																		field.value
																			? new Date(field.value + "T12:00:00")
																			: undefined
																	}
																	onSelect={(d) => {
																		if (!d) return;
																		const key = format(d, "yyyy-MM-dd");
																		if (!availableDateSet.has(key)) return;
																		field.onChange(key);
																		setDatePickerOpen(false);
																	}}
																	disabled={(d) =>
																		!availableDateSet.has(format(d, "yyyy-MM-dd"))
																	}
																/>
															</div>
														)}
													</div>
												)}
											</div>
											{selectedDoctor &&
												!isLoadingDates &&
												availableDates.length === 0 && (
													<p className="text-sm text-muted-foreground">
														Нет свободных слотов в ближайшие 90 дней для выбранных
														параметров.
													</p>
												)}
											<FormMessage />
										</FormItem>
									)}
								/>

								{selectedDate && (
									<FormField
										control={form.control}
										name="time"
										render={({ field }) => (
											<FormItem>
												<FormLabel>
													Доступное время
												</FormLabel>

												{isLoadingSlots ? (
													<div className="flex justify-center py-4">
														<Loader2 className="h-6 w-6 animate-spin" />
													</div>
												) : slots.length > 0 ? (
													<div className="grid grid-cols-3 gap-2 sm:grid-cols-4 md:grid-cols-5">
														{slots
															.filter((slot) => !slot.isBooked)
															.map((slot) => {
																const slotDate = new Date(slot.startTime);
																const hours = slotDate.getUTCHours().toString().padStart(2, '0');
																const minutes = slotDate.getUTCMinutes().toString().padStart(2, '0');
																const slotTime = `${hours}:${minutes}`;
																return (
																	<Button
																		key={slot.id}
																		type="button"
																		disabled={slot.isBooked}
																		variant={
																			field.value === slotTime
																				? "default"
																				: "outline"
																		}
																		className={`h-auto min-h-[3rem] flex-col gap-0.5 py-2 px-1 ${
																			field.value === slotTime
																				? "gradient-button"
																				: "border-slate-300 hover:bg-slate-100 text-slate-800 hover-scale"
																		}`}
																		onClick={() => {
																			field.onChange(slotTime);
																			form.setValue("slot_id", slot.id);
																		}}
																	>
																		<span className="text-sm font-medium">{slotTime}</span>
																		<span className="max-w-[5.5rem] truncate text-[10px] leading-tight opacity-80">
																			{slotServiceLabel(slot)}
																		</span>
																	</Button>
																);
															})}
													</div>
												) : (
													<div className="text-center text-slate-400 w-full py-4">
														На текущую дату нет
														доступных слотов.
														Пожалуйста выберите
														другую дату.
													</div>
												)}

												<FormMessage />
											</FormItem>
										)}
									/>
								)}

								<div className="flex justify-end">
									<Button
										type="button"
										className="gradient-button"
										disabled={
											(searchMode === "by_specialty" && !selectedSpecialty) ||
											(searchMode === "by_service" && !selectedService?.trim()) ||
											!selectedDoctor ||
											!selectedDate ||
											!form.watch("time") ||
											isLoadingSlots
										}
										onClick={handleNextStep}
									>
										Продолжить
									</Button>
								</div>
							</CardContent>
						</Card>
					)}

					{step === 2 && (
						<Card className="gradient-card scale-in hover-glow w-full">
							<div className="h-1 w-full bg-gradient-to-r from-blue-500 via-purple-500 to-teal-500"></div>
							<CardHeader>
								<CardTitle>Подтверждение записи</CardTitle>
								<CardDescription>Шаг 2 из 2</CardDescription>
							</CardHeader>
							<CardContent className="space-y-6">
								<div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
									<h3 className="text-lg font-medium">
										Информация о приёме
									</h3>
									<div className="mt-4 space-y-2">
										<div className="flex justify-between">
											<span className="text-slate-600">Способ записи:</span>
											<span>
												{searchMode === "by_service"
													? "По услуге"
													: "По специальности"}
											</span>
										</div>
										{searchMode === "by_specialty" && (
											<div className="flex justify-between">
												<span className="text-slate-600">Специальность:</span>
												<span>
													{specialties.find(
														(s) => s.id.toString() === selectedSpecialty
													)?.name ?? "—"}
												</span>
											</div>
										)}
										<div className="flex justify-between">
											<span className="text-slate-600">Услуга:</span>
											<span className="text-right max-w-[60%]">
												{serviceField?.trim()
													? selectedServiceForSummary?.name ??
														`#${serviceField.trim()}`
													: "Без услуги"}
											</span>
										</div>
										{serviceField?.trim() && (
											<div className="flex justify-between">
												<span className="text-slate-600">
													Стоимость услуги:
												</span>
												<span className="text-right font-medium max-w-[60%]">
													{selectedServiceForSummary
														? formatClinicServicePriceFromFields(
																selectedServiceForSummary.price
															)
														: "—"}
												</span>
											</div>
										)}
										<div className="flex justify-between">
											<span className="text-slate-600">
												Врач:
											</span>
											<span>
												{
													doctors.find(
														(d) =>
															d.id.toString() ===
															selectedDoctor
													)?.user.lastName
												}{" "}
												{
													doctors.find(
														(d) =>
															d.id.toString() ===
															selectedDoctor
													)?.user.firstName
												}{" "}
												{
													doctors.find(
														(d) =>
															d.id.toString() ===
															selectedDoctor
													)?.user.middleName
												}
											</span>
										</div>
										<div className="flex justify-between">
											<span className="text-slate-600">
												Дата и время:
											</span>
											<span>
												{new Date(selectedDate).toLocaleDateString("ru-RU", {
													day: "numeric",
													month: "long",
													year: "numeric",
												})}{" "}
												в {form.watch("time")}
											</span>
										</div>
									</div>
								</div>

								<FormField
									control={form.control}
									name="reason"
									render={({ field }) => (
										<FormItem>
											<FormLabel>
												Причина обращения
											</FormLabel>
											<FormControl>
												<Textarea
													placeholder="Опишите причину обращения к врачу"
													className="light-input hover-scale"
													{...field}
												/>
											</FormControl>
											<FormMessage />
										</FormItem>
									)}
								/>

								<FormField
									control={form.control}
									name="notificationType"
									render={({ field }) => (
										<FormItem>
											<FormLabel>
												Способ напоминания
											</FormLabel>
											<FormControl>
												<RadioGroup
													onValueChange={
														field.onChange
													}
													value={field.value}
													className="flex flex-col gap-2"
												>
													<div className="flex items-center space-x-2"></div>
													<div className="flex items-center space-x-2">
														<RadioGroupItem
															value="email"
															id="email"
														/>
														<Label htmlFor="email">
															Email на адрес
															{"   " +
																patient?.user
																	.email}
														</Label>
													</div>
													<div className="flex items-center space-x-2">
														<RadioGroupItem
															value="none"
															id="none"
														/>
														<Label htmlFor="none">
															Не отправлять
														</Label>
													</div>
												</RadioGroup>
											</FormControl>
											<FormMessage />
										</FormItem>
									)}
								/>

								<div className="flex justify-between">
									<Button
										type="button"
										variant="outline"
										className="border-slate-300 hover-scale"
										onClick={handlePrevStep}
										disabled={isSubmitting}
									>
										Назад
									</Button>
									<Button
										type="submit"
										className="gradient-button"
										disabled={isSubmitting}
									>
										{isSubmitting ? (
											<>
												<Loader2 className="h-4 w-4 animate-spin mr-2" />
												Отправка...
											</>
										) : (
											"Подтвердить запись"
										)}
									</Button>
								</div>
							</CardContent>
						</Card>
					)}
				</form>
			</Form>
			{selectedDoctorForReviews && (
				<DoctorReviewsDialog
					doctorId={selectedDoctorForReviews.id}
					doctorName={selectedDoctorForReviews.name}
					open={isReviewsDialogOpen}
					onOpenChange={setIsReviewsDialogOpen}
					canDelete={false}
				/>
			)}
		</div>
	);
}
