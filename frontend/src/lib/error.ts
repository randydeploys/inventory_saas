import type { AxiosError } from "axios";
import type { ApiResponse } from "@/types/api";

export function getErrorMessage(err: unknown): string {
  const error = err as AxiosError<ApiResponse<unknown>>;
  
  if (error.response?.data?.errors?.length) {
    return error.response.data.errors.join(", ");
  }
  
  if (error.response?.data?.message) {
    return error.response.data.message;
  }
  
  return "Une erreur est survenue";
}