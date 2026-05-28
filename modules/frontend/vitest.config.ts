import path from "path";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
	plugins: [react()],
	resolve: {
		alias: {
			"@": path.resolve(__dirname, "./src"),
		},
	},
	test: {
		coverage: {
			provider: "v8",
			include: ["src/lib/**", "src/store/api/**"],
			exclude: [
				"**/*.test.ts",
				"**/*.integration.test.ts",
				"src/test/**",
				"src/store/api/apiSlice.ts",
				"src/lib/authDebug.ts",
				"src/lib/directusUpload.ts",
				"src/lib/utils.ts",
			],
			thresholds: {
				lines: 80,
				functions: 80,
				branches: 80,
				statements: 80,
			},
			reporter: ["text", "text-summary"],
		},
		projects: [
			{
				extends: true,
				test: {
					name: "unit",
					include: ["src/**/*.test.ts"],
					exclude: ["src/**/*.integration.test.ts"],
					environment: "jsdom",
					setupFiles: ["./src/test/setup.ts"],
				},
			},
			{
				extends: true,
				test: {
					name: "integration",
					include: ["src/**/*.integration.test.ts"],
					environment: "jsdom",
					setupFiles: ["./src/test/setup.ts", "./src/test/integration-setup.ts"],
				},
			},
		],
	},
});
