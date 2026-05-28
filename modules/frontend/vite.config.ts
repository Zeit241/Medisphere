import path from "path"
import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    // Обход CORS: браузер шлёт запросы на тот же origin (:5173), Vite проксирует на Directus.
    // В .env задайте VITE_DIRECTUS_URL=/__directus (или полный http://localhost:8055 + CORS в docker-compose).
    proxy: {
      "/__directus": {
        target: "http://127.0.0.1:8055",
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/__directus/, ""),
      },
    },
  },
})
