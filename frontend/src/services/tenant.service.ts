import api from "@/lib/axios";
import type { ApiResponse, Tenant } from "@/types/api";

export interface TenantRequest {
  name: string;
  slug: string;
}

export const tenantService = {
  get: () => api.get<ApiResponse<Tenant>>("/api/tenant"),
  update: (data: TenantRequest) => api.put<ApiResponse<Tenant>>("/api/tenant", data),
};
