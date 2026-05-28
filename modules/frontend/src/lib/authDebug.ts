/** Включено только в dev: логи в консоль браузера (F12). Удалите вызовы после починки. */
const ON = import.meta.env.DEV;

export function authDebug(...args: unknown[]): void {
	if (ON) {
		console.debug("[auth-debug]", ...args);
	}
}
