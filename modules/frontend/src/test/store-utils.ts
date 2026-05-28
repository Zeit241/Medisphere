import { configureStore } from "@reduxjs/toolkit";

import { api } from "@/store/api/apiSlice";

export function setupApiStore() {
	const store = configureStore({
		reducer: {
			[api.reducerPath]: api.reducer,
		},
		middleware: (getDefaultMiddleware) =>
			getDefaultMiddleware().concat(api.middleware),
	});

	return { store, api };
}

export type TestStore = ReturnType<typeof setupApiStore>["store"];
