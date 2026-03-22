import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { movementService } from "@/services/movement.service";
import type { MovementRequest, MovementFilters } from "@/services/movement.service";

export function useMovements(filters: MovementFilters = {}) {
  return useQuery({
    queryKey: ["movements", filters],
    queryFn: () => movementService.getAll(filters).then((res) => res.data.data),
  });
}

export function useMovementsByProduct(productId: string, page = 0) {
  return useQuery({
    queryKey: ["movements", "product", productId, page],
    queryFn: () =>
      movementService.getByProduct(productId, page).then((res) => res.data.data),
    enabled: !!productId,
  });
}

export function useCreateMovement() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: MovementRequest) => movementService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["movements"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
    },
  });
}