import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { tenantService } from "@/services/tenant.service";
import type { TenantRequest } from "@/services/tenant.service";

export function useTenant() {
  return useQuery({
    queryKey: ["tenant"],
    queryFn: () => tenantService.get().then((res) => res.data.data),
  });
}

export function useUpdateTenant() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: TenantRequest) => tenantService.update(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tenant"] });
    },
  });
}
