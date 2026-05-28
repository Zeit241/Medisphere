const DIRECTUS_FILE_UUID =
	/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/** Для /assets/ без заголовка Authorization Directus отдаёт 403 без прав у Public — добавляем access_token из .env (dev-админка). */
function appendDirectusAssetReadToken (url: string): string {
	const token = import.meta.env.VITE_DIRECTUS_STATIC_TOKEN?.trim()
	if (!token || !url.includes("/assets/")) return url
	if (/[?&]access_token=/.test(url)) return url
	const sep = url.includes("?") ? "&" : "?"
	return `${url}${sep}access_token=${encodeURIComponent(token)}`
}

/**
 * URL для <img src>: ответ API (http/https), data URL, UUID + Directus base, либо сырой base64 (устар.).
 */
export function doctorPhotoImgSrc (
	photo: string | null | undefined,
	directusBaseUrl?: string,
): string | null {
	if (photo == null || !photo.trim()) return null
	const p = photo.trim()
	if (p.startsWith("data:image")) {
		return p
	}
	if (p.startsWith("http://") || p.startsWith("https://")) {
		return appendDirectusAssetReadToken(p)
	}
	const base =
		directusBaseUrl?.replace(/\/$/, "") ||
		import.meta.env.VITE_DIRECTUS_PUBLIC_URL?.replace(/\/$/, "") ||
		""
	if (DIRECTUS_FILE_UUID.test(p) && base) {
		return appendDirectusAssetReadToken(`${base}/assets/${p}?format=webp`)
	}
	if (p.startsWith("/9j/") || p.startsWith("iVBORw0KGgo")) {
		const mime = p.startsWith("/9j/") ? "image/jpeg" : "image/png"
		return `data:${mime};base64,${p}`
	}
	return `data:image/jpeg;base64,${p}`
}
