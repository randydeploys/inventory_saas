import api from "@/lib/axios";
import type { ApiResponse, User } from "@/types/api";

export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: string;
}

export const userService = {
  getAll: () =>
    api.get<ApiResponse<User[]>>("/api/users"),

  create: (data: CreateUserRequest) =>
    api.post<ApiResponse<User>>("/api/users", data),

  updateRole: (id: string, role: string) =>
    api.put<ApiResponse<User>>(`/api/users/${id}/role`, { role }),

  deactivate: (id: string) =>
    api.put<ApiResponse<void>>(`/api/users/${id}/deactivate`),
};