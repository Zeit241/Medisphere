import type { Specialization } from "@/api/types";
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
	useCreateSpecializationMutation,
	useDeleteSpecializationMutation,
	useGetSpecializationsQuery,
	useUpdateSpecializationMutation,
} from "@/store/api/apiSlice";
import { Pencil, PlusCircle, Tags, Trash2 } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

export default function AdminCategoriesPage() {
	const { data: specializations = [], isLoading, refetch } = useGetSpecializationsQuery();
	const [createSpec] = useCreateSpecializationMutation();
	const [updateSpec] = useUpdateSpecializationMutation();
	const [deleteSpec] = useDeleteSpecializationMutation();

	const [search, setSearch] = useState("");
	const filtered = useMemo(() => {
		const q = search.trim().toLowerCase();
		if (!q) return specializations;
		return specializations.filter(
			(s) =>
				s.name.toLowerCase().includes(q) ||
				s.code.toLowerCase().includes(q) ||
				(s.description || "").toLowerCase().includes(q)
		);
	}, [search, specializations]);

	const [dialogOpen, setDialogOpen] = useState(false);
	const [editing, setEditing] = useState<Specialization | null>(null);
	const [code, setCode] = useState("");
	const [name, setName] = useState("");
	const [description, setDescription] = useState("");

	const [deleteTarget, setDeleteTarget] = useState<Specialization | null>(null);

	const openCreate = () => {
		setEditing(null);
		setCode("");
		setName("");
		setDescription("");
		setDialogOpen(true);
	};

	const openEdit = (s: Specialization) => {
		setEditing(s);
		setCode(s.code);
		setName(s.name);
		setDescription(s.description || "");
		setDialogOpen(true);
	};

	const handleSave = async () => {
		if (!code.trim() || !name.trim()) {
			toast.error("Заполните код и название");
			return;
		}
		try {
			if (editing) {
				await updateSpec({
					id: editing.id,
					body: {
						code: code.trim(),
						name: name.trim(),
						description: description.trim(),
					},
				}).unwrap();
				toast.success("Категория обновлена");
			} else {
				await createSpec({
					code: code.trim(),
					name: name.trim(),
					description: description.trim(),
				}).unwrap();
				toast.success("Категория создана");
			}
			setDialogOpen(false);
			void refetch();
		} catch {
			toast.error("Не удалось сохранить категорию");
		}
	};

	const handleDelete = async () => {
		if (!deleteTarget) return;
		try {
			await deleteSpec(deleteTarget.id).unwrap();
			toast.success("Категория удалена");
			setDeleteTarget(null);
			void refetch();
		} catch {
			toast.error(
				"Не удалось удалить категорию (возможно, она используется врачами или услугами)"
			);
		}
	};

	return (
		<div className="flex-1 flex flex-col gradient-bg">
			<main className="py-8">
				<div className="container mx-auto px-4">
					<div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
						<div>
							<h2 className="text-3xl font-bold gradient-heading flex items-center gap-2">
								<Tags className="h-8 w-8" />
								Категории услуг
							</h2>
							<p className="mt-1 text-sm text-muted-foreground">
								Специализации клиники — в записи на приём они отображаются как категории
								услуг
							</p>
						</div>
						<Button className="gradient-button" onClick={openCreate}>
							<PlusCircle className="mr-2 h-4 w-4" />
							Добавить категорию
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
								placeholder="Код, название или описание"
								className="border-slate-700"
							/>
						</CardContent>
					</Card>

					<Card className="gradient-card">
						<CardContent className="p-0">
							<Table>
								<TableHeader>
									<TableRow>
										<TableHead>Код</TableHead>
										<TableHead>Название</TableHead>
										<TableHead className="hidden md:table-cell">Описание</TableHead>
										<TableHead className="w-[120px]">Действия</TableHead>
									</TableRow>
								</TableHeader>
								<TableBody>
									{isLoading ? (
										<TableRow>
											<TableCell colSpan={4} className="text-center py-8">
												Загрузка...
											</TableCell>
										</TableRow>
									) : filtered.length === 0 ? (
										<TableRow>
											<TableCell colSpan={4} className="text-center py-8 text-muted-foreground">
												Нет категорий
											</TableCell>
										</TableRow>
									) : (
										filtered.map((s) => (
											<TableRow key={s.id}>
												<TableCell className="font-mono text-sm">{s.code}</TableCell>
												<TableCell className="font-medium">{s.name}</TableCell>
												<TableCell className="hidden md:table-cell max-w-md truncate text-muted-foreground">
													{s.description || "—"}
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
				<DialogContent className="sm:max-w-md">
					<DialogHeader>
						<DialogTitle>
							{editing ? "Редактировать категорию" : "Новая категория"}
						</DialogTitle>
					</DialogHeader>
					<div className="grid gap-4 py-2">
						<div className="grid gap-2">
							<Label htmlFor="cat-code">Код</Label>
							<Input
								id="cat-code"
								value={code}
								onChange={(e) => setCode(e.target.value)}
								placeholder="например, THERAPY"
								className="border-slate-700"
							/>
						</div>
						<div className="grid gap-2">
							<Label htmlFor="cat-name">Название</Label>
							<Input
								id="cat-name"
								value={name}
								onChange={(e) => setName(e.target.value)}
								placeholder="Отображаемое название"
								className="border-slate-700"
							/>
						</div>
						<div className="grid gap-2">
							<Label htmlFor="cat-desc">Описание</Label>
							<Textarea
								id="cat-desc"
								value={description}
								onChange={(e) => setDescription(e.target.value)}
								rows={3}
								className="border-slate-700 resize-none"
							/>
						</div>
					</div>
					<DialogFooter>
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
						<AlertDialogTitle>Удалить категорию?</AlertDialogTitle>
						<AlertDialogDescription>
							{deleteTarget
								? `«${deleteTarget.name}» будет удалена. Это действие необратимо, если база данных не запрещает удаление из‑за связей.`
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
