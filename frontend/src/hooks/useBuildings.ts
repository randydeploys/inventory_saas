import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { buildingService } from "@/services/building.service";
import type { BuildingRequest } from "@/services/building.service";

export function useBuildings(archived = false) {
  return useQuery({
    queryKey: ["buildings", { archived }],
    queryFn: () => buildingService.getAll(archived).then((res) => res.data.data),
  });
}

export function useBuilding(id: string) {
  return useQuery({
    queryKey: ["buildings", id],
    queryFn: () => buildingService.getById(id).then((res) => res.data.data),
    enabled: !!id,
  });
}

export function useCreateBuilding() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: BuildingRequest) => buildingService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["buildings"] });
    },
  });
}

export function useUpdateBuilding() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: BuildingRequest }) =>
      buildingService.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["buildings"] });
    },
  });
}

export function useDeleteBuilding() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => buildingService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["buildings"] });
    },
  });
}

export function useReassignBuilding() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, targetRoomId }: { id: string; targetRoomId: string }) =>
      buildingService.reassign(id, targetRoomId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["buildings"] });
    },
  });
}