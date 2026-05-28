import { zodResolver } from "@hookform/resolvers/zod";
import { format } from "date-fns";
import { BarChart, FileSpreadsheet, FileText } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import {
	Bar,
	CartesianGrid,
	Legend,
	BarChart as RechartsBarChart,
	ResponsiveContainer,
	Tooltip,
	XAxis,
	YAxis,
} from "recharts";

import type { DailyReport, DailyReportAppointmentRow } from "@/api/types";
import {
	useGetDoctorsQuery,
	useLazyExportDailyExcelByDoctorQuery,
	useLazyExportDailyExcelQuery,
	useLazyExportDailyPdfByDoctorQuery,
	useLazyExportDailyPdfQuery,
	useLazyExportRangeExcelByDoctorQuery,
	useLazyExportRangeExcelQuery,
	useLazyExportRangePdfByDoctorQuery,
	useLazyExportRangePdfQuery,
	useLazyGetDailyReportByDoctorQuery,
	useLazyGetDailyReportQuery,
	useLazyGetRangeReportByDoctorQuery,
	useLazyGetRangeReportQuery,
} from "@/store/api/apiSlice";

import { DateRangePicker } from "@/components/date-range-picker";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
	Form,
	FormControl,
	FormField,
	FormItem,
	FormLabel,
} from "@/components/ui/form";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { toast } from "sonner";

import {
	reportFormSchema,
	type ReportFormValues,
} from "./report-form-schema";
import {
	getStatusLabel,
	prepareChartData,
	type ChartDataItem,
} from "./statistics-chart";

