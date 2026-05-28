import type { ClinicService } from "@/api/types";
import {
	AlertDialog,
	AlertDialogAction,
	AlertDialogCancel,
	AlertDialogContent,
	AlertDialogDescription,
	AlertDialogFooter,
	AlertDialogHeader,
	AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import {
	Dialog,
	DialogContent,
	DialogFooter,
	DialogHeader,
	DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import {
	useCreateServiceMutation,
	useDeleteServiceMutation,
	useGetServicesQuery,
	useGetSpecializationsQuery,
	useSetServiceSpecializationsMutation,
	useUpdateServiceMutation,
} from "@/store/api/apiSlice";
import { BriefcaseMedical, Pencil, PlusCircle, Stethoscope, Trash2 } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

function formatPrice(s: ClinicService): string {
	if (s.price == null) return "—";
	const n = typeof s.price === "number" ? s.price : Number(String(s.price).replace(",", "."));
	if (!Number.isFinite(n)) return "—";
	return new Intl.NumberFormat("ru-RU", {
		style: "currency",
		currency: "RUB",
		maximumFractionDigits: 2,
	}).format(n);
}

function categoryLine(s: ClinicService): string {
	const names = s.specializationNames?.filter(Boolean) ?? [];
	if (names.length === 0) return "Без категории";
	return [...names].sort((a, b) => a.localeCompare(b, "ru")).join(", ");
}

export default function AdminServicesPage() {
	const { data: services = [], isLoading, refetch } = useGetServicesQuery();
	const { data: specializations = [], isLoading: loadingSpecs } =
		useGetSpecializationsQuery();
	const [createService] = useCreateServiceMutation();
	const [updateService] = useUpdateServiceMutation();
	const [deleteService] = useDeleteServiceMutation();
	const [setServiceSpecializations] = useSetServiceSpecializationsMutation();

	const [search, setSearch] = useState("");
	const filtered = useMemo(() => {
		const q = search.trim().toLowerCase();
		if (!q) return services;
		return services.filter(
			(s) =>
				s.name.toLowerCase().includes(q) ||
				(s.code || "").toLowerCase().includes(q) ||
				categoryLine(s).toLowerCase().includes(q)
		);
	}, [search, services]);

	const [dialogOpen, setDialogOpen] = useState(false);
	const [editing, setEditing] = useState<ClinicService | null>(null);
	const [name, setName] = useState("");
	const [code, setCode] = useState("");
	const [price, setPrice] = useState("");
	const [durationMinutes, setDurationMinutes] = useState("30");
	const [description, setDescription] = useState("");
	const [selectedSpecIds, setSelectedSpecIds] = useState<number[]>([]);

	const [deleteTarget, setDeleteTarget] = useState<ClinicService | null>(null);

	const openCreate = () => {
		setEditing(null);
		setName("");
		setCode("");
		setPrice("");
		setDurationMinutes("30");
		setDescription("");
		setSelectedSpecIds([]);
		setDialogOpen(true);
	};

	const openEdit = (s: ClinicService) => {
		setEditing(s);
		setName(s.name);
		setCode(s.code || "");
		if (s.price != null) {
			const n =
				typeof s.price === "number" ? s.price : Number(String(s.price).replace(",", "."));
			setPrice(Number.isFinite(n) ? String(n) : "");
		} else setPrice("");
		setDurationMinutes(String(s.durationMinutes ?? 30));
		setDescription(s.description || "");
		setSelectedSpecIds([...(s.specializationIds ?? [])]);
		setDialogOpen(true);
	};

	const toggleSpec = (id: number, checked: boolean) => {
		setSelectedSpecIds((prev) =>
			checked ? [...prev, id] : prev.filter((x) => x !== id)
		);
	};

	const handleSave = async () => {
		if (!name.trim()) {
			toast.error("Укажите название услуги");
			return;
		}
		const priceNum = price.trim() === "" ? 0 : Number(price.replace(",", "."));
		if (!Number.isFinite(priceNum) || priceNum < 0) {
			toast.error("Некорректная цена");
			return;
		}
		const dur = Number(durationMinutes);
		if (!Number.isFinite(dur) || dur < 1) {
			toast.error("Укажите длительность в минутах (целое число ≥ 1)");
			return;
		}

		try {
			const specIds = [...new Set(selectedSpecIds)];
			if (editing) {
				await updateService({
					id: editing.id,
					body: {
						name: name.trim(),
						code: code.trim() || null,
						price: priceNum,
						durationMinutes: Math.round(dur),
						description: description.trim() || null,
					},
				}).unwrap();
				await setServiceSpecializations({
					id: editing.id,
					body: { specializationIds: specIds },
				}).unwrap();
				toast.success("Услуга обновлена");
			} else {
				const created = await createService({
					name: name.trim(),
					code: code.trim() || null,
					price: priceNum,
					durationMinutes: Math.round(dur),
					description: description.trim() || null,
				}).unwrap();
				await setServiceSpecializations({
					id: created.id,
					body: { specializationIds: specIds },
				}).unwrap();
				toast.success("Услуга создана");
			}
			setDialogOpen(false);
			void refetch();
		} catch {
			toast.error("Не удалось сохранить услугу или привязки к категориям");
		}
	};

	const handleDelete = async () => {
		if (!deleteTarget) return;
		try {
			await deleteService(deleteTarget.id).unwrap();
			toast.success("Услуга удалена");
			setDeleteTarget(null);
			void refetch();
		} catch {
			toast.error("Не удалось удалить услугу");
		}
	};

	return (
		<div className="flex-1 flex flex-col gradient-bg">
			<main className="py-8">
				<div className="container mx-auto px-4">
					<div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
						<div>
							<h2 className="text-3xl font-bold gradient-heading flex items-center gap-2">
								<BriefcaseMedical className="h-8 w-8" />
								Услуги
							</h2>
							<p className="mt-1 text-sm text-muted-foreground">
								Справочник услуг и привязка к категориям (специализациям)
							</p>
						</div>
						<Button className="gradient-button" onClick={openCreate}>
							<PlusCircle className="mr-2 h-4 w-4" />
							Добавить услугу
						</Button>
					</div>

					<Card className="mb-6 gradient-card">
						<CardHeader>
							<CardTitle>Поиск</CardTitle>
						</CardHeader>
						<CardContent>
							<Input
								value={search}
								onChange={(e) => setSearch(e.target.value)}
								placeholder="Название, код или категория"
								className="border-slate-700"
							/>
						</CardContent>
					</Card>

					<Card className="gradient-card">
						<CardContent className="p-0">
							<Table>
								<TableHeader>
									<TableRow>
										<TableHead>Название</TableHead>
										<TableHead className="hidden sm:table-cell">Код</TableHead>
										<TableHead>Цена</TableHead>
										<TableHead className="hidden md:table-cell">Мин.</TableHead>
										<TableHead className="hidden lg:table-cell">Категории</TableHead>
										<TableHead className="w-[100px]">Действия</TableHead>
									</TableRow>
								</TableHeader>
								<TableBody>
									{isLoading ? (
										<TableRow>
											<TableCell colSpan={6} className="text-center py-8">
												Загрузка...
											</TableCell>
										</TableRow>
									) : filtered.length === 0 ? (
										<TableRow>
											<TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
												Нет услуг
											</TableCell>
										</TableRow>
									) : (
										filtered.map((s) => (
											<TableRow key={s.id}>
												<TableCell className="font-medium">{s.name}</TableCell>
												<TableCell className="hidden sm:table-cell font-mono text-sm">
													{s.code || "—"}
												</TableCell>
												<TableCell>{formatPrice(s)}</TableCell>
												<TableCell className="hidden md:table-cell">
													{s.durationMinutes ?? "—"}
												</TableCell>
												<TableCell className="hidden lg:table-cell max-w-xs text-muted-foreground text-sm">
													{categoryLine(s)}
												</TableCell>
												<TableCell>
													<div className="flex gap-1">
														<Button
															variant="ghost"
															size="icon"
															className="h-8 w-8"
															onClick={() => openEdit(s)}
															aria-label="Редактировать"
														>
															<Pencil className="h-4 w-4" />
														</Button>
														<Button
															variant="ghost"
															size="icon"
															className="h-8 w-8 text-destructive"
															onClick={() => setDeleteTarget(s)}
															aria-label="Удалить"
														>
															<Trash2 className="h-4 w-4" />
														</Button>
													</div>
												</TableCell>
											</TableRow>
										))
									)}
								</TableBody>
							</Table>
						</CardContent>
					</Card>
				</div>
			</main>

			<Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
				<DialogContent className="sm:max-w-lg max-h-[90vh] flex flex-col">
					<DialogHeader>
						<DialogTitle>
							{editing ? "Редактировать услугу" : "Новая услуга"}
						</DialogTitle>
					</DialogHeader>
					<div className="grid gap-4 py-2 overflow-y-auto pr-1">
						<div className="grid gap-2">
							<Label htmlFor="svc-name">Название</Label>
							<Input
								id="svc-name"
								value={name}
								onChange={(e) => setName(e.target.value)}
								className="border-slate-700"
							/>
						</div>
						<div className="grid gap-2">
							<Label htmlFor="svc-code">Код (необязательно)</Label>
							<Input
								id="svc-code"
								value={code}
								onChange={(e) => setCode(e.target.value)}
								className="border-slate-700"
							/>
						</div>
						<div className="grid grid-cols-2 gap-3">
							<div className="grid gap-2">
								<Label htmlFor="svc-price">Цена (₽)</Label>
								<Input
									id="svc-price"
									type="number"
									min={0}
									step={0.01}
									value={price}
									onChange={(e) => setPrice(e.target.value)}
									className="border-slate-700"
								/>
							</div>
							<div className="grid gap-2">
								<Label htmlFor="svc-dur">Длительность (мин)</Label>
								<Input
									id="svc-dur"
									type="number"
									min={1}
									step={1}
									value={durationMinutes}
									onChange={(e) => setDurationMinutes(e.target.value)}
									className="border-slate-700"
								/>
							</div>
						</div>
						<div className="grid gap-2">
							<Label htmlFor="svc-desc">Описание</Label>
							<Textarea
								id="svc-desc"
								value={description}
								onChange={(e) => setDescription(e.target.value)}
								rows={2}
								className="border-slate-700 resize-none"
							/>
						</div>
						<div className="grid gap-2">
							<Label className="flex items-center gap-2">
								<Stethoscope className="h-4 w-4" />
								Категории (специализации)
							</Label>
							<div className="rounded-md border border-slate-700 p-3 max-h-48 overflow-y-auto space-y-2">
								{loadingSpecs ? (
									<p className="text-sm text-muted-foreground">Загрузка...</p>
								) : specializations.length === 0 ? (
									<p className="text-sm text-muted-foreground">
										Сначала создайте категории на странице «Категории услуг»
									</p>
								) : (
									specializations.map((sp) => (
										<label
											key={sp.id}
											className="flex items-center gap-2 text-sm cursor-pointer"
										>
											<Checkbox
												checked={selectedSpecIds.includes(sp.id)}
												onCheckedChange={(c) => toggleSpec(sp.id, c === true)}
											/>
											<span>{sp.name}</span>
											<span className="text-muted-foreground text-xs font-mono">
												{sp.code}
											</span>
										</label>
									))
								)}
							</div>
						</div>
					</div>
					<DialogFooter className="gap-2 sm:gap-0">
						<Button variant="outline" onClick={() => setDialogOpen(false)}>
							Отмена
						</Button>
						<Button className="gradient-button" onClick={() => void handleSave()}>
							Сохранить
						</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>

			<AlertDialog open={!!deleteTarget} onOpenChange={(o) => !o && setDeleteTarget(null)}>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Удалить услугу?</AlertDialogTitle>
						<AlertDialogDescription>
							{deleteTarget
								? `«${deleteTarget.name}» будет удалена вместе с привязками к категориям.`
								: null}
						</AlertDialogDescription>
					</AlertDialogHeader>
					<AlertDialogFooter>
						<AlertDialogCancel>Отмена</AlertDialogCancel>
						<AlertDialogAction onClick={() => void handleDelete()}>
							Удалить
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>
		</div>
	);
}
