import { z } from "zod";

export const reportFormSchema = z.object({
	reportType: z.enum(["appointments"]),
	statisticsType: z.string().optional(),
	filterType: z.string().optional(),
	filterValue: z.string().optional(),
	doctorId: z.string().optional(),
	dateRange: z.object({
		from: z.date(),
		to: z.date(),
	}),
});

export type ReportFormValues = z.infer<typeof reportFormSchema>;