export default function StatisticsPage() {
	const { data: doctors = [] } = useGetDoctorsQuery();
	const [fetchDailyReport] = useLazyGetDailyReportQuery();
	const [fetchDailyReportByDoctor] = useLazyGetDailyReportByDoctorQuery();
	const [fetchRangeReport] = useLazyGetRangeReportQuery();
	const [fetchRangeReportByDoctor] = useLazyGetRangeReportByDoctorQuery();
	const [exportDailyExcel] = useLazyExportDailyExcelQuery();
	const [exportDailyExcelByDoctor] = useLazyExportDailyExcelByDoctorQuery();
	const [exportRangeExcel] = useLazyExportRangeExcelQuery();
	const [exportRangeExcelByDoctor] = useLazyExportRangeExcelByDoctorQuery();
	const [exportDailyPdf] = useLazyExportDailyPdfQuery();
	const [exportDailyPdfByDoctor] = useLazyExportDailyPdfByDoctorQuery();
	const [exportRangePdf] = useLazyExportRangePdfQuery();
	const [exportRangePdfByDoctor] = useLazyExportRangePdfByDoctorQuery();

	const [isGeneratingReport, setIsGeneratingReport] = useState(false);
	const [reportData, setReportData] = useState<DailyReport | null>(null);
	const [isLoading, setIsLoading] = useState(false);
	const [chartData, setChartData] = useState<ChartDataItem[]>([]);

	const form = useForm<ReportFormValues>({
		resolver: zodResolver(reportFormSchema),
		defaultValues: {
			reportType: "appointments",
			statisticsType: "all",
			filterType: "none",
			filterValue: "all",
			doctorId: "all",
			dateRange: {
				from: new Date(),
				to: new Date(),
			},
		},
	});

	// Получаем доступные типы статистики
	const getStatisticsTypes = () => {
		return [
			{ value: "all", label: "Вся статистика" },
			{ value: "status", label: "По статусам" },
			{ value: "doctors", label: "По врачам" },
		];
	};

	// Получаем доступные типы фильтров
	const getFilterTypes = () => {
		return [
			{ value: "none", label: "Без фильтра" },
			{ value: "doctor", label: "По врачу" },
		];
	};

	// Получаем доступные значения фильтра
	const getFilterValues = (filterType: string) => {
		switch (filterType) {
			case "doctor":
				return doctors.map((doctor) => {
					const specializationName = doctor.specializations?.[0]?.name || "";
					const doctorName = doctor.displayName || `${doctor.user?.lastName || ""} ${doctor.user?.firstName || ""}`.trim();
					return {
						value: doctor.id.toString(),
						label: specializationName ? `${doctorName} - ${specializationName}` : doctorName,
					};
				});
			default:
				return [];
		}
	};

	const fetchData = async (values: ReportFormValues) => {
		setIsLoading(true);
		try {
			const startDate = format(values.dateRange.from, "yyyy-MM-dd");
			const endDate = format(values.dateRange.to, "yyyy-MM-dd");
			
			// Определяем, нужно ли использовать отчет за день или за период
			const isSingleDay = startDate === endDate;
			
			// Определяем doctorId из фильтра или из отдельного поля
			let doctorId: number | null = null;
			if (values.filterType === "doctor" && values.filterValue && values.filterValue !== "all") {
				doctorId = parseInt(values.filterValue);
			} else if (values.doctorId && values.doctorId !== "all") {
				doctorId = parseInt(values.doctorId);
			}

			let report: DailyReport;

			if (isSingleDay) {
				// Отчет за один день
				if (doctorId) {
					report = await fetchDailyReportByDoctor({ doctorId, date: startDate }).unwrap();
				} else {
					report = await fetchDailyReport(startDate).unwrap();
				}
			} else {
				// Отчет за период
				if (doctorId) {
					report = await fetchRangeReportByDoctor({
						doctorId,
						startDate,
						endDate,
					}).unwrap();
				} else {
					report = await fetchRangeReport({ startDate, endDate }).unwrap();
				}
			}

			setReportData(report);

			// Подготовка данных для графика
			const nextChart = prepareChartData(
				report.appointments ?? [],
				values.statisticsType || "all"
			);
			setChartData(nextChart);
		} catch (error: unknown) {
			console.error("Ошибка при загрузке данных:", error);
			const errorMessage =
				error && typeof error === "object" && "data" in error
					? String((error as { data?: { error?: string } }).data?.error)
					: "Не удалось загрузить данные";
			toast.error(errorMessage || "Не удалось загрузить данные");
		} finally {
			setIsLoading(false);
		}
	};

	const downloadFile = (blob: Blob, filename: string) => {
		const url = window.URL.createObjectURL(blob);
		const link = document.createElement("a");
		link.href = url;
		link.download = filename;
		document.body.appendChild(link);
		link.click();
		document.body.removeChild(link);
		window.URL.revokeObjectURL(url);
	};

	const generateExcelReport = async () => {
		if (!reportData) return;

		setIsGeneratingReport(true);
		try {
			const values = form.getValues();
			const startDate = format(values.dateRange.from, "yyyy-MM-dd");
			const endDate = format(values.dateRange.to, "yyyy-MM-dd");
			const isSingleDay = startDate === endDate;
			
			// Определяем doctorId из фильтра или из отдельного поля
			let doctorId: number | null = null;
			if (values.filterType === "doctor" && values.filterValue && values.filterValue !== "all") {
				doctorId = parseInt(values.filterValue);
			} else if (values.doctorId && values.doctorId !== "all") {
				doctorId = parseInt(values.doctorId);
			}

			let blob: Blob;
			let filename: string;

			if (isSingleDay) {
				if (doctorId) {
					blob = await exportDailyExcelByDoctor({ doctorId, date: startDate }).unwrap();
					filename = `report_doctor${doctorId}_${startDate}.xlsx`;
				} else {
					blob = await exportDailyExcel(startDate).unwrap();
					filename = `report_${startDate}.xlsx`;
				}
			} else {
				if (doctorId) {
					blob = await exportRangeExcelByDoctor({
						doctorId,
						startDate,
						endDate,
					}).unwrap();
					filename = `report_doctor${doctorId}_${startDate}_${endDate}.xlsx`;
				} else {
					blob = await exportRangeExcel({ startDate, endDate }).unwrap();
					filename = `report_${startDate}_${endDate}.xlsx`;
				}
			}

			downloadFile(blob, filename);
			toast.success("Отчет Excel успешно скачан");
		} catch (error: unknown) {
			console.error("Ошибка при генерации Excel отчета:", error);
			toast.error("Не удалось сгенерировать отчет");
		} finally {
			setIsGeneratingReport(false);
		}
	};

	const generatePdfReport = async () => {
		if (!reportData) return;

		setIsGeneratingReport(true);
		try {
			const values = form.getValues();
			const startDate = format(values.dateRange.from, "yyyy-MM-dd");
			const endDate = format(values.dateRange.to, "yyyy-MM-dd");
			const isSingleDay = startDate === endDate;
			
			// Определяем doctorId из фильтра или из отдельного поля
			let doctorId: number | null = null;
			if (values.filterType === "doctor" && values.filterValue && values.filterValue !== "all") {
				doctorId = parseInt(values.filterValue);
			} else if (values.doctorId && values.doctorId !== "all") {
				doctorId = parseInt(values.doctorId);
			}

			let blob: Blob;
			let filename: string;

			if (isSingleDay) {
				if (doctorId) {
					blob = await exportDailyPdfByDoctor({ doctorId, date: startDate }).unwrap();
					filename = `report_doctor${doctorId}_${startDate}.pdf`;
				} else {
					blob = await exportDailyPdf(startDate).unwrap();
					filename = `report_${startDate}.pdf`;
				}
			} else {
				if (doctorId) {
					blob = await exportRangePdfByDoctor({
						doctorId,
						startDate,
						endDate,
					}).unwrap();
					filename = `report_doctor${doctorId}_${startDate}_${endDate}.pdf`;
				} else {
					blob = await exportRangePdf({ startDate, endDate }).unwrap();
					filename = `report_${startDate}_${endDate}.pdf`;
				}
			}

			downloadFile(blob, filename);
			toast.success("Отчет PDF успешно скачан");
		} catch (error: unknown) {
			console.error("Ошибка при генерации PDF отчета:", error);
			toast.error("Не удалось сгенерировать отчет");
		} finally {
			setIsGeneratingReport(false);
		}
	};

	return (
		<div className="flex flex-1 flex-col gradient-bg">
			<main className="py-8">
				<div className="container mx-auto px-4">
					<div className="mb-8 fade-in">
						<h2 className="text-3xl font-bold gradient-heading">
							Статистика и отчеты
						</h2>
						<p className="mt-2 text-slate-600">
							Аналитика работы клиники, формирование отчетов и
							отправка уведомлений
						</p>
					</div>

					<Form {...form}>
						<form
							onSubmit={form.handleSubmit(fetchData)}
							className="space-y-4"
						>
							<Card className="gradient-card">
								<CardHeader>
									<CardTitle>Параметры отчета</CardTitle>
								</CardHeader>
								<CardContent className="space-y-4">
									<div className="grid grid-cols-1 md:grid-cols-3 gap-4">
										<FormField
											control={form.control}
											name="statisticsType"
											render={({ field }) => (
												<FormItem>
													<FormLabel>
														Тип статистики
													</FormLabel>
													<Select
														onValueChange={
															field.onChange
														}
														defaultValue={
															field.value
														}
													>
														<FormControl>
															<SelectTrigger>
																<SelectValue placeholder="Выберите тип статистики" />
															</SelectTrigger>
														</FormControl>
														<SelectContent>
															{getStatisticsTypes().map((type) => (
																<SelectItem
																	key={
																		type.value
																	}
																	value={
																		type.value
																	}
																>
																	{type.label}
																</SelectItem>
															))}
														</SelectContent>
													</Select>
												</FormItem>
											)}
										/>

										<FormField
											control={form.control}
											name="filterType"
											render={({ field }) => (
												<FormItem>
													<FormLabel>
														Фильтр
													</FormLabel>
													<Select
														onValueChange={(
															value
														) => {
															field.onChange(
																value
															);
															form.setValue(
																"filterValue",
																"all"
															);
														}}
														defaultValue={
															field.value
														}
													>
														<FormControl>
															<SelectTrigger>
																<SelectValue placeholder="Выберите тип фильтра" />
															</SelectTrigger>
														</FormControl>
														<SelectContent>
															{getFilterTypes().map((type) => (
																<SelectItem
																	key={
																		type.value
																	}
																	value={
																		type.value
																	}
																>
																	{type.label}
																</SelectItem>
															))}
														</SelectContent>
													</Select>
												</FormItem>
											)}
										/>

										{form.getValues("filterType") !==
											"none" && (
											<FormField
												control={form.control}
												name="filterValue"
												render={({ field }) => (
													<FormItem>
														<FormLabel>
															Значение фильтра
														</FormLabel>
														<Select
															onValueChange={
																field.onChange
															}
															defaultValue={
																field.value
															}
														>
															<FormControl>
																<SelectTrigger>
																	<SelectValue placeholder="Выберите значение" />
																</SelectTrigger>
															</FormControl>
															<SelectContent>
																{getFilterValues(
																	form.getValues(
																		"filterType"
																	) as string
																).map(
																	(value) => (
																		<SelectItem
																			key={
																				value.value
																			}
																			value={
																				value.value
																			}
																		>
																			{
																				value.label
																			}
																		</SelectItem>
																	)
																)}
															</SelectContent>
														</Select>
													</FormItem>
												)}
											/>
										)}

										<FormField
											control={form.control}
											name="doctorId"
											render={({ field }) => (
												<FormItem>
													<FormLabel>
														Врач (опционально)
													</FormLabel>
													<Select
														onValueChange={
															field.onChange
														}
														defaultValue={
															field.value
														}
													>
														<FormControl>
															<SelectTrigger>
																<SelectValue placeholder="Выберите врача" />
															</SelectTrigger>
														</FormControl>
														<SelectContent>
															<SelectItem value="all">
																Все врачи
															</SelectItem>
															{doctors.map(
																(doctor) => {
																	const specializationName = doctor.specializations?.[0]?.name || "";
																	const doctorName = doctor.displayName || `${doctor.user?.lastName || ""} ${doctor.user?.firstName || ""}`.trim();
																	return (
																		<SelectItem
																			key={doctor.id}
																			value={doctor.id.toString()}
																		>
																			{specializationName ? `${doctorName} - ${specializationName}` : doctorName}
																		</SelectItem>
																	);
																}
															)}
														</SelectContent>
													</Select>
												</FormItem>
											)}
										/>

										<FormField
											control={form.control}
											name="dateRange"
											render={({ field }) => (
												<FormItem>
													<FormLabel>
														Период
													</FormLabel>
													<DateRangePicker
														value={field.value}
														onChange={
															field.onChange
														}
													/>
												</FormItem>
											)}
										/>
									</div>

									<div className="flex justify-end space-x-2">
										<Button
											type="submit"
											disabled={isLoading}
										>
											{isLoading
												? "Загрузка..."
												: "Показать данные"}
										</Button>
									</div>
								</CardContent>
							</Card>
						</form>
					</Form>

					{/* Статистика */}
					{reportData && (
						<Card className="mt-8 gradient-card">
							<CardHeader>
								<CardTitle>Сводная статистика</CardTitle>
							</CardHeader>
							<CardContent>
								<div className="grid grid-cols-2 md:grid-cols-5 gap-4">
									<div>
										<p className="text-sm text-muted-foreground">Всего записей</p>
										<p className="text-2xl font-bold">{reportData.totalAppointments ?? 0}</p>
									</div>
									<div>
										<p className="text-sm text-muted-foreground">Запланировано</p>
										<p className="text-2xl font-bold">{reportData.scheduledCount ?? 0}</p>
									</div>
									<div>
										<p className="text-sm text-muted-foreground">Завершено</p>
										<p className="text-2xl font-bold text-green-600">{reportData.completedCount ?? 0}</p>
									</div>
									<div>
										<p className="text-sm text-muted-foreground">Отменено</p>
										<p className="text-2xl font-bold text-red-600">{reportData.cancelledCount ?? 0}</p>
									</div>
									<div>
										<p className="text-sm text-muted-foreground">Неявки</p>
										<p className="text-2xl font-bold text-orange-600">{reportData.noShowCount ?? 0}</p>
									</div>
								</div>
							</CardContent>
						</Card>
					)}

					{/* График */}
					{chartData.length > 0 && (
						<Card className="mt-8 gradient-card">
							<CardHeader>
								<CardTitle className="flex items-center gap-2">
									<BarChart className="h-5 w-5 text-blue-600" />
									Статистика
								</CardTitle>
							</CardHeader>
							<CardContent>
								<div className="h-[400px]">
									<ResponsiveContainer
										width="100%"
										height="100%"
									>
										<RechartsBarChart data={chartData}>
											<CartesianGrid strokeDasharray="3 3" />
											<XAxis 
												dataKey="name" 
												angle={-45}
												textAnchor="end"
												height={100}
											/>
											<YAxis />
											<Tooltip />
											<Legend />
											<Bar
												dataKey="count"
												fill="#3b82f6"
												name="Количество"
											/>
										</RechartsBarChart>
									</ResponsiveContainer>
								</div>
							</CardContent>
						</Card>
					)}

					{/* Таблица данных */}
					{reportData && (reportData.appointments?.length ?? 0) > 0 && (
						<Card className="mt-8">
							<CardHeader>
								<div className="flex items-center justify-between">
									<CardTitle>Детали записей</CardTitle>
									<div className="flex space-x-2">
										<Button
											variant="outline"
											size="sm"
											onClick={generateExcelReport}
											disabled={isGeneratingReport}
										>
											<FileSpreadsheet className="mr-2 h-4 w-4" />
											Excel
										</Button>
										<Button
											variant="outline"
											size="sm"
											onClick={generatePdfReport}
											disabled={isGeneratingReport}
										>
											<FileText className="mr-2 h-4 w-4" />
											PDF
										</Button>
									</div>
								</div>
							</CardHeader>
							<CardContent>
								<div className="rounded-md border">
									<div className="overflow-x-auto">
										<table className="w-full">
											<thead>
												<tr className="border-b">
													<th className="p-2 text-left">Дата и время</th>
													<th className="p-2 text-left">Врач</th>
													<th className="p-2 text-left">Пациент</th>
													<th className="p-2 text-left">Телефон</th>
													<th className="p-2 text-left">Кабинет</th>
													<th className="p-2 text-left">Статус</th>
													<th className="p-2 text-left">Диагноз</th>
												</tr>
											</thead>
											<tbody>
												{(reportData.appointments ?? []).map(
													(appointment: DailyReportAppointmentRow) => (
														<tr
															key={appointment.appointmentId ?? appointment.startTime}
															className="border-b hover:bg-slate-50"
														>
															<td className="p-2">
																{appointment.startTime
																	? format(
																			new Date(appointment.startTime),
																			"dd.MM.yyyy HH:mm"
																		)
																	: "—"}
															</td>
															<td className="p-2">
																{appointment.doctorDisplayName}
															</td>
															<td className="p-2">
																{appointment.patientFullName}
															</td>
															<td className="p-2">
																{appointment.patientPhone || "-"}
															</td>
															<td className="p-2">
																{appointment.roomNumber || "-"}
															</td>
															<td className="p-2">
																<span
																	className={`px-2 py-1 rounded text-xs ${
																		appointment.status === "completed"
																			? "bg-green-100 text-green-800"
																			: appointment.status === "cancelled"
																			? "bg-red-100 text-red-800"
																			: appointment.status === "no_show"
																			? "bg-orange-100 text-orange-800"
																			: "bg-blue-100 text-blue-800"
																	}`}
																>
																	{getStatusLabel(appointment.status ?? "")}
																</span>
															</td>
															<td className="p-2">
																{typeof appointment.diagnosis === "object" &&
																appointment.diagnosis != null &&
																"code" in appointment.diagnosis
																	? `${(appointment.diagnosis as { code?: string; name?: string }).code ?? ""} ${(appointment.diagnosis as { name?: string }).name ?? ""}`.trim() || "-"
																	: (appointment.diagnosis as string) || "-"}
															</td>
														</tr>
													)
												)}
											</tbody>
										</table>
									</div>
								</div>
							</CardContent>
						</Card>
					)}
				</div>
			</main>
		</div>
	);
}
