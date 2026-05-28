import {
	type UpdateAppointmentRequest,
	Appointment,
	Doctor,
	Patient,
	Review,
} from "@/api/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
	Dialog,
	DialogContent,
	DialogDescription,
	DialogHeader,
	DialogTitle,
} from "@/components/ui/dialog";
import {
	Form,
	FormControl,
	FormField,
	FormItem,
	FormLabel,
	FormMessage,
} from "@/components/ui/form";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { zodResolver } from "@hookform/resolvers/zod";
import { format } from "date-fns";
import { Star } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";

import {
	useCreateReviewMutation,
	useGetDoctorByIdQuery,
	useGetPatientByIdQuery,
	useLazyGetReviewByAppointmentQuery,
	useSendAppointmentNotificationMutation,
	useUpdateAppointmentMutation,
	useUpdateReviewMutation,
} from "@/store/api/apiSlice";

import {
	diagnosisToFormString,
	formatDiagnosisDisplay,
	statusLabels,
} from "./appointment-display-helpers";
import {
	formSchema,
	reviewFormSchema,
	type FormValues,
	type ReviewFormValues,
} from "./appointment-form-schemas";

interface AppointmentDetailsDialogProps {
	appointment: Appointment | null;
	open: boolean;
	onOpenChange: (open: boolean) => void;
	onUpdate: () => void;
	isPatientView?: boolean;
}

