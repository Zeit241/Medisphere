import axios from "axios";

export const api = axios.create({
	baseURL: import.meta.env.VITE_API_URL || "http://localhost:8085/api",
	headers: {
		"Content-Type": "application/json",
	},
});

// Добавляем перехватчик для добавления токена
api.interceptors.request.use((config) => {
	const token = localStorage.getItem("token");
	if (token) {
		config.headers.Authorization = `Bearer ${token}`;
	}
	return config;
});

// Добавляем перехватчик для обработки ошибок
api.interceptors.response.use(
	(response) => response,
	(error) => {
		if (error.response?.status === 401) {
			// Обработка ошибки авторизации
			localStorage.removeItem("token");
			localStorage.removeItem("user");
			window.location.href = "/login";
		}
		return Promise.reject(error);
	}
);
