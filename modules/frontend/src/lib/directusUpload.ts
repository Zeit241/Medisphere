function normalizeDirectusBase (): string {
	const raw = import.meta.env.VITE_DIRECTUS_URL?.trim() ?? ""
	return raw.replace(/\/$/, "")
}

/**
 * Загрузка файла в Directus Files; возвращает UUID для поля photo в API врача.
 * При VITE_DIRECTUS_URL=/__directus запросы идут через proxy Vite (без CORS).
 */
export async function uploadImageToDirectus (file: File): Promise<string> {
	const base = normalizeDirectusBase()
	const token = import.meta.env.VITE_DIRECTUS_STATIC_TOKEN?.trim()
	if (!base) {
		throw new Error("Не задан VITE_DIRECTUS_URL")
	}
	if (!token) {
		throw new Error("Не задан VITE_DIRECTUS_STATIC_TOKEN")
	}
	const body = new FormData()
	body.append("file", file)
	const res = await fetch(`${base}/files`, {
		method: "POST",
		headers: { Authorization: `Bearer ${token}` },
		body,
	})
	if (!res.ok) {
		const text = await res.text().catch(() => "")
		throw new Error(
			`Directus upload: ${res.status} ${res.statusText}${text ? ` — ${text.slice(0, 200)}` : ""}`,
		)
	}
	const json = (await res.json()) as { data?: { id?: string } }
	const id = json?.data?.id
	if (!id) {
		throw new Error("Directus: в ответе нет data.id")
	}
	return id
}

/**
 * URL для <img> превью. Запросы к /assets без Bearer — Directus даёт 403 без прав Public.
 * Поэтому: 1) явный публичный хост Directus; 2) при наличии токена — access_token в query (как в Directus REST).
 * Не используйте путь /__directus для img — только полный URL или задайте VITE_DIRECTUS_PUBLIC_URL.
 */
export function directusAssetPreviewUrl (
	fileId: string,
	directusBaseUrl?: string,
): string {
	const fromEnv =
		import.meta.env.VITE_DIRECTUS_PUBLIC_URL?.trim().replace(/\/$/, "") ||
		(() => {
			const u = import.meta.env.VITE_DIRECTUS_URL?.trim().replace(/\/$/, "") ?? ""
			if (!u || u.startsWith("/")) return ""
			return u
		})()
	const base = (directusBaseUrl ?? fromEnv).replace(/\/$/, "")
	if (!base) return ""
	const token = import.meta.env.VITE_DIRECTUS_STATIC_TOKEN?.trim()
	const qs = new URLSearchParams()
	qs.set("format", "webp")
	if (token) qs.set("access_token", token)
	return `${base}/assets/${fileId}?${qs.toString()}`
}
