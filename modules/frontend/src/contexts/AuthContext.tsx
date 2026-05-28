import React, { createContext, useContext, useEffect, useState } from "react";

import type {
	LoginCurrentUserPayload,
	LoginResponse,
	Patient,
	RegisterRequest,
	RegisterResponse,
	User,
} from "../api/types";
import { authDebug } from "@/lib/authDebug";
import { mergeUserAfterPatientUpdate, userFromLoginPayload } from "@/lib/mapLoginUser";
import { useLoginMutation, useRegisterMutation } from "@/store/api/apiSlice";

interface AuthContextType {
	user: User | null;
	isLoading: boolean;
	login: (email: string, password: string) => Promise<{ success: boolean; error?: string }>;
	register: (data: RegisterRequest) => Promise<{ success: boolean; error?: string }>;
	logout: () => void;
	/** Обновить пользователя в контексте и localStorage (после смены профиля и т.п.) */
	updateUser: (next: User) => void;
	/** Слияние после patientsApi.update */
	refreshUserFromPatient: (updatedPatient: Patient) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function rtkErrorMessage(error: unknown): string | undefined {
	if (!error || typeof error !== "object" || !("data" in error)) return undefined;
	const d = (error as { data?: unknown }).data;
	if (typeof d === "object" && d && "message" in d) {
		return String((d as { message?: string }).message);
	}
	return undefined;
}

/** Ответ регистрации (RegisterResponse) не содержит roleCode в data — только логин. */
function isLoginApiData(data: object): data is NonNullable<LoginResponse["data"]> {
	return "roleCode" in data && (data as { roleCode?: string }).roleCode != null;
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
	children,
}) => {
	const [loginMut] = useLoginMutation();
	const [registerMut] = useRegisterMutation();
	const [user, setUser] = useState<User | null>(null);
	const [isLoading, setIsLoading] = useState(true);

	useEffect(() => {
		const storedUser = localStorage.getItem("user");
		if (storedUser) {
			try {
				setUser(JSON.parse(storedUser));
			} catch {
				localStorage.removeItem("user");
			}
		}
		setIsLoading(false);
	}, []);

	const persistUser = (u: User) => {
		localStorage.setItem("user", JSON.stringify(u));
		setUser(u);
	};

	const updateUser = (next: User) => {
		persistUser(next);
	};

	const refreshUserFromPatient = (updatedPatient: Patient) => {
		setUser((prev) => {
			if (!prev) return prev;
			const next = mergeUserAfterPatientUpdate(prev, updatedPatient);
			localStorage.setItem("user", JSON.stringify(next));
			return next;
		});
	};

	const handleAuthResponse = (response: LoginResponse | RegisterResponse) => {
		const isSuccess = response.success === true || response.status === 200;
		if (!isSuccess || !response.data) {
			return;
		}

		const data = response.data;

		if (data.token) {
			localStorage.setItem("token", data.token);
		}

		authDebug("handleAuthResponse keys", Object.keys(data), "raw", data);

		// Логин: в data есть roleCode (у RegisterResponse его нет — не путать с регистрацией)
		const maybeUser = data.user;
		if (maybeUser && typeof maybeUser === "object" && isLoginApiData(data)) {
			const payload = maybeUser as LoginCurrentUserPayload;
			const mapped = userFromLoginPayload(payload, data.roleCode);
			authDebug("ветка логина", {
				roleCode: data.roleCode,
				rawUserKeys: Object.keys(payload),
				patientId: payload.patientId,
				doctorId: payload.doctorId,
				mappedDoctorId: mapped.doctorId,
				mappedRole: mapped.role,
			});
			persistUser(mapped);
			return;
		}

		// Регистрация: UserDto в data.user (без roleCode на уровне data)
		if (maybeUser && typeof maybeUser === "object" && "email" in maybeUser) {
			const ru = maybeUser as User & { roleCode?: string };
			authDebug("ветка регистрации (spread user)", { keys: Object.keys(ru) });
			persistUser({
				...ru,
				role: ru.role ?? ru.roleCode,
			});
			return;
		}

		if ("patient" in data && data.patient?.user) {
			const pu = data.patient.user;
			const role = pu.role ?? (pu as { roleCode?: string }).roleCode;
			persistUser({
				...pu,
				role,
				patientId: data.patient.id,
			});
			return;
		}

		// Запасной вариант: только email + roleCode (старый бэкенд)
		const emailOnly = (data as { email?: string }).email;
		const roleCode = (data as { roleCode?: string }).roleCode;
		if (emailOnly) {
			persistUser({
				id: 0,
				email: emailOnly,
				phone: "",
				firstName: "",
				lastName: "",
				middleName: "",
				createdAt: new Date().toISOString(),
				updatedAt: new Date().toISOString(),
				active: true,
				role: roleCode,
			});
		}
	};

	const login = async (email: string, password: string) => {
		try {
			const data = await loginMut({ email, password }).unwrap();
			const isSuccess = data.success === true || data.status === 200;
			if (isSuccess) {
				handleAuthResponse(data);
			}
			return { success: isSuccess };
		} catch (error: unknown) {
			const errorMessage =
				rtkErrorMessage(error) || "Ошибка при входе в систему";
			return { success: false, error: errorMessage };
		}
	};

	const register = async (data: RegisterRequest) => {
		try {
			const response = await registerMut(data).unwrap();
			const isSuccess = response.success === true || response.status === 200;
			if (isSuccess) {
				handleAuthResponse(response);
			}
			return { success: isSuccess };
		} catch (error: unknown) {
			const errorMessage = rtkErrorMessage(error) || "Ошибка при регистрации";
			return { success: false, error: errorMessage };
		}
	};

	const logout = () => {
		localStorage.removeItem("token");
		localStorage.removeItem("user");
		setUser(null);
	};

	return (
		<AuthContext.Provider
			value={{
				user,
				isLoading,
				login,
				register,
				logout,
				updateUser,
				refreshUserFromPatient,
			}}
		>
			{children}
		</AuthContext.Provider>
	);
};

export const useAuth = () => {
	const context = useContext(AuthContext);
	if (context === undefined) {
		throw new Error("useAuth must be used within an AuthProvider");
	}
	return context;
};
