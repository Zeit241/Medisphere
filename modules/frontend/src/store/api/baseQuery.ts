import type { BaseQueryFn, FetchArgs, FetchBaseQueryError } from "@reduxjs/toolkit/query";
import { fetchBaseQuery } from "@reduxjs/toolkit/query/react";

import { API_BASE, isAuthPath } from "./utils";

const rawBaseQuery = fetchBaseQuery({
	baseUrl: API_BASE,
	prepareHeaders: (headers) => {
		const token = localStorage.getItem("token");
		if (token) headers.set("Authorization", `Bearer ${token}`);
		headers.set("Content-Type", "application/json");
		return headers;
	},
});

export const baseQueryWithReauth: BaseQueryFn<
	string | FetchArgs,
	unknown,
	FetchBaseQueryError
> = async (args, api, extraOptions) => {
	const result = await rawBaseQuery(args, api, extraOptions);
	if (result.error && result.error.status === 401) {
		const url = typeof args === "string" ? args : args.url;
		if (url && !isAuthPath(url)) {
			localStorage.removeItem("token");
			localStorage.removeItem("user");
			window.location.href = "/auth/login";
		}
	}
	return result;
};
