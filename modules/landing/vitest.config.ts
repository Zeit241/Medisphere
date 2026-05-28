import path from "node:path"
import { defineConfig } from "vitest/config"

export default defineConfig({
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "."),
      "next/cache": path.resolve(__dirname, "test/mocks/next-cache.ts"),
    },
  },
  test: {
    coverage: {
      provider: "v8",
      include: ["lib/cms/**"],
      exclude: ["lib/cms/types.ts"],
      thresholds: {
        lines: 75,
        functions: 75,
        branches: 50,
        statements: 75,
      },
    },
    projects: [
      {
        extends: true,
        test: {
          name: "unit",
          environment: "jsdom",
          setupFiles: ["./test/setup.ts"],
          include: ["lib/**/*.test.ts"],
        },
      },
      {
        extends: true,
        test: {
          name: "integration",
          environment: "node",
          setupFiles: ["./test/setup.ts"],
          include: ["test/integration/**/*.test.ts"],
        },
      },
    ],
  },
})
