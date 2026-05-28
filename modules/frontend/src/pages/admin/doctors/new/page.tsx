import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft } from "lucide-react";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { PatternFormat } from "react-number-format";
import { Link } from "react-router-dom";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
	Popover,
	PopoverContent,
	PopoverTrigger,
} from "@/components/ui/popover";
import { Textarea } from "@/components/ui/textarea";
import { ChevronDown } from "lucide-react";
import { directusAssetPreviewUrl, uploadImageToDirectus } from "@/lib/directusUpload";
import { cn } from "@/lib/utils";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

import { useCreateDoctorMutation, useGetSpecializationsQuery } from "@/store/api/apiSlice";
const doctorSchema = z.object({
	lastName: z.string().min(2, "Фамилия должна содержать минимум 2 символа"),
	firstName: z.string().min(2, "Имя должно содержать минимум 2 символа"),
	middleName: z.string().optional(),
	phone: z
		.string()
		.regex(/^\+7 \(\d{3}\) \d{3}-\d{2}-\d{2}$/, "Неверный формат телефона"),
	email: z.string().email("Неверный формат email").min(1, "Email обязателен"),
	experienceYears: z
		.string()
		.min(1, "Укажите количество лет стажа")
		.refine((val) => {
			const num = parseInt(val);
			return num >= 0 && num <= 80;
		}, "Стаж должен быть от 0 до 80 лет"),
	bio: z
		.string()
		.max(50, "Биография не должна превышать 50 символов")
		.optional(),
	specializationIds: z.array(z.number()).optional(),
	photo: z.string().optional(),
});

type DoctorFormData = z.infer<typeof doctorSchema>;

