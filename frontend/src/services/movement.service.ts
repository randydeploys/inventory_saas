import api from "@/lib/axios";
import type { ApiResponse, Movement, PaginatedResponse } from "@/types/api";

export interface MovementRequest {
  productId: string;
  type: "IN" | "OUT" | "TRANSFER";
  quantity: number;
  fromRoomId?: string;
  toRoomId?: string;
  reason?: string;
}

export interface MovementFilters {
  page?: number;
  size?: number;
  productId?: string;
  type?: string;
  fromDate?: string;
  toDate?: string;
}

export const movementService = {
  getAll: (filters: MovementFilters = {}) =>
    api.get<ApiResponse<PaginatedResponse<Movement>>>("/api/movements", {
      params: {
        page: filters.page ?? 0,
        size: filters.size ?? 20,
        productId: filters.productId || undefined,
        type: filters.type || undefined,
        fromDate: filters.fromDate || undefined,
        toDate: filters.toDate || undefined,
      },
    }),

  getByProduct: (productId: string, page = 0, size = 20) =>
    api.get<ApiResponse<PaginatedResponse<Movement>>>(
      `/api/products/${productId}/movements`,
      { params: { page, size } }
    ),

  create: (data: MovementRequest) =>
    api.post<ApiResponse<Movement>>("/api/movements", data),
};