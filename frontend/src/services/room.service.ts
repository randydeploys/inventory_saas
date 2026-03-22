import api from "@/lib/axios";
import type { ApiResponse, Room } from "@/types/api";

export interface RoomRequest {
  name: string;
  description?: string;
}

export const roomService = {
  getAllByBuilding: (buildingId: string, archived = false) =>
    api.get<ApiResponse<Room[]>>(`/api/buildings/${buildingId}/rooms`, {
      params: { archived },
    }),

  getById: (id: string) =>
    api.get<ApiResponse<Room>>(`/api/rooms/${id}`),

  create: (buildingId: string, data: RoomRequest) =>
    api.post<ApiResponse<Room>>(`/api/buildings/${buildingId}/rooms`, data),

  update: (id: string, data: RoomRequest) =>
    api.put<ApiResponse<Room>>(`/api/rooms/${id}`, data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`/api/rooms/${id}`),
};