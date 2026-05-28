import { describe, expect, it } from "vitest"
import {
  cmsFileAssetUrl,
  doctorPortraitSrc,
  isDirectusProxiedImageSrc,
} from "@/lib/cms/images"

const BASE = "http://localhost:8055"
const UUID = "11111111-1111-1111-1111-111111111111"

describe("doctorPortraitSrc", () => {
  it("returns proxied URL for cms photo UUID", () => {
    const src = doctorPortraitSrc(
      { cms_photo_file_id: UUID, operational_photo_ref: null },
      BASE
    )
    expect(src).toBe(`/api/directus-asset?id=${UUID}&format=webp`)
  })

  it("returns absolute http URL as-is", () => {
    const url = "https://cdn.example/photo.jpg"
    const src = doctorPortraitSrc(
      { cms_photo_file_id: null, operational_photo_ref: url },
      BASE
    )
    expect(src).toBe(url)
  })
})

describe("cmsFileAssetUrl", () => {
  it("returns null for empty id and proxied URL for UUID", () => {
    expect(cmsFileAssetUrl(null)).toBeNull()
    expect(cmsFileAssetUrl(UUID)).toBe(
      `/api/directus-asset?id=${UUID}&format=webp`
    )
  })
})

describe("isDirectusProxiedImageSrc", () => {
  it("detects proxied and assets paths", () => {
    expect(isDirectusProxiedImageSrc("/api/directus-asset?id=x")).toBe(true)
    expect(isDirectusProxiedImageSrc("/assets/file")).toBe(true)
    expect(isDirectusProxiedImageSrc("/images/local.jpg")).toBe(false)
  })
})
