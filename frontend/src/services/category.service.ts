import api from "@/lib/axios";
import type { ApiResponse, Category } from "@/types/api";

export interface CategoryRequest {
  name: string;
  color: string;
}

export const categoryService = {
  getAll: (archived = false) =>
    api.get<ApiResponse<Category[]>>("/api/categories", {
      params: { archived },
    }),

  getById: (id: string) =>
    api.get<ApiResponse<Category>>(`/api/categories/${id}`),

  create: (data: CategoryRequest) =>
    api.post<ApiResponse<Category>>("/api/categories", data),

  update: (id: string, data: CategoryRequest) =>
    api.put<ApiResponse<Category>>(`/api/categories/${id}`, data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`/api/categories/${id}`),
};