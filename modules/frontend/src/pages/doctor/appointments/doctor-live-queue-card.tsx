import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Loader2 } from "lucide-react";

interface QueueEntry {
	patientId: number;
	position: number;
}

interface DoctorLiveQueueCardProps {
	queueLoading: boolean;
	liveQueue: QueueEntry[];
}

export function DoctorLiveQueueCard({
	queueLoading,
	liveQueue,
}: DoctorLiveQueueCardProps) {
	return (
		<Card>
			<CardHeader>
				<CardTitle>Живая очередь</CardTitle>
				<CardDescription>
					Redis (позиции обновляются после завершения приёма)
				</CardDescription>
			</CardHeader>
			<CardContent>
				{queueLoading ? (
					<Loader2 className="h-6 w-6 animate-spin text-slate-400" />
				) : liveQueue.length === 0 ? (
					<p className="text-sm text-slate-500">Очередь пуста</p>
				) : (
					<ol className="list-decimal space-y-2 pl-4 text-sm">
						{liveQueue
							.slice()
							.sort((x, y) => x.position - y.position)
							.map((entry) => (
								<li key={`${entry.patientId}-${entry.position}`}>
									Пациент #{entry.patientId}
									<span className="text-slate-500">
										{" "}
										(поз. {entry.position + 1})
									</span>
								</li>
							))}
					</ol>
				)}
			</CardContent>
		</Card>
	);
}
