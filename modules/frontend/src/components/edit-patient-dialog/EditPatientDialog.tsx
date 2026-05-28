import { zodResolver } from "@hookform/resolvers/zod";
import type React from "react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { PatternFormat } from "react-number-format";

import { useUpdatePatientMutation } from "@/store/api/apiSlice";
import { Patient } from "@/api/types";
import { Button } from "@/components/ui/button";
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
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { toast } from "sonner";

import {
	editPatientFormSchema,
	type EditPatientFormValues,
} from "./edit-patient-form-schema";

interface EditPatientDialogProps {
	patient: Patient;
	trigger?: React.ReactNode;
	onSave?: (patientData: any) => void;
	open?: boolean;
	onOpenChange?: (open: boolean) => void;
}

export function EditPatientDialog({
	patient,
	open: controlledOpen,
	onOpenChange,
}: EditPatientDialogProps) {
	const [updatePatient] = useUpdatePatientMutation();
	const [internalOpen, setInternalOpen] = useState(false);
	const open = controlledOpen ?? internalOpen;
	const setOpen = onOpenChange ?? setInternalOpen;
	const [isSubmitting, setIsSubmitting] = useState(false);

	const form = useForm<EditPatientFormValues>({
		resolver: zodResolver(editPatientFormSchema),
		defaultValues: {
			lastName: patient?.user.lastName || "",
			firstName: patient?.user.firstName || "",
			middleName: patient?.user.middleName || "",
			birthDate: patient?.birthDate || "",
			gender: patient?.gender === 1 ? "male" : "female",
			phone: patient?.user.phone || "",
			email: patient?.user.email || "",
			policyNumber: patient?.insuranceNumber || "",
		},
	});

	const onSubmit = async (values: EditPatientFormValues) => {
		setIsSubmitting(true);
		try {
			// Обновляем пациента с данными пользователя
			await updatePatient({
				id: patient.id,
				body: {
					user: {
						email: values.email || patient.user.email,
						phone: values.phone,
						firstName: values.firstName,
						lastName: values.lastName,
						middleName: values.middleName,
					},
					birthDate: values.birthDate,
					gender: values.gender === "male" ? 1 : 2,
					insuranceNumber: values.policyNumber,
				},
			}).unwrap();

			toast.success("Данные сохранены", {
				description: "Информация о пациенте успешно обновлена",
			});

			setOpen(false);
		} catch (error) {
			toast.error("Ошибка", {
				description: "Не удалось сохранить данные",
			});
		} finally {
			setIsSubmitting(false);
		}
	};

	return (
		<Dialog open={open} onOpenChange={setOpen}>
			<DialogContent className="sm:max-w-[600px]">
				<DialogHeader>
					<DialogTitle>Редактирование данных пациента</DialogTitle>
					<DialogDescription>
						Внесите изменения в информацию о пациенте и нажмите
						"Сохранить" для подтверждения.
					</DialogDescription>
				</DialogHeader>
				<Form {...form}>
					<form
						onSubmit={form.handleSubmit(onSubmit)}
						className="space-y-4"
					>
						<div className="grid grid-cols-2 gap-4">
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
						</div>

						<div className="grid grid-cols-2 gap-4">
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
							<FormField
								control={form.control}
								name="birthDate"
								render={({ field }) => (
									<FormItem>
										<FormLabel>Дата рождения</FormLabel>
										<FormControl>
											<Input
												type="date"
												{...field}
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
							name="gender"
							render={({ field }) => (
								<FormItem>
									<FormLabel>Пол</FormLabel>
									<FormControl>
										<RadioGroup
											onValueChange={field.onChange}
											value={field.value}
											className="flex gap-4"
										>
											<div className="flex items-center space-x-2">
												<RadioGroupItem
													value="male"
													id="male"
												/>
												<Label htmlFor="male">
													Мужской
												</Label>
											</div>
											<div className="flex items-center space-x-2">
												<RadioGroupItem
													value="female"
													id="female"
												/>
												<Label htmlFor="female">
													Женский
												</Label>
											</div>
										</RadioGroup>
									</FormControl>
									<FormMessage />
								</FormItem>
							)}
						/>

						<div className="grid grid-cols-2 gap-4">
							<FormField
								control={form.control}
								name="phone"
								render={({ field }) => (
									<FormItem>
										<FormLabel>Телефон</FormLabel>
										<FormControl>
											<PatternFormat
												customInput={Input}
												format="+7 (###) ###-##-##"
												mask="_"
												placeholder="+7 (900) 123-45-67"
												className="light-input"
												value={field.value}
												onValueChange={(values) => {
													field.onChange(
														values.value
													);
												}}
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
												type="email"
												{...field}
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
							name="policyNumber"
							render={({ field }) => (
								<FormItem>
									<FormLabel>Номер полиса ОМС</FormLabel>
									<FormControl>
										<PatternFormat
											customInput={Input}
											format="#### #### #### ####"
											mask="_"
											placeholder="1234 5678 9012 3456"
											className="light-input"
											value={field.value}
											onValueChange={(values) => {
												field.onChange(
													values.value.replace(
														/\s/g,
														""
													)
												);
											}}
										/>
									</FormControl>
									<FormMessage />
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
