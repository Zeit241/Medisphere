import { zodResolver } from "@hookform/resolvers/zod";
import type React from "react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import * as z from "zod";

import { Room } from "@/api/types";
import { Button } from "@/components/ui/button";
import {
	Dialog,
	DialogContent,
	DialogDescription,
	DialogFooter,
	DialogHeader,
	DialogTitle,
	DialogTrigger,
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
import { toast } from "sonner";

import { useCreateRoomMutation } from "@/store/api/apiSlice";

const formSchema = z.object({
	code: z.string().min(1, "Код кабинета обязателен для заполнения"),
	name: z.string().optional(),
});

interface CreateRoomDialogProps {
	open?: boolean;
	onOpenChange?: (open: boolean) => void;
	trigger?: React.ReactNode;
	onRoomCreated?: (room: Room) => void;
}

export function CreateRoomDialog({
	open: controlledOpen,
	onOpenChange,
	trigger,
	onRoomCreated,
}: CreateRoomDialogProps) {
	const [createRoom] = useCreateRoomMutation();
	const [internalOpen, setInternalOpen] = useState(false);
	const open = controlledOpen ?? internalOpen;
	const setOpen = onOpenChange ?? setInternalOpen;
	const [isSubmitting, setIsSubmitting] = useState(false);

	const form = useForm<z.infer<typeof formSchema>>({
		resolver: zodResolver(formSchema),
		defaultValues: {
			code: "",
			name: "",
		},
	});

	// Функция для обработки ошибок валидации от API
	const handleApiError = (error: any): string => {
		if (!error.response?.data) {
			return "Не удалось создать кабинет. Пожалуйста, попробуйте позже.";
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

	const onSubmit = async (data: z.infer<typeof formSchema>) => {
		setIsSubmitting(true);
		try {
			const room = await createRoom({
				code: data.code,
				name: data.name || undefined,
			}).unwrap();

			toast.success("Кабинет создан", {
				description: "Кабинет успешно добавлен",
			});

			form.reset();
			setOpen(false);
			onRoomCreated?.(room);
		} catch (error: any) {
			console.error("Ошибка при создании кабинета:", error);
			const errorMessage = handleApiError(error);
			toast.error("Ошибка при создании кабинета", {
				description: errorMessage,
			});
		} finally {
			setIsSubmitting(false);
		}
	};

	return (
		<Dialog open={open} onOpenChange={setOpen}>
			{trigger && <DialogTrigger asChild>{trigger}</DialogTrigger>}
			<DialogContent className="sm:max-w-[500px]">
				<DialogHeader>
					<DialogTitle>Создать кабинет</DialogTitle>
					<DialogDescription>
						Заполните информацию о новом кабинете
					</DialogDescription>
				</DialogHeader>
				<Form {...form}>
					<form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
						<FormField
							control={form.control}
							name="code"
							render={({ field }) => (
								<FormItem>
									<FormLabel>Код кабинета *</FormLabel>
									<FormControl>
										<Input
											{...field}
											placeholder="Например: 101"
											className="light-input"
										/>
									</FormControl>
									<FormMessage />
								</FormItem>
							)}
						/>
						<FormField
							control={form.control}
							name="name"
							render={({ field }) => (
								<FormItem>
									<FormLabel>Название кабинета</FormLabel>
									<FormControl>
										<Input
											{...field}
											placeholder="Например: Кабинет терапевта №1"
											className="light-input"
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
										Создание...
									</span>
								) : (
									"Создать"
								)}
							</Button>
						</DialogFooter>
					</form>
				</Form>
			</DialogContent>
		</Dialog>
	);
}

