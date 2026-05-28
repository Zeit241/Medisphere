import { afterAll, afterEach, beforeAll, beforeEach, vi } from "vitest"
import { setupServer } from "msw/node"
import { directusHandlers } from "./mocks/directus-handlers"
import { installDirectusFetchMock } from "./helpers/directus-fetch-mock"

export const server = setupServer(...directusHandlers)

let fetchMock: ReturnType<typeof installDirectusFetchMock> | undefined

beforeAll(() => {
  process.env.NEXT_PUBLIC_DIRECTUS_URL = "http://localhost:8055"
  process.env.DIRECTUS_TOKEN = "test-token"
  fetchMock = installDirectusFetchMock()
  server.listen({ onUnhandledRequest: "warn" })
})

beforeEach(() => {
  fetchMock?.mockRestore()
  fetchMock = installDirectusFetchMock()
})

afterEach(() => {
  server.resetHandlers()
})

afterAll(() => {
  fetchMock?.mockRestore()
  server.close()
})
