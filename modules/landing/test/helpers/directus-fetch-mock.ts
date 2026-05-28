import { vi } from "vitest"
import {
  mockDoctorRaw,
  mockReviewRaw,
  mockServiceRaw,
  mockSiteSettings,
} from "../mocks/directus-handlers"

const BASE = "http://localhost:8055"

function jsonResponse(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json" },
  })
}

export function installDirectusFetchMock() {
  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
    const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url

    if (url.includes("/items/cms_site_settings")) {
      return jsonResponse({ data: [mockSiteSettings] })
    }
    if (url.includes("/items/doctors")) {
      return jsonResponse({ data: [mockDoctorRaw] })
    }
    if (url.includes("/items/doctor_specializations")) {
      return jsonResponse({
        data: [{ doctor_id: 10, specialization_id: { name: "Терапия" } }],
      })
    }
    if (url.includes("/items/services")) {
      return jsonResponse({ data: [mockServiceRaw] })
    }
    if (url.includes("/items/specialization_services")) {
      return jsonResponse({ data: [] })
    }
    if (url.includes("/items/specializations")) {
      return jsonResponse({ data: [{ id: 1, name: "Терапия", code: "THER" }] })
    }
    if (url.includes("/items/reviews")) {
      return jsonResponse({ data: [mockReviewRaw] })
    }
    if (url.includes("/assets/")) {
      return new Response(new Uint8Array([0xff, 0xd8, 0xff]), {
        status: 200,
        headers: { "Content-Type": "image/jpeg" },
      })
    }

    return jsonResponse({ data: [] }, 404)
  })
}

export function installDirectusFetchFailure() {
  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
    const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url
    if (url.startsWith(BASE)) {
      return new Response(null, { status: 503 })
    }
    return jsonResponse({ data: [] }, 404)
  })
}

export function installDirectusAssetFetchFailure() {
  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
    const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url
    if (url.includes("/assets/")) {
      return new Response(null, { status: 403 })
    }
    return jsonResponse({ data: [] }, 404)
  })
}
