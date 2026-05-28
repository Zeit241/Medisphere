import { useTheme } from "next-themes";
import { Toaster } from "sonner";
import { useEffect, useState } from "react";

export function AppToaster() {
	const { resolvedTheme } = useTheme();
	const [mounted, setMounted] = useState(false);

	useEffect(() => {
		setMounted(true);
	}, []);

	if (!mounted) {
		return <Toaster richColors position="top-right" />;
	}

	return (
		<Toaster
			theme={resolvedTheme === "dark" ? "dark" : "light"}
			richColors
			position="top-right"
		/>
	);
}
