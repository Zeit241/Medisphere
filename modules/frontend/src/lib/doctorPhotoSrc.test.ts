import { afterEach, describe, expect, it, vi } from "vitest";

import { doctorPhotoImgSrc } from "./doctorPhotoSrc";

describe("doctorPhotoImgSrc", () => {
	afterEach(() => {
		vi.unstubAllEnvs();
	});

	it("returns null for empty input", () => {
		expect(doctorPhotoImgSrc(null)).toBeNull();
		expect(doctorPhotoImgSrc("  ")).toBeNull();
	});

	it("passes through data URLs unchanged", () => {
		const dataUrl = "data:image/png;base64,abc";
		expect(doctorPhotoImgSrc(dataUrl)).toBe(dataUrl);
	});

	it("builds Directus asset URL from UUID and handles other formats", () => {
		vi.stubEnv("VITE_DIRECTUS_PUBLIC_URL", "http://directus.test");
		vi.stubEnv("VITE_DIRECTUS_STATIC_TOKEN", "static-token");
		const uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
		expect(doctorPhotoImgSrc(uuid)).toContain(`/assets/${uuid}?format=webp`);
		expect(doctorPhotoImgSrc(uuid)).toContain("access_token=static-token");
		expect(doctorPhotoImgSrc("https://cdn.test/assets/photo.jpg")).toContain("access_token=");
		expect(doctorPhotoImgSrc("https://cdn.test/assets/x?access_token=already")).toBe(
			"https://cdn.test/assets/x?access_token=already"
		);
		expect(doctorPhotoImgSrc("iVBORw0KGgoABC")).toBe("data:image/png;base64,iVBORw0KGgoABC");
		expect(doctorPhotoImgSrc("plainbase64")).toMatch(/^data:image\/jpeg;base64,/);
	});

	it("wraps raw base64 JPEG prefix as data URL", () => {
		const raw = "/9j/abc123";
		expect(doctorPhotoImgSrc(raw)).toBe("data:image/jpeg;base64,/9j/abc123");
	});
});
