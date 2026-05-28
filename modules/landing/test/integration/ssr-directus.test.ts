vi.mock("next/cache", () => ({
  unstable_cache: <T>(fn: T) => fn,
}))

vi.mock("react", async (importOriginal) => {
  const mod = await importOriginal<typeof import("react")>()
  return {
    ...mod,
    cache: <T>(fn: T) => fn,
  }
})

import { http, HttpResponse } from "msw"
import { NextRequest } from "next/server"
import { renderToStaticMarkup } from "react-dom/server"
import { afterEach, describe, expect, it, vi } from "vitest"
import { GET as directusAssetGet } from "@/app/api/directus-asset/route"
import Home from "@/app/page"
import DoctorsPage from "@/app/doctors/page"
import ServicesPage from "@/app/services/page"
import {
  getLandingDoctors,
  getLandingReviews,
  getLandingServices,
  getMergedSiteSettings,
} from "@/lib/cms/directus"
import { DEFAULT_SETTINGS } from "@/lib/cms/merge-settings"
import { installDirectusAssetFetchFailure, installDirectusFetchFailure } from "../helpers/directus-fetch-mock"
import { server } from "../setup"

const BASE = "http://localhost:8055"
const FILE_UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"

function nextRequest (url: string) {
  return new NextRequest(url)
}

describe("Directus CMS fetch (MSW)", () => {
  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it("getMergedSiteSettings returns merged clinic name from Directus", async () => {
    const settings = await getMergedSiteSettings()
    expect(settings.clinicName).toBe("Клиника Тест")
    expect(settings.heroTitle).toBe("Тестовый заголовок")
  })

  it("getMergedSiteSettings falls back to defaults when Directus returns 503", async () => {
    installDirectusFetchFailure()
    const settings = await getMergedSiteSettings()
    expect(settings.clinicName).toBe(DEFAULT_SETTINGS.clinicName)
  })

  it("getLandingDoctors returns empty array on upstream failure", async () => {
    installDirectusFetchFailure()
    expect(await getLandingDoctors()).toEqual([])
  })

  it("getLandingDoctors maps doctor display name from MSW data", async () => {
    const doctors = await getLandingDoctors()
    expect(doctors).toHaveLength(1)
    expect(doctors[0].display_name).toContain("Петров")
    expect(doctors[0].specialties_display).toBe("Терапия")
  })

  it("getLandingServices returns empty array on failure", async () => {
    installDirectusFetchFailure()
    expect(await getLandingServices()).toEqual([])
  })

  it("getLandingServices maps service title from MSW data", async () => {
    const services = await getLandingServices()
    expect(services[0].title_display).toBe("Консультация")
    expect(services[0].operational_price).toBe(1500)
  })

  it("getLandingReviews returns empty array on failure", async () => {
    installDirectusFetchFailure()
    expect(await getLandingReviews()).toEqual([])
  })

  it("getLandingReviews maps patient and doctor names", async () => {
    const reviews = await getLandingReviews(5)
    expect(reviews[0].patient_display_name).toContain("Пациент")
    expect(reviews[0].doctor_display_name).toContain("Врачова")
    expect(reviews[0].review_text).toBe("Отлично")
  })
})

describe("SSR page render", () => {
  it("home page includes clinic name from CMS", async () => {
    const tree = await Home()
    const html = renderToStaticMarkup(tree)
    expect(html).toContain("Клиника Тест")
  })

  it("home page renders with default clinic when settings fail", async () => {
    installDirectusFetchFailure()
    const tree = await Home()
    const html = renderToStaticMarkup(tree)
    expect(html).toContain(DEFAULT_SETTINGS.clinicName)
  })

  it("doctors page renders mocked doctor name", async () => {
    const tree = await DoctorsPage()
    const html = renderToStaticMarkup(tree)
    expect(html).toContain("Петров")
  })

  it("services page renders mocked service title", async () => {
    const tree = await ServicesPage()
    const html = renderToStaticMarkup(tree)
    expect(html).toContain("Консультация")
  })
})

describe("directus-asset route", () => {
  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it("returns 400 for invalid file id", async () => {
    const res = await directusAssetGet(
      nextRequest("http://localhost/api/directus-asset?id=not-a-uuid")
    )
    expect(res.status).toBe(400)
  })

  it("returns 503 when Directus URL is not configured", async () => {
    vi.stubEnv("NEXT_PUBLIC_DIRECTUS_URL", "")
    vi.stubEnv("DIRECTUS_INTERNAL_URL", "")
    const res = await directusAssetGet(
      nextRequest(
        `http://localhost/api/directus-asset?id=${FILE_UUID}&format=webp`
      )
    )
    expect(res.status).toBe(503)
  })

  it("proxies asset bytes when upstream succeeds", async () => {
    vi.stubEnv("NEXT_PUBLIC_DIRECTUS_URL", BASE)
    const res = await directusAssetGet(
      nextRequest(
        `http://localhost/api/directus-asset?id=${FILE_UUID}&format=webp`
      )
    )
    expect(res.status).toBe(200)
    expect(res.headers.get("Content-Type")).toContain("image/jpeg")
  })

  it("returns 502 when upstream asset fetch fails", async () => {
    vi.stubEnv("NEXT_PUBLIC_DIRECTUS_URL", BASE)
    installDirectusAssetFetchFailure()
    const res = await directusAssetGet(
      nextRequest(
        `http://localhost/api/directus-asset?id=${FILE_UUID}&format=webp`
      )
    )
    expect(res.status).toBe(502)
  })
})
