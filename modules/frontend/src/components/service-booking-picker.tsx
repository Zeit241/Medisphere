import { Check, LayoutGrid, List, Search, Stethoscope } from "lucide-react";
import { useMemo, useState } from "react";

import type { ClinicService } from "@/api/types";
import { formatClinicServicePrice, parseClinicPrice } from "@/lib/format-clinic-service-price";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { cn } from "@/lib/utils";

const UNCATEGORIZED = "Без категории";

function categoryLabel(s: ClinicService): string {
	const names = s.specializationNames?.filter(Boolean) ?? [];
	if (names.length === 0) return UNCATEGORIZED;
	return [...names].sort((a, b) => a.localeCompare(b, "ru")).join(", ");
}

function parsePrice(s: ClinicService): number {
	const n = parseClinicPrice(s.price);
	return Number.isFinite(n) ? n : 0;
}

function formatPrice(s: ClinicService): string {
	return formatClinicServicePrice(s);
}

export type ServiceBookingPickerView = "list" | "cards";

export interface ServiceBookingPickerProps {
	services: ClinicService[];
	value: string;
	onChange: (serviceId: string) => void;
	disabled?: boolean;
	className?: string;
}

export function ServiceBookingPicker({
	services,
	value,
	onChange,
	disabled,
	className,
}: ServiceBookingPickerProps) {
	const [searchQuery, setSearchQuery] = useState("");
	const [categoryFilter, setCategoryFilter] = useState<string>("__all__");
	const [sortBy, setSortBy] = useState<"name" | "price" | "category">("name");
	const [sortDir, setSortDir] = useState<"asc" | "desc">("asc");
	const [view, setView] = useState<ServiceBookingPickerView>("cards");

	const categories = useMemo(() => {
		const set = new Set<string>();
		for (const s of services) {
			set.add(categoryLabel(s));
		}
		return [...set].sort((a, b) => a.localeCompare(b, "ru"));
	}, [services]);

	const filteredSorted = useMemo(() => {
		const q = searchQuery.trim().toLowerCase();
		let list = services.filter((s) => {
			if (categoryFilter !== "__all__" && categoryLabel(s) !== categoryFilter) {
				return false;
			}
			if (!q) return true;
			const name = (s.name ?? "").toLowerCase();
			const code = (s.code ?? "").toLowerCase();
			return name.includes(q) || code.includes(q);
		});

		const dir = sortDir === "asc" ? 1 : -1;
		list = [...list].sort((a, b) => {
			if (sortBy === "name") {
				return dir * (a.name ?? "").localeCompare(b.name ?? "", "ru", { sensitivity: "base" });
			}
			if (sortBy === "price") {
				return dir * (parsePrice(a) - parsePrice(b));
			}
			return dir * categoryLabel(a).localeCompare(categoryLabel(b), "ru", { sensitivity: "base" });
		});
		return list;
	}, [services, searchQuery, categoryFilter, sortBy, sortDir]);

	return (
		<div className={cn("space-y-4", className)}>
			<div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-end">
				<div className="flex-1 min-w-[200px] space-y-1.5">
					<Label className="text-xs text-muted-foreground">Поиск</Label>
					<div className="relative">
						<Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
						<Input
							className="light-input hover-scale pl-9"
							placeholder="Название или код услуги"
							value={searchQuery}
							onChange={(e) => setSearchQuery(e.target.value)}
							disabled={disabled}
						/>
					</div>
				</div>
				<div className="w-full sm:w-[200px] space-y-1.5">
					<Label className="text-xs text-muted-foreground">Категория</Label>
					<Select
						value={categoryFilter}
						onValueChange={setCategoryFilter}
						disabled={disabled}
					>
						<SelectTrigger className="light-input hover-scale">
							<SelectValue placeholder="Все" />
						</SelectTrigger>
						<SelectContent>
							<SelectItem value="__all__">Все категории</SelectItem>
							{categories.map((c) => (
								<SelectItem key={c} value={c}>
									{c}
								</SelectItem>
							))}
						</SelectContent>
					</Select>
				</div>
				<div className="flex flex-wrap gap-2 sm:gap-3">
					<div className="w-full xs:w-auto min-w-[140px] space-y-1.5">
						<Label className="text-xs text-muted-foreground">Сортировка</Label>
						<Select
							value={sortBy}
							onValueChange={(v) => setSortBy(v as typeof sortBy)}
							disabled={disabled}
						>
							<SelectTrigger className="light-input hover-scale">
								<SelectValue />
							</SelectTrigger>
							<SelectContent>
								<SelectItem value="name">По названию</SelectItem>
								<SelectItem value="price">По цене</SelectItem>
								<SelectItem value="category">По категории</SelectItem>
							</SelectContent>
						</Select>
					</div>
					<div className="w-full xs:w-auto min-w-[120px] space-y-1.5">
						<Label className="text-xs text-muted-foreground">Порядок</Label>
						<Select
							value={sortDir}
							onValueChange={(v) => setSortDir(v as typeof sortDir)}
							disabled={disabled}
						>
							<SelectTrigger className="light-input hover-scale">
								<SelectValue />
							</SelectTrigger>
							<SelectContent>
								<SelectItem value="asc">По возрастанию</SelectItem>
								<SelectItem value="desc">По убыванию</SelectItem>
							</SelectContent>
						</Select>
					</div>
				</div>
				<div className="space-y-1.5">
					<Label className="text-xs text-muted-foreground">Вид</Label>
					<ToggleGroup
						type="single"
						value={view}
						onValueChange={(v) => v && setView(v as ServiceBookingPickerView)}
						disabled={disabled}
						className="justify-start"
					>
						<ToggleGroupItem value="list" aria-label="Список" className="gap-1.5 px-3">
							<List className="size-4" />
							Список
						</ToggleGroupItem>
						<ToggleGroupItem value="cards" aria-label="Карточки" className="gap-1.5 px-3">
							<LayoutGrid className="size-4" />
							Карточки
						</ToggleGroupItem>
					</ToggleGroup>
				</div>
			</div>

			{filteredSorted.length === 0 ? (
				<div className="text-sm text-muted-foreground border rounded-md p-4 text-center">
					Нет услуг по заданным фильтрам
				</div>
			) : view === "list" ? (
				<ul className="divide-y rounded-md border border-slate-200 bg-white max-h-[min(360px,50vh)] overflow-y-auto">
					{filteredSorted.map((s) => {
						const idStr = String(s.id);
						const selected = value === idStr;
						return (
							<li key={s.id}>
								<button
									type="button"
									disabled={disabled}
									onClick={() => onChange(idStr)}
									className={cn(
										"flex w-full items-start gap-3 px-3 py-3 text-left text-sm transition-colors hover:bg-slate-50",
										selected && "bg-blue-50"
									)}
								>
									<div className="mt-0.5 text-muted-foreground">
										<Stethoscope className="size-4" />
									</div>
									<div className="min-w-0 flex-1">
										<div className="font-medium text-slate-900">{s.name}</div>
										<div className="text-xs text-muted-foreground mt-0.5">
											{categoryLabel(s)}
											{s.durationMinutes != null ? ` · ${s.durationMinutes} мин` : ""}
										</div>
										<div className="text-xs text-slate-600 mt-1">{formatPrice(s)}</div>
									</div>
									{selected && (
										<div className="shrink-0 flex size-7 items-center justify-center rounded-full bg-blue-600 text-white">
											<Check className="size-4" />
										</div>
									)}
								</button>
							</li>
						);
					})}
				</ul>
			) : (
				<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 max-h-[min(420px,55vh)] overflow-y-auto pr-1">
					{filteredSorted.map((s) => {
						const idStr = String(s.id);
						const selected = value === idStr;
						return (
							<button
								key={s.id}
								type="button"
								disabled={disabled}
								onClick={() => onChange(idStr)}
								className={cn(
									"relative rounded-lg border-2 p-4 text-left transition-all hover:shadow-md",
									selected
										? "border-blue-500 bg-blue-50"
										: "border-slate-200 bg-white hover:border-blue-200"
								)}
							>
								<div className="flex items-start gap-2">
									<Stethoscope className="size-5 shrink-0 text-slate-400 mt-0.5" />
									<div className="min-w-0 flex-1">
										<h3 className="font-medium text-sm leading-snug">{s.name}</h3>
										<p className="text-xs text-muted-foreground mt-1 line-clamp-2">
											{categoryLabel(s)}
										</p>
										<div className="mt-2 flex flex-wrap gap-x-2 gap-y-1 text-xs text-slate-600">
											<span>{formatPrice(s)}</span>
											{s.durationMinutes != null && <span>{s.durationMinutes} мин</span>}
										</div>
									</div>
								</div>
								{selected && (
									<div className="absolute top-2 right-2 flex size-6 items-center justify-center rounded-full bg-blue-600 text-white">
										<Check className="size-3.5" />
									</div>
								)}
							</button>
						);
					})}
				</div>
			)}
		</div>
	);
}
