import { MoreVertical, PlusCircle, User } from "lucide-react";
import { Link } from "react-router-dom";

import { Doctor } from "@/api/types";
import { DoctorReviewsDialog } from "@/components/doctor-reviews-dialog";
import { DoctorScheduleDialog } from "@/components/doctor-schedule-dialog";
import { EditDoctorDialog } from "@/components/edit-doctor-dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
	Dialog,
	DialogContent,
	DialogDescription,
	DialogFooter,
	DialogHeader,
	DialogTitle,
} from "@/components/ui/dialog";
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { doctorPhotoImgSrc } from "@/lib/doctorPhotoSrc";
import { Input } from "@/components/ui/input";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { useDeleteDoctorMutation, useGetDoctorsQuery } from "@/store/api/apiSlice";

export default function DoctorsPage() {
	const [appliedQuery, setAppliedQuery] = useState<string | undefined>(undefined);
	const { data: doctors = [], isLoading, refetch } = useGetDoctorsQuery(
		appliedQuery ? { q: appliedQuery } : undefined
	);
	const [deleteDoctor] = useDeleteDoctorMutation();
	const [search, setSearch] = useState("");
	const [selectedDoctor, setSelectedDoctor] = useState<Doctor | null>(null);
	const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
	const [isScheduleDialogOpen, setIsScheduleDialogOpen] = useState(false);
	const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
	const [isReviewsDialogOpen, setIsReviewsDialogOpen] = useState(false);

	const filteredDoctors = useMemo(
		() =>
			doctors.filter(
				(doctor) =>
					doctor.user.lastName
						.toLowerCase()
						.includes(search.toLowerCase()) ||
					doctor.user.firstName
						.toLowerCase()
						.includes(search.toLowerCase()) ||
					doctor.user.middleName
						?.toLowerCase()
						.includes(search.toLowerCase()) ||
					doctor.specializations.some((spec) =>
						spec.name.toLowerCase().includes(search.toLowerCase())
					) ||
					doctor.user.email.toLowerCase().includes(search.toLowerCase()) ||
					doctor.user.phone.includes(search)
			),
		[search, doctors],
	);

	const fetchDoctors = () => {
		setAppliedQuery(search.trim() || undefined);
		void refetch();
	};

	const handleDelete = async () => {
		if (selectedDoctor) {
			try {
				await deleteDoctor(selectedDoctor.id).unwrap();
				void refetch();
				setIsDeleteDialogOpen(false);
				toast.success("Врач удален", {
					description: "Врач успешно удален из системы",
				});
			} catch (error) {
				console.error("Ошибка при удалении врача:", error);
				toast.error("Ошибка при удалении врача");
			}
		}
	};

	return (
		<div className="flex-1 flex flex-col gradient-bg">
			<main className="py-8">
				<div className="container mx-auto px-4">
					<div className="mb-8 flex items-center justify-between">
						<h2 className="text-3xl font-bold gradient-heading">
							Врачи
						</h2>
						<Button asChild className="gradient-button">
							<Link to="/admin/doctors/new">
								<PlusCircle className="mr-2 h-4 w-4" />
								Добавить врача
							</Link>
						</Button>
					</div>

					<Card className="mb-6 gradient-card">
						<CardHeader>
							<CardTitle>Поиск врача</CardTitle>
						</CardHeader>
						<CardContent>
							<div className="flex flex-col gap-4 md:flex-row">
								<Input
									value={search}
									onChange={(e) => setSearch(e.target.value)}
									placeholder="Поиск по имени или специальности"
									className="flex-1 border-slate-700"
								/>
								<Button
									className="gradient-button"
									onClick={fetchDoctors}
								>
									Поиск
								</Button>
							</div>
						</CardContent>
					</Card>

					<Card className="gradient-card">
						<CardContent className="p-0">
							<Table>
								<TableHeader className="">
									<TableRow className="">
								<TableHead>Фото</TableHead>
								<TableHead>ФИО</TableHead>
								<TableHead>Специальность</TableHead>
								<TableHead>Email</TableHead>
								<TableHead>Телефон</TableHead>
								<TableHead>Опыт</TableHead>
								<TableHead>Действия</TableHead>
									</TableRow>
								</TableHeader>
								<TableBody>
									{!isLoading &&
										filteredDoctors.map((doctor) => {
											const rowPhotoSrc = doctorPhotoImgSrc(doctor.photo);
											return (
											<TableRow
												className=" border-slate-700"
												key={doctor.id}
											>
												<TableCell>
													{rowPhotoSrc ? (
														<img
															src={rowPhotoSrc}
															alt={`${doctor.user.lastName} ${doctor.user.firstName}`}
															className="w-12 h-12 rounded-full object-cover border-2 border-slate-300"
															onError={(e) => {
																const target = e.target as HTMLImageElement;
																target.style.display = "none";
																const placeholder = document.createElement("div");
																placeholder.className = "w-12 h-12 rounded-full bg-slate-200 flex items-center justify-center border-2 border-slate-300";
																placeholder.innerHTML = '<svg class="h-6 w-6 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>';
																target.parentElement?.insertBefore(placeholder, target);
															}}
														/>
													) : (
														<div className="w-12 h-12 rounded-full bg-slate-200 flex items-center justify-center border-2 border-slate-300">
															<User className="h-6 w-6 text-slate-400" />
														</div>
													)}
												</TableCell>
												<TableCell className="font-medium">
													{doctor.user.lastName}{" "}
													{doctor.user.firstName}{" "}
													{doctor.user.middleName}
												</TableCell>
												<TableCell>
													<div className="flex flex-wrap gap-1">
														{doctor.specializations.map((spec) => (
															<span
																key={spec.id}
																className="px-2 py-1 rounded-full text-xs border border-slate-700"
															>
																{spec.name}
															</span>
														))}
													</div>
												</TableCell>
												<TableCell>
													{doctor.user.email}
												</TableCell>
												<TableCell>
													{doctor.user.phone}
												</TableCell>
												<TableCell>
													{doctor.experienceYears} {doctor.experienceYears === 1 ? "год" : doctor.experienceYears < 5 ? "года" : "лет"}
												</TableCell>
												<TableCell>
													<div className="flex gap-2">
														<DropdownMenu>
															<DropdownMenuTrigger
																asChild
															>
																<Button
																	variant="ghost"
																	size="sm"
																	className="border-slate-700"
																>
																	<MoreVertical className="h-4 w-4" />
																</Button>
															</DropdownMenuTrigger>
															<DropdownMenuContent>
																<DropdownMenuItem
																	onClick={() => {
																		setSelectedDoctor(
																			doctor
																		);
																		setIsEditDialogOpen(
																			true
																		);
																	}}
																>
																	Изменить
																</DropdownMenuItem>
																<DropdownMenuItem
																	onClick={() => {
																		setSelectedDoctor(
																			doctor
																		);
																		setIsScheduleDialogOpen(
																			true
																		);
																	}}
																>
																	Расписание
																</DropdownMenuItem>
																<DropdownMenuItem
																	onClick={() => {
																		setSelectedDoctor(
																			doctor
																		);
																		setIsReviewsDialogOpen(
																			true
																		);
																	}}
																>
																	Отзывы
																</DropdownMenuItem>
																<DropdownMenuItem
																	className="text-red-500"
																	onClick={() => {
																		setSelectedDoctor(
																			doctor
																		);
																		setIsDeleteDialogOpen(
																			true
																		);
																	}}
																>
																	Удалить
																</DropdownMenuItem>
															</DropdownMenuContent>
														</DropdownMenu>
													</div>
												</TableCell>
											</TableRow>
											);
										})}
									{!isLoading &&
										filteredDoctors.length === 0 && (
											<TableRow>
												<TableCell
													colSpan={7}
													className="text-center"
												>
													Врачи не найдены
												</TableCell>
											</TableRow>
										)}
									{isLoading && (
										<TableRow>
											<TableCell
												colSpan={7}
												className="text-center"
											>
												Загрузка...
											</TableCell>
										</TableRow>
									)}
								</TableBody>
							</Table>
							
							{/* Пагинация */}
							{doctors.length > 0 && (
								<div className="flex items-center justify-between px-4 py-4 border-t border-slate-700">
									<div className="text-sm text-slate-400">
										Показано {filteredDoctors.length} врачей
									</div>
								</div>
							)}
						</CardContent>
					</Card>
				</div>
			</main>

			{selectedDoctor && (
				<>
					<EditDoctorDialog
						doctor={selectedDoctor}
						open={isEditDialogOpen}
						onOpenChange={() => {
							setIsEditDialogOpen(!isEditDialogOpen);
							setSelectedDoctor(null);
							fetchDoctors();
						}}
					/>
					<DoctorScheduleDialog
						doctorId={selectedDoctor.id.toString()}
						doctorName={`${selectedDoctor.user.lastName} ${
							selectedDoctor.user.firstName
						} ${selectedDoctor.user.middleName || ""}`}
						open={isScheduleDialogOpen}
						onOpenChange={setIsScheduleDialogOpen}
					/>
					<DoctorReviewsDialog
						doctorId={selectedDoctor.id}
						doctorName={`${selectedDoctor.user.lastName} ${
							selectedDoctor.user.firstName
						} ${selectedDoctor.user.middleName || ""}`}
						open={isReviewsDialogOpen}
						onOpenChange={setIsReviewsDialogOpen}
					/>
					<Dialog
						open={isDeleteDialogOpen}
						onOpenChange={setIsDeleteDialogOpen}
					>
						<DialogContent className="sm:max-w-[425px]">
							<DialogHeader>
								<DialogTitle>
									Подтверждение удаления
								</DialogTitle>
								<DialogDescription>
									Вы уверены, что хотите удалить врача{" "}
									{selectedDoctor.user.lastName}{" "}
									{selectedDoctor.user.firstName}{" "}
									{selectedDoctor.user.middleName}? Это действие
									нельзя отменить.
								</DialogDescription>
							</DialogHeader>
							<DialogFooter>
								<Button
									variant="outline"
									onClick={() => setIsDeleteDialogOpen(false)}
								>
									Отмена
								</Button>
								<Button
									variant="destructive"
									onClick={handleDelete}
								>
									Удалить
								</Button>
							</DialogFooter>
						</DialogContent>
					</Dialog>
				</>
			)}
		</div>
	);
}
