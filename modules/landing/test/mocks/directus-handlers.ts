import { http, HttpResponse } from "msw"

const BASE = "http://localhost:8055"

export const mockSiteSettings = {
  id: 1,
  clinic_name: "Клиника Тест",
  phone: "+7 (999) 111-22-33",
  hero_title: "Тестовый заголовок",
}

export const mockDoctorRaw = {
  id: 10,
  bio: "Биография",
  experience_years: 5,
  photo: "22222222-2222-2222-2222-222222222222",
  user_id: { first_name: "Иван", last_name: "Петров", middle_name: "Сергеевич" },
}

export const mockServiceRaw = {
  id: 20,
  name: "Консультация",
  description: "Первичный приём",
  price: 1500,
  duration_minutes: 30,
  code: "CONS",
}

export const mockReviewRaw = {
  id: 30,
  rating: 5,
  review_text: "Отлично",
  created_at: "2025-01-15T10:00:00.000Z",
  doctor_id: {
    id: 10,
    user_id: { first_name: "Анна", last_name: "Врачова" },
  },
  patient_id: {
    id: 1,
    user_id: { first_name: "Пётр", last_name: "Пациентов" },
  },
}

export const directusHandlers = [
  http.get(`${BASE}/items/cms_site_settings`, () =>
    HttpResponse.json({ data: [mockSiteSettings] })
  ),
  http.get(`${BASE}/items/doctors`, () =>
    HttpResponse.json({ data: [mockDoctorRaw] })
  ),
  http.get(`${BASE}/items/doctor_specializations`, () =>
    HttpResponse.json({
      data: [
        {
          doctor_id: 10,
          specialization_id: { name: "Терапия" },
        },
      ],
    })
  ),
  http.get(`${BASE}/items/services`, () =>
    HttpResponse.json({ data: [mockServiceRaw] })
  ),
  http.get(`${BASE}/items/specialization_services`, () =>
    HttpResponse.json({ data: [] })
  ),
  http.get(`${BASE}/items/specializations`, () =>
    HttpResponse.json({
      data: [{ id: 1, name: "Терапия", code: "THER" }],
    })
  ),
  http.get(`${BASE}/items/reviews`, () =>
    HttpResponse.json({ data: [mockReviewRaw] })
  ),
  http.get(`${BASE}/assets/:id`, () =>
    new HttpResponse(new Uint8Array([0xff, 0xd8, 0xff]), {
      status: 200,
      headers: { "Content-Type": "image/jpeg" },
    })
  ),
]
