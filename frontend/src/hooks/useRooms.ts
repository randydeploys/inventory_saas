import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { roomService } from "@/services/room.service";
import type { RoomRequest } from "@/services/room.service";

export function useRooms(buildingId: string, archived = false) {
  return useQuery({
    queryKey: ["rooms", buildingId, { archived }],
    queryFn: () =>
      roomService.getAllByBuilding(buildingId, archived).then((res) => res.data.data),
    enabled: !!buildingId,
  });
}

export function useCreateRoom() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ buildingId, data }: { buildingId: string; data: RoomRequest }) =>
      roomService.create(buildingId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["rooms"] });
    },
  });
}

export function useUpdateRoom() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RoomRequest }) =>
      roomService.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["rooms"] });
    },
  });
}

export function useDeleteRoom() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => roomService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["rooms"] });
    },
  });
}

export function useReassignRoom() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, targetRoomId }: { id: string; targetRoomId: string }) =>
      roomService.reassign(id, targetRoomId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["rooms"] });
    },
  });
}