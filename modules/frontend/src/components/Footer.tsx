export default function Footer() {
	return (
		<footer className="border-t border-border bg-muted/30 py-6">
			<div className="container mx-auto px-4 text-center text-sm text-muted-foreground">
				<p className="text-center">
					&copy; {new Date().getFullYear()} Клиника Менеджмент. Все
					права защищены.
				</p>
			</div>
		</footer>
	);
}
