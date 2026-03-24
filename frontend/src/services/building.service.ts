import api from "@/lib/axios";
import type { ApiResponse, Building } from "@/types/api";

export interface BuildingRequest {
  name: string;
  address?: string;
}

export const buildingService = {
  getAll: (archived = false) =>
    api.get<ApiResponse<Building[]>>("/api/buildings", {
      params: { archived },
    }),

  getById: (id: string) =>
    api.get<ApiResponse<Building>>(`/api/buildings/${id}`),

  create: (data: BuildingRequest) =>
    api.post<ApiResponse<Building>>("/api/buildings", data),

  update: (id: string, data: BuildingRequest) =>
    api.put<ApiResponse<Building>>(`/api/buildings/${id}`, data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`/api/buildings/${id}`),

  reassign: (id: string, targetRoomId: string) =>
    api.post<ApiResponse<void>>(`/api/buildings/${id}/reassign`, { targetRoomId }),
};