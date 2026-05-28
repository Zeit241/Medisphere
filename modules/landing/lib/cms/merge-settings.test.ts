import { describe, expect, it } from "vitest"
import {
  DEFAULT_SETTINGS,
  mapDoctorRow,
  mapReviewRow,
  mergeSiteSettings,
} from "@/lib/cms/merge-settings"

describe("mergeSiteSettings", () => {
  it("returns defaults when row is null", () => {
    const merged = mergeSiteSettings(null)
    expect(merged.clinicName).toBe(DEFAULT_SETTINGS.clinicName)
    expect(merged.homeFeatureBlocks).toHaveLength(3)
  })

  it("merges clinic name from row", () => {
    const merged = mergeSiteSettings({ clinic_name: "  Новая клиника  " })
    expect(merged.clinicName).toBe("Новая клиника")
  })

  it("uses social_links object when valid", () => {
    const merged = mergeSiteSettings({
      social_links: { vk: "https://vk.com/clinic" },
    })
    expect(merged.socialLinks).toEqual({ vk: "https://vk.com/clinic" })
  })

  it("falls back to empty social links for invalid value", () => {
    expect(mergeSiteSettings({ social_links: "bad" as never }).socialLinks).toEqual(
      {}
    )
  })

  it("normalizes section photo UUID string", () => {
    const uuid = "11111111-1111-1111-1111-111111111111"
    const merged = mergeSiteSettings({ section_1_photo: uuid })
    expect(merged.section1PhotoFileId).toBe(uuid)
  })

  it("normalizes section photo from object id", () => {
    const merged = mergeSiteSettings({
      section_2_photo: { id: "22222222-2222-2222-2222-222222222222" },
    })
    expect(merged.section2PhotoFileId).toBe(
      "22222222-2222-2222-2222-222222222222"
    )
  })

})

describe("mapDoctorRow", () => {
  it("builds display name and specialties", () => {
    const specMap = new Map([[10, "Терапия"]])
    const doc = mapDoctorRow(
      {
        id: 10,
        bio: "Bio",
        experience_years: 3,
        photo: "11111111-1111-1111-1111-111111111111",
        user_id: { first_name: "Иван", last_name: "Иванов" },
      },
      specMap
    )
    expect(doc.display_name).toBe("Иванов Иван")
    expect(doc.specialties_display).toBe("Терапия")
  })
})

describe("mapReviewRow", () => {
  it("maps patient and doctor display names", () => {
    const review = mapReviewRow({
      id: 1,
      rating: 4,
      review_text: "Хорошо",
      created_at: "2025-06-01T00:00:00.000Z",
      doctor_id: {
        user_id: { first_name: "Док", last_name: "Тор" },
      },
      patient_id: {
        user_id: { first_name: "Пациент" },
      },
    })
    expect(review.patient_display_name).toBe("Пациент")
    expect(review.doctor_display_name).toContain("Тор")
    expect(review.created_at).toBe("2025-06-01T00:00:00.000Z")
  })
})
