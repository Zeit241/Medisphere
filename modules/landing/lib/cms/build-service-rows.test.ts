import { describe, expect, it } from "vitest"
import {
  buildServiceRows,
  specializationsUsedByServices,
} from "@/lib/cms/build-service-rows"
import type { VSiteServiceRow } from "@/lib/cms/types"

function row (overrides: Partial<VSiteServiceRow> = {}): VSiteServiceRow {
  return {
    service_id: 1,
    title_display: "Консультация",
    description_display: "Описание",
    operational_name: "Консультация",
    operational_price: 2000,
    duration_minutes: 45,
    specialization_ids: [1],
    specialization_chips: [{ id: 1, name: "Терапия" }],
    ...overrides,
  }
}

describe("buildServiceRows", () => {
  it("maps title and description from row", () => {
    const [vm] = buildServiceRows([row()])
    expect(vm.title).toBe("Консультация")
    expect(vm.desc).toBe("Описание")
  })

  it("falls back to operational_name for title", () => {
    const [vm] = buildServiceRows([
      row({ title_display: "", operational_name: "УЗИ" }),
    ])
    expect(vm.title).toBe("УЗИ")
  })

  it("uses service id fallback when title empty", () => {
    const [vm] = buildServiceRows([
      row({ service_id: 42, title_display: "", operational_name: "" }),
    ])
    expect(vm.title).toBe("Услуга #42")
  })

  it("formats numeric price in RUB", () => {
    const [vm] = buildServiceRows([row({ operational_price: 1500 })])
    expect(vm.priceLabel).toMatch(/1\s*500/)
    expect(vm.priceLabel).toContain("₽")
  })

  it("returns empty price label when price missing", () => {
    const [vm] = buildServiceRows([row({ operational_price: null })])
    expect(vm.priceLabel).toBe("")
  })

  it("returns Уточняется when duration missing", () => {
    const [vm] = buildServiceRows([row({ duration_minutes: null })])
    expect(vm.durationLabel).toBe("Уточняется")
  })

  it("formats duration in minutes", () => {
    const [vm] = buildServiceRows([row({ duration_minutes: 30 })])
    expect(vm.durationLabel).toBe("30 мин")
  })

  it("copies specialization ids and chips", () => {
    const [vm] = buildServiceRows([row()])
    expect(vm.specializationIds).toEqual([1])
    expect(vm.specializationChips).toEqual([{ id: 1, name: "Терапия" }])
  })
})

describe("specializationsUsedByServices", () => {
  it("returns specs referenced by services sorted by name", () => {
    const services = buildServiceRows([
      row({ specialization_ids: [2] }),
      row({ service_id: 2, specialization_ids: [1] }),
    ])
    const specs = specializationsUsedByServices(
      [
        { id: 2, name: "Хирургия" },
        { id: 1, name: "Анализы" },
      ],
      services
    )
    expect(specs.map((s) => s.id)).toEqual([1, 2])
  })

  it("returns empty array when no specialization ids used", () => {
    const services = buildServiceRows([row({ specialization_ids: [] })])
    expect(
      specializationsUsedByServices([{ id: 1, name: "A" }], services)
    ).toEqual([])
  })
})