export default function NewDoctorPage() {
	const navigate = useNavigate();
	const { data: specializations = [], isLoading: isLoadingSpecializations } =
		useGetSpecializationsQuery();
	const [createDoctor] = useCreateDoctorMutation();
	const [isSubmitting, setIsSubmitting] = useState(false);
	const [photoPreview, setPhotoPreview] = useState<string | null>(null);
	const [isPhotoUploading, setIsPhotoUploading] = useState(false);

	const {
		register,
		handleSubmit,
		formState: { errors },
		control,
		watch,
		setValue,
	} = useForm<DoctorFormData>({
		resolver: zodResolver(doctorSchema),
		defaultValues: {
			phone: "",
			bio: "",
			specializationIds: [],
			photo: undefined,
		},
	});

	const selectedSpecializationIds = watch("specializationIds") || [];

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
			setValue("photo", fileId);
			setPhotoPreview(directusAssetPreviewUrl(fileId));
			toast.success("Фото загружено в Directus");
		} catch (err: unknown) {
			const msg = err instanceof Error ? err.message : String(err);
			toast.error("Не удалось загрузить фото", { description: msg });
		} finally {
			setIsPhotoUploading(false);
		}
	};

	const onSubmit = async (data: DoctorFormData) => {
		setIsSubmitting(true);
		try {
			// Формируем displayName из ФИО
			const displayName = [
				data.lastName,
				data.firstName,
				data.middleName,
			]
				.filter(Boolean)
				.join(" ");

			// Формируем запрос согласно API
			const requestData = {
				user: {
					email: data.email,
					...(data.phone && { phone: data.phone }),
					firstName: data.firstName,
					lastName: data.lastName,
					middleName: data.middleName || "",
				},
				displayName: displayName,
				...(data.bio && { bio: data.bio }),
				...(data.experienceYears && {
					experienceYears: parseInt(data.experienceYears),
				}),
				...(data.photo ? { photo: data.photo } : { photo: null }),
				...(data.specializationIds &&
					data.specializationIds.length > 0 && {
						specializationIds: data.specializationIds,
					}),
			};

			await createDoctor(requestData).unwrap();
			navigate("/admin/doctors");
			toast.success("Врач добавлен", {
				description: "Новый врач успешно добавлен в систему",
			});
		} catch (error: any) {
			const errorData = error.response?.data;
			if (typeof errorData === 'object' && errorData !== null) {
				// Получаем первое сообщение об ошибке из объекта
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
						description: "Не удалось добавить врача"
					});
				}
			} else {
				toast.error("Ошибка", {
					description: error.message || "Не удалось добавить врача"
				});
			}
		} finally {
			setIsSubmitting(false);
		}
	};

	return (
		<div className="flex flex-1 flex-col gradient-bg">
			<main className=" py-8">
				<div className="container mx-auto px-4">
					<div className="mb-8">
						<Button variant="ghost" asChild className="mb-2 hover:">
							<Link to="/admin/doctors">
								<ArrowLeft className="mr-2 h-4 w-4" />
								Назад к списку врачей
							</Link>
						</Button>
						<h2 className="text-3xl font-bold gradient-heading">
							Добавить нового врача
						</h2>
					</div>

					<Card className="mx-auto max-w-2xl gradient-card">
						<div className="h-1 w-full bg-gradient-to-r from-blue-500 via-purple-500 to-teal-500"></div>
						<CardHeader>
							<CardTitle>Информация о враче</CardTitle>
							<CardDescription>
								Введите данные нового врача
							</CardDescription>
						</CardHeader>
						<CardContent>
							<form
								onSubmit={handleSubmit(onSubmit)}
								className="space-y-6"
							>
								<div className="grid gap-4 md:grid-cols-2">
									<div className="space-y-2">
										<Label htmlFor="lastName">
											Фамилия
										</Label>
										<Input
											id="lastName"
											placeholder="Иванов"
											{...register("lastName")}
											className="border-slate-700"
										/>
										{errors.lastName && (
											<p className="text-sm text-red-500">
												{errors.lastName.message}
											</p>
										)}
									</div>
									<div className="space-y-2">
										<Label htmlFor="firstName">Имя</Label>
										<Input
											id="firstName"
											placeholder="Иван"
											{...register("firstName")}
											className="border-slate-700"
										/>
										{errors.firstName && (
											<p className="text-sm text-red-500">
												{errors.firstName.message}
											</p>
										)}
									</div>
								</div>

								<div className="space-y-2">
									<Label htmlFor="middleName">
										Отчество
									</Label>
									<Input
										id="middleName"
										placeholder="Иванович"
										{...register("middleName")}
										className="border-slate-700"
									/>
									{errors.middleName && (
										<p className="text-sm text-red-500">
											{errors.middleName.message}
										</p>
									)}
								</div>

								<div className="grid gap-4 md:grid-cols-2">
									<div className="space-y-2">
										<Label htmlFor="phone">Телефон</Label>
										<Controller
											name="phone"
											control={control}
											render={({ field }) => (
												<PatternFormat
													format="+7 (###) ###-##-##"
													mask="_"
													customInput={Input}
													{...field}
													id="phone"
													placeholder="+7 (900) 123-45-67"
													className="border-slate-700"
													onValueChange={(values) =>
														field.onChange(
															values.formattedValue
														)
													}
												/>
											)}
										/>
										{errors.phone && (
											<p className="text-sm text-red-500">
												{errors.phone.message}
											</p>
										)}
									</div>
									<div className="space-y-2">
										<Label htmlFor="email">Email</Label>
										<Input
											id="email"
											type="email"
											placeholder="doctor@clinic.ru"
											{...register("email")}
											className="border-slate-700"
										/>
										{errors.email && (
											<p className="text-sm text-red-500">
												{errors.email.message}
											</p>
										)}
									</div>
								</div>

								<div className="space-y-2">
									<Label htmlFor="experienceYears">
										Стаж работы (лет)
									</Label>
									<Input
										id="experienceYears"
										type="number"
										placeholder="10"
										{...register("experienceYears")}
										className="border-slate-700"
									/>
									{errors.experienceYears && (
										<p className="text-sm text-red-500">
											{errors.experienceYears.message}
										</p>
									)}
								</div>

								<div className="space-y-2">
									<Label htmlFor="bio">О враче (до 50 символов)</Label>
									<Textarea
										id="bio"
										placeholder="О враче"
										{...register("bio")}
										className="border-slate-700"
										maxLength={50}
									/>
									{errors.bio && (
										<p className="text-sm text-red-500">
											{errors.bio.message}
										</p>
									)}
								</div>

								<div className="space-y-2">
									<Label htmlFor="photo">Фото врача</Label>
									<div className="flex items-center gap-4">
										{photoPreview && (
											<img
												src={photoPreview}
												alt="Предпросмотр"
												className="w-24 h-24 rounded-full object-cover border-2 border-slate-300"
											/>
										)}
										<Input
											id="photo"
											type="file"
											accept="image/*"
											onChange={handlePhotoChange}
											disabled={isPhotoUploading}
											className="border-slate-700"
										/>
									</div>
									<p className="text-xs text-slate-500">
										Directus: VITE_DIRECTUS_URL, VITE_DIRECTUS_STATIC_TOKEN. Максимум 5MB.
									</p>
								</div>

								<div className="space-y-2">
									<Label>Специализации</Label>
									<Controller
										name="specializationIds"
										control={control}
										render={({ field }) => (
											<Popover>
												<PopoverTrigger asChild>
													<button
														type="button"
														className={cn(
															"flex h-10 w-full items-center justify-between rounded-md border border-slate-700 bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
															!field.value || field.value.length === 0
																? "text-muted-foreground"
																: "text-foreground"
														)}
													>
														<span className="truncate">
															{isLoadingSpecializations
																? "Загрузка..."
																: field.value && field.value.length > 0
																	? `Выбрано: ${field.value.length}`
																	: "Выберите специализации"}
														</span>
														<ChevronDown className="h-4 w-4 opacity-50" />
													</button>
												</PopoverTrigger>
												<PopoverContent
													className="w-[var(--radix-popover-trigger-width)] p-0"
													align="start"
												>
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
															<div className="space-y-2">
																{specializations.map(
																	(specialization) => {
																		const isChecked =
																			field.value?.includes(
																				specialization.id
																			) || false;
																		return (
																			<div
																				key={specialization.id}
																				className="flex items-start space-x-2 rounded-sm p-2 hover:bg-accent cursor-pointer"
																				onClick={() => {
																					const currentIds =
																						field.value || [];
																					if (isChecked) {
																						field.onChange(
																							currentIds.filter(
																								(id) =>
																									id !==
																									specialization.id
																							)
																						);
																					} else {
																						field.onChange([
																							...currentIds,
																							specialization.id,
																						]);
																					}
																				}}
																			>
																				<Checkbox
																					checked={isChecked}
																					onCheckedChange={(
																						checked
																					) => {
																						const currentIds =
																							field.value || [];
																						if (checked) {
																							field.onChange([
																								...currentIds,
																								specialization.id,
																							]);
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
																			</div>
																		);
																	}
																)}
															</div>
														)}
													</div>
												</PopoverContent>
											</Popover>
										)}
									/>
									{errors.specializationIds && (
										<p className="text-sm text-red-500">
											{errors.specializationIds.message}
										</p>
									)}
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
																setValue(
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
								</div>


								<div className="flex justify-end gap-4">
									<Button
										variant="outline"
										asChild
										className="border-slate-700 hover:"
									>
										<Link to="/admin/doctors">Отмена</Link>
									</Button>
									<Button
										type="submit"
										disabled={isSubmitting}
										className="gradient-button"
									>
										{isSubmitting
											? "Сохранение..."
											: "Сохранить"}
									</Button>
								</div>
							</form>
						</CardContent>
					</Card>
				</div>
			</main>
		</div>
	);
}
