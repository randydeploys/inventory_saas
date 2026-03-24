import api from "@/lib/axios";
import type { ApiResponse, AuthResponse } from "@/types/api";

export const authService = {
  login: (email: string, password: string) =>
    api.post<ApiResponse<AuthResponse>>("/api/auth/login", { email, password }),

  register: (data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    tenantName: string;
  }) => api.post<ApiResponse<AuthResponse>>("/api/auth/register", data),

  refresh: () => api.post<ApiResponse<void>>("/api/auth/refresh"),

  logout: () => api.post<ApiResponse<void>>("/api/auth/logout"),
  me: () => api.get<ApiResponse<AuthResponse>>("/api/auth/me"),
};