import { zodResolver } from "@hookform/resolvers/zod";
import type React from "react";
import { useEffect, useRef, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { PatternFormat } from "react-number-format";

import { Doctor } from "@/api/types";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
	Dialog,
	DialogContent,
	DialogDescription,
	DialogFooter,
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
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { ChevronDown } from "lucide-react";
import { directusAssetPreviewUrl, uploadImageToDirectus } from "@/lib/directusUpload";
import { doctorPhotoImgSrc } from "@/lib/doctorPhotoSrc";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

import {
	useGetSpecializationsQuery,
	useUpdateDoctorMutation,
} from "@/store/api/apiSlice";

import {
	editDoctorFormSchema,
	type EditDoctorFormValues,
} from "./edit-doctor-form-schema";

interface EditDoctorDialogProps {
	doctor?: Doctor;
	trigger?: React.ReactNode;
	onSave?: (doctorData: any) => void;
	open?: boolean;
	onOpenChange?: (open: boolean) => void;
}

export function EditDoctorDialog({
	doctor,
	open: controlledOpen,
	onOpenChange,
}: EditDoctorDialogProps) {
	const { data: specializations = [], isLoading: isLoadingSpecializations } =
		useGetSpecializationsQuery();
	const [updateDoctor] = useUpdateDoctorMutation();
	const [internalOpen, setInternalOpen] = useState(false);
	const open = controlledOpen ?? internalOpen;
	const setOpen = onOpenChange ?? setInternalOpen;
	const [isSubmitting, setIsSubmitting] = useState(false);
	const form = useForm<EditDoctorFormValues>({
		resolver: zodResolver(editDoctorFormSchema),
		defaultValues: {
			lastName: "",
			firstName: "",
			middleName: "",
			phone: "",
			email: "",
			experienceYears: "",
			bio: "",
			specializationIds: [],
			photo: undefined,
		},
	});
	const [isMultiSelectOpen, setIsMultiSelectOpen] = useState(false);
	const [photoPreview, setPhotoPreview] = useState<string | null>(null);
	const [isPhotoUploading, setIsPhotoUploading] = useState(false);
	const selectedSpecializationIds = form.watch("specializationIds") || [];
	const multiSelectRef = useRef<HTMLDivElement>(null);

	// Закрываем мультиселект при клике вне его
	useEffect(() => {
		const handleClickOutside = (event: MouseEvent) => {
			if (
				multiSelectRef.current &&
				!multiSelectRef.current.contains(event.target as Node)
			) {
				setIsMultiSelectOpen(false);
			}
		};

		if (isMultiSelectOpen) {
			document.addEventListener("mousedown", handleClickOutside);
		}

		return () => {
			document.removeEventListener("mousedown", handleClickOutside);
		};
	}, [isMultiSelectOpen]);

	// Обновляем форму при изменении doctor
	useEffect(() => {
		if (doctor && open) {
			const photoUrl = doctorPhotoImgSrc(
				doctor.photo,
				import.meta.env.VITE_DIRECTUS_URL,
			);
			form.reset({
				lastName: doctor.user.lastName || "",
				firstName: doctor.user.firstName || "",
				middleName: doctor.user.middleName || "",
				phone: doctor.user.phone || "",
				email: doctor.user.email || "",
				experienceYears: doctor.experienceYears?.toString() || "",
				bio: doctor.bio || "",
				specializationIds: doctor.specializations?.map((s) => s.id) || [],
				photo: doctor.photo || undefined,
			});
			setPhotoPreview(photoUrl);
		} else if (!open) {
			setPhotoPreview(null);
		}
	}, [doctor, open]);

	const handlePhotoChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
		const file = e.target.files?.[0];
		e.target.value = "";
		if (!file) return;
		if (file.size > 5 * 1024 * 1024) {
			toast.error("Ошибка", {
				description: "Размер файла не должен превышать 5MB",
			});
			return;
		}
		if (!file.type.startsWith("image/")) {
			toast.error("Ошибка", {
				description: "Выберите изображение",
			});
			return;
		}
		setIsPhotoUploading(true);
		try {
			const fileId = await uploadImageToDirectus(file);
			form.setValue("photo", fileId);
			setPhotoPreview(directusAssetPreviewUrl(fileId));
			toast.success("Фото загружено в Directus");
		} catch (err: unknown) {
			const msg = err instanceof Error ? err.message : String(err);
			toast.error("Не удалось загрузить фото", { description: msg });
		} finally {
			setIsPhotoUploading(false);
		}
	};

	const onSubmit = async (values: EditDoctorFormValues) => {
		setIsSubmitting(true);
		try {
			// Формируем displayName из ФИО
			const displayName = [
				values.lastName,
				values.firstName,
				values.middleName,
			]
				.filter(Boolean)
				.join(" ");

			// Формируем запрос согласно API
			const requestData = {
				user: {
					email: values.email,
					...(values.phone && { phone: values.phone }),
					firstName: values.firstName,
					lastName: values.lastName,
					middleName: values.middleName || "",
				},
				displayName: displayName,
				...(values.bio && { bio: values.bio }),
				...(values.experienceYears && {
					experienceYears: parseInt(values.experienceYears),
				}),
				...(values.photo ? { photo: values.photo } : { photo: null }),
				...(values.specializationIds &&
					values.specializationIds.length > 0 && {
						specializationIds: values.specializationIds,
					}),
			};

			await updateDoctor({
				id: doctor?.id || 0,
				body: requestData,
			}).unwrap();
			toast.success("Данные сохранены", {
				description: "Информация о враче успешно обновлена",
			});

			setOpen(false);
		} catch (error: any) {
			const errorData = error.response?.data;
			if (typeof errorData === 'object' && errorData !== null) {
				const firstError = Object.values(errorData)[0];
				if (Array.isArray(firstError)) {
					toast.error("Ошибка", {
						description: firstError[0] as string
					});
				} else if (typeof firstError === 'string') {
					toast.error("Ошибка", {
						description: firstError
					});
				} else {
					toast.error("Ошибка", {
						description: "Не удалось сохранить данные"
					});
				}
			} else {
				toast.error("Ошибка", {
					description: error.message || "Не удалось сохранить данные"
				});
			}
		} finally {
			setIsSubmitting(false);
		}
	};

	return (
		<Dialog open={open} onOpenChange={setOpen}>
			<DialogContent className="sm:max-w-[600px]">
				<DialogHeader>
					<DialogTitle>Редактирование данных врача</DialogTitle>
					<DialogDescription>
						Внесите изменения в информацию о враче и нажмите
						"Сохранить" для подтверждения.
					</DialogDescription>
				</DialogHeader>
				<Form {...form}>
					<form
						onSubmit={form.handleSubmit(onSubmit)}
						className="space-y-4"
					>
						<div className="grid grid-cols-3 gap-4">
							<FormField
								control={form.control}
								name="lastName"
								render={({ field }) => (
									<FormItem>
										<FormLabel>Фамилия</FormLabel>
										<FormControl>
											<Input
												{...field}
												className="light-input"
											/>
										</FormControl>
										<FormMessage />
									</FormItem>
								)}
							/>
							<FormField
								control={form.control}
								name="firstName"
								render={({ field }) => (
									<FormItem>
										<FormLabel>Имя</FormLabel>
										<FormControl>
											<Input
												{...field}
												className="light-input"
											/>
										</FormControl>
										<FormMessage />
									</FormItem>
								)}
							/>
							<FormField
								control={form.control}
								name="middleName"
								render={({ field }) => (
									<FormItem>
										<FormLabel>Отчество</FormLabel>
										<FormControl>
											<Input
												{...field}
												className="light-input"
											/>
										</FormControl>
										<FormMessage />
									</FormItem>
								)}
							/>
						</div>

						<div className="grid grid-cols-2 gap-4">
							<FormField
								control={form.control}
								name="phone"
								render={() => (
									<FormItem>
										<FormLabel>Телефон</FormLabel>
										<FormControl>
											<Controller
												control={form.control}
												name="phone"
												render={({ field: controllerField }) => (
													<PatternFormat
														format="+7 (###) ###-##-##"
														mask="_"
														customInput={Input}
														{...controllerField}
														placeholder="+7 (900) 123-45-67"
														className="light-input"
														onValueChange={(values) =>
															controllerField.onChange(
																values.formattedValue
															)
														}
													/>
												)}
											/>
										</FormControl>
										<FormMessage />
									</FormItem>
								)}
							/>
							<FormField
								control={form.control}
								name="email"
								render={({ field }) => (
									<FormItem>
										<FormLabel>Email</FormLabel>
										<FormControl>
											<Input
												{...field}
												type="email"
												className="light-input"
											/>
										</FormControl>
										<FormMessage />
									</FormItem>
								)}
							/>
						</div>

						<FormField
							control={form.control}
							name="experienceYears"
							render={({ field }) => (
								<FormItem>
									<FormLabel>Стаж работы (лет)</FormLabel>
									<FormControl>
										<Input
											{...field}
											type="number"
											className="light-input"
										/>
									</FormControl>
									<FormMessage />
								</FormItem>
							)}
						/>

						<FormField
							control={form.control}
							name="bio"
							render={({ field }) => (
								<FormItem>
									<FormLabel>О враче (до 50 символов)</FormLabel>
									<FormControl>
										<Textarea
											{...field}
											className="light-input min-h-[100px]"
											maxLength={50}
										/>
									</FormControl>
									<FormMessage />
								</FormItem>
							)}
						/>

						<FormField
							control={form.control}
							name="photo"
							render={() => (
								<FormItem>
									<FormLabel>Фото врача</FormLabel>
									<FormControl>
										<div className="space-y-2">
											{photoPreview && (
												<img
													src={photoPreview}
													alt="Предпросмотр"
													className="w-24 h-24 rounded-full object-cover border-2 border-slate-300"
												/>
											)}
											<Input
												type="file"
												accept="image/*"
												onChange={handlePhotoChange}
												disabled={isPhotoUploading}
												className="light-input"
											/>
										</div>
									</FormControl>
									<p className="text-xs text-slate-500">
										Загрузка в Directus (нужны VITE_DIRECTUS_URL и VITE_DIRECTUS_STATIC_TOKEN).
										Максимум 5MB.
									</p>
									<FormMessage />
								</FormItem>
							)}
						/>

						<FormField
							control={form.control}
							name="specializationIds"
							render={({ field }) => (
								<FormItem>
									<FormLabel>Специализации</FormLabel>
									<FormControl>
										<div className="relative" ref={multiSelectRef}>
											<button
												type="button"
												onClick={() => setIsMultiSelectOpen(!isMultiSelectOpen)}
												className={cn(
													"flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
													!(field.value || []).length
														? "text-muted-foreground"
														: "text-foreground"
												)}
											>
												<span className="truncate">
													{isLoadingSpecializations
														? "Загрузка..."
														: (field.value || []).length > 0
															? `Выбрано: ${(field.value || []).length}`
															: "Выберите специализации"}
												</span>
												<ChevronDown
													className={cn(
														"h-4 w-4 opacity-50 transition-transform",
														isMultiSelectOpen && "rotate-180"
													)}
												/>
											</button>
											{isMultiSelectOpen && (
												<div className="absolute z-50 mt-1 w-full rounded-md border bg-popover text-popover-foreground shadow-md">
													<div className="max-h-60 overflow-y-auto p-2">
														{isLoadingSpecializations ? (
															<p className="text-sm text-muted-foreground p-2">
																Загрузка специализаций...
															</p>
														) : specializations.length === 0 ? (
															<p className="text-sm text-muted-foreground p-2">
																Специализации не найдены
															</p>
														) : (
															<div className="space-y-1">
																{specializations.map(
																	(specialization) => {
																		const currentValue = field.value || [];
																		const isChecked = currentValue.includes(
																			specialization.id
																		);
																		return (
																			<label
																				key={specialization.id}
																				htmlFor={`spec-${specialization.id}`}
																				className="flex items-start space-x-2 rounded-sm p-2 hover:bg-accent cursor-pointer"
																			>
																				<Checkbox
																					id={`spec-${specialization.id}`}
																					checked={isChecked}
																					onCheckedChange={(checked) => {
																						const currentIds = field.value || [];
																						if (checked) {
																							if (!currentIds.includes(specialization.id)) {
																								field.onChange([
																									...currentIds,
																									specialization.id,
																								]);
																							}
																						} else {
																							field.onChange(
																								currentIds.filter(
																									(id) =>
																										id !==
																										specialization.id
																								)
																							);
																						}
																					}}
																				/>
																				<div className="flex-1">
																					<div className="text-sm font-medium leading-none">
																						{
																							specialization.name
																						}
																					</div>
																					{specialization.description && (
																						<div className="text-xs text-muted-foreground mt-1">
																							{
																								specialization.description
																							}
																						</div>
																					)}
																				</div>
																			</label>
																		);
																	}
																)}
															</div>
														)}
													</div>
												</div>
											)}
										</div>
									</FormControl>
									<FormMessage />
									{selectedSpecializationIds.length > 0 && (
										<div className="flex flex-wrap gap-2 mt-2">
											{selectedSpecializationIds.map((id) => {
												const specialization =
													specializations.find((s) => s.id === id);
												if (!specialization) return null;
												return (
													<span
														key={id}
														className="inline-flex items-center rounded-md bg-slate-800 px-2 py-1 text-xs font-medium text-slate-200"
													>
														{specialization.name}
														<button
															type="button"
															onClick={() => {
																const currentIds =
																	selectedSpecializationIds || [];
																form.setValue(
																	"specializationIds",
																	currentIds.filter(
																		(selectedId) =>
																			selectedId !== id
																	),
																	{ shouldValidate: true }
																);
															}}
															className="ml-1.5 inline-flex h-4 w-4 items-center justify-center rounded-full hover:bg-slate-700"
														>
															×
														</button>
													</span>
												);
											})}
										</div>
									)}
								</FormItem>
							)}
						/>

						<DialogFooter>
							<Button
								type="button"
								variant="outline"
								onClick={() => setOpen(false)}
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
									"Сохранить"
								)}
							</Button>
						</DialogFooter>
					</form>
				</Form>
			</DialogContent>
		</Dialog>
	);
}
