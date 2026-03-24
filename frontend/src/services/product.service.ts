import api from "@/lib/axios";
import type { ApiResponse, Product, PaginatedResponse } from "@/types/api";

export interface ProductRequest {
  name: string;
  sku: string;
  description?: string;
  trackingType: "QUANTITY" | "UNIQUE";
  categoryId?: string;
  serialNumber?: string;
  minQuantity?: number;
  unit?: string;
}

export interface ProductFilters {
  page?: number;
  size?: number;
  search?: string;
  categoryId?: string;
  trackingType?: string;
  archived?: boolean;
}

export const productService = {
  getAll: (filters: ProductFilters = {}) =>
    api.get<ApiResponse<PaginatedResponse<Product>>>("/api/products", {
      params: {
        page: filters.page ?? 0,
        size: filters.size ?? 20,
        search: filters.search || undefined,
        categoryId: filters.categoryId || undefined,
        trackingType: filters.trackingType || undefined,
        archived: filters.archived ?? false,
      },
    }),

  getById: (id: string) =>
    api.get<ApiResponse<Product>>(`/api/products/${id}`),

  create: (data: ProductRequest) =>
    api.post<ApiResponse<Product>>("/api/products", data),

  update: (id: string, data: ProductRequest) =>
    api.put<ApiResponse<Product>>(`/api/products/${id}`, data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`/api/products/${id}`),

  getLowStock: () =>
    api.get<ApiResponse<Product[]>>("/api/products/low-stock"),
};