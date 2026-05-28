"use client";

import { format } from "date-fns";
import { ru } from "date-fns/locale";
import { CalendarIcon } from "lucide-react";
import { DateRange } from "react-day-picker";

import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
	Popover,
	PopoverContent,
	PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";

interface DateRangePickerProps {
	value?: DateRange;
	onChange?: (value: DateRange | undefined) => void;
	className?: string;
}

export function DateRangePicker({
	value,
	onChange,
	className,
}: DateRangePickerProps) {
	return (
		<div className={cn("grid gap-2", className)}>
			<Popover>
				<PopoverTrigger asChild>
					<Button
						id="date"
						variant={"outline"}
						className={cn(
							"w-[300px] justify-start text-left font-normal",
							!value && "text-muted-foreground"
						)}
					>
						<CalendarIcon className="mr-2 h-4 w-4" />
						{value?.from ? (
							value.to ? (
								<>
									{format(value.from, "LLL dd, y", {
										locale: ru,
									})}{" "}
									-{" "}
									{format(value.to, "LLL dd, y", {
										locale: ru,
									})}
								</>
							) : (
								format(value.from, "LLL dd, y", { locale: ru })
							)
						) : (
							<span>Выберите период</span>
						)}
					</Button>
				</PopoverTrigger>
				<PopoverContent className="w-auto p-0" align="start">
					<Calendar
						initialFocus
						mode="range"
						defaultMonth={value?.from}
						selected={value}
						onSelect={onChange}
						numberOfMonths={2}
						locale={ru}
					/>
				</PopoverContent>
			</Popover>
		</div>
	);
}