export function AppointmentDetailsDialog({
	appointment,
	open,
	onOpenChange,
	onUpdate,
	isPatientView = false,
}: AppointmentDetailsDialogProps) {
	const [updateAppointment] = useUpdateAppointmentMutation();
	const [sendNotification] = useSendAppointmentNotificationMutation();
	const [updateReview] = useUpdateReviewMutation();
	const [createReview] = useCreateReviewMutation();
	const [getReviewByAppointment] = useLazyGetReviewByAppointmentQuery();

	const docId = appointment?.doctorId;
	const patId = appointment?.patientId;
	const { data: doctorData } = useGetDoctorByIdQuery(docId!, { skip: !open || !docId });
	const { data: patientData } = useGetPatientByIdQuery(patId!, { skip: !open || !patId });

	const [isEditing, setIsEditing] = useState(false);
	const [localAppointment, setLocalAppointment] =
		useState<Appointment | null>(appointment);
	const [isSendingNotification, setIsSendingNotification] = useState(false);
	const [doctor, setDoctor] = useState<Doctor | null>(null);
	const [patient, setPatient] = useState<Patient | null>(null);
	const [review, setReview] = useState<Review | null>(null);
	const [isLoadingReview, setIsLoadingReview] = useState(false);
	const [isSubmittingReview, setIsSubmittingReview] = useState(false);
	const [showReviewForm, setShowReviewForm] = useState(false);

	useEffect(() => {
		setDoctor(doctorData ?? null);
	}, [doctorData]);

	useEffect(() => {
		setPatient(patientData ?? null);
	}, [patientData]);

	const form = useForm<FormValues>({
		resolver: zodResolver(formSchema),
		defaultValues: {
			// В форме редактируем только реальные статусы приёма (без "available")
			status:
				(localAppointment?.status as FormValues["status"]) ||
				"scheduled",
			diagnosis: diagnosisToFormString(localAppointment?.diagnosis),
		},
	});

	const reviewForm = useForm<ReviewFormValues>({
		resolver: zodResolver(reviewFormSchema),
		defaultValues: {
			rating: 5,
			reviewText: "",
		},
	});

	const loadReview = async (appointmentId: number) => {
		setIsLoadingReview(true);
		try {
			const reviewData = await getReviewByAppointment(appointmentId).unwrap();
			setReview(reviewData);
			reviewForm.reset({
				rating: reviewData.rating,
				reviewText: reviewData.reviewText || "",
			});
		} catch (error: unknown) {
			const st =
				error && typeof error === "object" && "status" in error
					? (error as { status?: number }).status
					: undefined;
			if (st !== 404) {
				console.error("Error loading review:", error);
			}
			setReview(null);
		} finally {
			setIsLoadingReview(false);
		}
	};

	useEffect(() => {
		if (appointment && open) {
			setLocalAppointment(appointment);
			form.reset({
				status:
					(appointment.status as FormValues["status"]) || "scheduled",
				diagnosis: diagnosisToFormString(appointment.diagnosis),
			});

			if (isPatientView && appointment.status === "completed") {
				void loadReview(appointment.id);
			}
		} else if (!open) {
			// Сбрасываем состояние при закрытии диалога
			setDoctor(null);
			setPatient(null);
			setReview(null);
			setShowReviewForm(false);
			reviewForm.reset({
				rating: 5,
				reviewText: "",
			});
		}
	}, [appointment, open, form, isPatientView, reviewForm]);

	useEffect(() => {
		const subscription = form.watch((value, { name }) => {
			if (name === "status") {
				const status = value.status;
				if (status === "cancelled") {
					form.setValue("diagnosis", "");
				} else if (status === "scheduled") {
					form.setValue("diagnosis", "");
				} else if (status === "completed") {
					// Можно добавить дополнительную логику, если нужно
				}
			}
		});
		return () => subscription.unsubscribe();
	}, [form]);

	useEffect(() => {
		const status = form.watch("status");
		if (status === "scheduled") {
			form.setValue("diagnosis", "");
		} else if (status === "cancelled") {
			form.setValue("diagnosis", "");
		}
	}, [form.watch("status")]);

	const onSubmit = async (values: FormValues) => {
		if (!localAppointment) return;

		try {
			const body: UpdateAppointmentRequest = {
				status: values.status,
				diagnosis: values.diagnosis?.trim() || null,
			};
			const updatedAppointment = await updateAppointment({
				id: localAppointment.id,
				body,
			}).unwrap();
			setLocalAppointment((prev) =>
				prev ? { ...prev, ...updatedAppointment } : prev
			);
			toast.success("Прием успешно обновлен");
			setIsEditing(false);
			onOpenChange(false);
			onUpdate();
		} catch (error) {
			toast.error("Ошибка при обновлении приема");
		}
	};

	const handleSendNotification = async () => {
		if (!localAppointment) return;

		setIsSendingNotification(true);
		try {
			await sendNotification(localAppointment.id).unwrap();
			toast.success("Уведомление успешно отправлено");
		} catch (error) {
			toast.error("Ошибка при отправке уведомления");
			console.error("Error sending notification:", error);
		} finally {
			setIsSendingNotification(false);
		}
	};

	const handleSubmitReview = async (values: ReviewFormValues) => {
		if (!localAppointment || !localAppointment.doctorId || !localAppointment.patientId) return;

		setIsSubmittingReview(true);
		try {
			if (review) {
				await updateReview({
					id: review.id,
					body: {
						rating: values.rating,
						reviewText: values.reviewText || "",
					},
				}).unwrap();
				toast.success("Отзыв успешно обновлен");
			} else {
				await createReview({
					appointment: {
						id: localAppointment.id,
					},
					doctor: {
						id: localAppointment.doctorId,
					},
					patient: {
						id: localAppointment.patientId,
					},
					rating: values.rating,
					reviewText: values.reviewText || "",
				}).unwrap();
				toast.success("Отзыв успешно добавлен");
			}
			setShowReviewForm(false);
			// Перезагружаем отзыв
			await loadReview(localAppointment.id);
			onUpdate();
		} catch (error: any) {
			console.error("Error submitting review:", error);
			const errorMessage = error.response?.data?.[0] || error.response?.data?.message || "Не удалось сохранить отзыв";
			toast.error("Ошибка при сохранении отзыва", {
				description: errorMessage,
			});
		} finally {
			setIsSubmittingReview(false);
		}
	};

	if (!localAppointment) return null;

	return (
		<Dialog open={open} onOpenChange={onOpenChange}>
			<DialogContent className="max-w-2xl">
				<DialogHeader>
					<DialogTitle>Информация о приеме</DialogTitle>
					<DialogDescription>
						Детальная информация о приеме и возможность его
						редактирования
					</DialogDescription>
				</DialogHeader>

				<div className="grid gap-6">
					{/* Информация о приеме */}
					{!isEditing && (
						<div className="grid gap-4">
							<div className="grid grid-cols-2 gap-4">
								<div>
									<h4 className="font-medium text-sm">
										Дата и время
									</h4>
									<p>
										{format(
											new Date(localAppointment.startTime),
											"dd.MM.yyyy HH:mm"
										)}
									</p>
								</div>
								<div>
									<h4 className="font-medium text-sm">
										Статус
									</h4>
									<Badge>
										{statusLabels[localAppointment.status]}
									</Badge>
								</div>
								<div>
									<h4 className="font-medium text-sm">
										Пациент
									</h4>
									<p>
										{patient ? (
											<>
												{patient.user.lastName}{" "}
												{patient.user.firstName}{" "}
												{patient.user.middleName}
											</>
										) : (
											localAppointment.patientId ? `Пациент ID: ${localAppointment.patientId}` : "-"
										)}
									</p>
								</div>
								<div>
									<h4 className="font-medium text-sm">
										Врач
									</h4>
									<p>
										{doctor ? (
											<>
												{doctor.user.lastName}{" "}
												{doctor.user.firstName}{" "}
												{doctor.user.middleName}
											</>
										) : (
											`Врач ID: ${localAppointment.doctorId}`
										)}
									</p>
								</div>
								<div>
									<h4 className="font-medium text-sm">
										Специальность
									</h4>
									<p>
										{doctor && doctor.specializations.length > 0 ? (
											doctor.specializations.map(spec => spec.name).join(", ")
										) : (
											"-"
										)}
									</p>
								</div>
								<div>
									<h4 className="font-medium text-sm">
										Кабинет
									</h4>
									<p>
										{localAppointment.roomId || "-"}
									</p>
								</div>
							</div>

							<div>
								<h4 className="font-medium text-sm">Диагноз</h4>
								<p className="mt-1">
									{formatDiagnosisDisplay(localAppointment.diagnosis) ||
										"Нет диагноза"}
								</p>
							</div>

							{/* Отзыв для пациентов */}
							{isPatientView && localAppointment.status === "completed" && (
								<div className="mt-4 border-t pt-4">
									<h4 className="font-medium text-sm mb-3">Оценка приёма</h4>
									{isLoadingReview ? (
										<p className="text-sm text-slate-500">Загрузка отзыва...</p>
									) : showReviewForm ? (
										<Form {...reviewForm}>
											<form
												onSubmit={reviewForm.handleSubmit(handleSubmitReview)}
												className="space-y-4"
											>
												<FormField
													control={reviewForm.control}
													name="rating"
													render={({ field }) => (
														<FormItem>
															<FormLabel>Оценка</FormLabel>
															<FormControl>
																<div className="flex items-center gap-2">
																	{[1, 2, 3, 4, 5].map((star) => (
																		<button
																			type="button"
																			key={star}
																			onClick={() => field.onChange(star)}
																			className="focus:outline-none"
																		>
																			<Star
																				className={`h-8 w-8 cursor-pointer transition-colors ${
																					star <= field.value
																						? "fill-yellow-400 text-yellow-400"
																						: "text-slate-300 hover:text-yellow-300"
																				}`}
																			/>
																		</button>
																	))}
																	<span className="ml-2 text-sm text-slate-600">
																		{field.value} из 5
																	</span>
																</div>
															</FormControl>
															<FormMessage />
														</FormItem>
													)}
												/>
												<FormField
													control={reviewForm.control}
													name="reviewText"
													render={({ field }) => (
														<FormItem>
															<FormLabel>Комментарий (необязательно)</FormLabel>
															<FormControl>
																<Textarea
																	placeholder="Оставьте комментарий о приёме..."
																	{...field}
																	rows={4}
																/>
															</FormControl>
															<FormMessage />
														</FormItem>
													)}
												/>
												<div className="flex justify-end gap-2">
													<Button
														type="button"
														variant="outline"
														onClick={() => {
															setShowReviewForm(false);
															reviewForm.reset({
																rating: review?.rating || 5,
																reviewText: review?.reviewText || "",
															});
														}}
													>
														Отмена
													</Button>
													<Button
														type="submit"
														disabled={isSubmittingReview}
													>
														{isSubmittingReview ? "Сохранение..." : review ? "Обновить отзыв" : "Отправить отзыв"}
													</Button>
												</div>
											</form>
										</Form>
									) : review ? (
										<div className="space-y-2">
											<div className="flex items-center gap-1">
												{[1, 2, 3, 4, 5].map((star) => (
													<Star
														key={star}
														className={`h-5 w-5 ${
															star <= review.rating
																? "fill-yellow-400 text-yellow-400"
																: "text-slate-300"
														}`}
													/>
												))}
												<span className="ml-2 text-sm text-slate-600">
													{review.rating} из 5
												</span>
											</div>
											{review.reviewText && (
												<p className="text-sm text-slate-600 mt-2">
													{review.reviewText}
												</p>
											)}
											<Button
												type="button"
												variant="outline"
												size="sm"
												onClick={() => {
													setShowReviewForm(true);
													reviewForm.reset({
														rating: review.rating,
														reviewText: review.reviewText || "",
													});
												}}
												className="mt-2"
											>
												Изменить отзыв
											</Button>
										</div>
									) : (
										<Button
											type="button"
											variant="outline"
											onClick={() => setShowReviewForm(true)}
										>
											Оставить отзыв
										</Button>
									)}
								</div>
							)}
						</div>
					)}

					{/* Форма редактирования */}
					{isEditing && (
						<Form {...form}>
							<form
								onSubmit={form.handleSubmit(onSubmit)}
								className="space-y-4"
							>
								<FormField
									control={form.control}
									name="status"
									render={({ field }) => (
										<FormItem>
											<FormLabel>Статус</FormLabel>
											<Select
												onValueChange={field.onChange}
												defaultValue={field.value}
											>
												<FormControl>
													<SelectTrigger>
														<SelectValue placeholder="Выберите статус" />
													</SelectTrigger>
												</FormControl>
												<SelectContent>
													{Object.entries(
														statusLabels
													).map(([value, label]) => (
														<SelectItem
															key={value}
															value={value}
														>
															{label}
														</SelectItem>
													))}
												</SelectContent>
											</Select>
											<FormMessage />
										</FormItem>
									)}
								/>

								<FormField
									control={form.control}
									name="diagnosis"
									render={({ field }) => (
										<FormItem>
											<FormLabel>Диагноз</FormLabel>
											<FormControl>
												<Textarea
													placeholder="Введите диагноз"
													{...field}
													disabled={
														form.watch("status") !==
														"completed"
													}
												/>
											</FormControl>
											<FormMessage />
										</FormItem>
									)}
								/>

								<div className="flex justify-end gap-2">
									<Button
										type="button"
										variant="outline"
										onClick={() => setIsEditing(false)}
									>
										Отмена
									</Button>
									<Button type="submit">Сохранить</Button>
								</div>
							</form>
						</Form>
					)}

					{/* Кнопки действий */}
					{!isEditing && !isPatientView && (
						<div className="flex justify-end gap-2">
							{localAppointment.status === "scheduled" && localAppointment.patientId && (
								<Button
									variant="outline"
									onClick={handleSendNotification}
									disabled={isSendingNotification}
								>
									{isSendingNotification
										? "Отправка..."
										: "Напомнить о приеме"}
								</Button>
							)}
							{localAppointment.status === "cancelled" && (
								<Button
									variant="outline"
									onClick={handleSendNotification}
									disabled={isSendingNotification}
								>
									{isSendingNotification
										? "Отправка..."
										: "Сообщить об отмене"}
								</Button>
							)}
							{localAppointment.status === "completed" && (
								<Button
									variant="outline"
									onClick={handleSendNotification}
									disabled={isSendingNotification}
								>
									{isSendingNotification
										? "Отправка..."
										: "Отправить информацию о приеме"}
								</Button>
							)}
							{/* Редактирование доступно только если к приёму привязан пациент */}
							{localAppointment.patientId && (
								<Button onClick={() => setIsEditing(true)}>
									Изменить
								</Button>
							)}
						</div>
					)}
					{!isEditing && isPatientView && (
						<div className="flex justify-end gap-2">
							<Button
								variant="outline"
								onClick={() => onOpenChange(false)}
							>
								Закрыть
							</Button>
						</div>
					)}
				</div>
			</DialogContent>
		</Dialog>
	);
}
