/// <reference types="vite/client" />

interface ImportMetaEnv {
	/** Загрузка файлов: /__directus (proxy) или http://localhost:8055 */
	readonly VITE_DIRECTUS_URL?: string
	/** Превью img: только абсолютный URL Directus, например http://localhost:8055 */
	readonly VITE_DIRECTUS_PUBLIC_URL?: string
	readonly VITE_DIRECTUS_STATIC_TOKEN?: string
}

interface ImportMeta {
	readonly env: ImportMetaEnv
}
