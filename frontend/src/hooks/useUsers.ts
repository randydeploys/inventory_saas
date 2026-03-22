import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { userService } from "@/services/user.service";
import type { CreateUserRequest } from "@/services/user.service";

export function useUsers() {
  return useQuery({
    queryKey: ["users"],
    queryFn: () => userService.getAll().then((res) => res.data.data),
  });
}

export function useCreateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserRequest) => userService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
}

export function useUpdateRole() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, role }: { id: string; role: string }) =>
      userService.updateRole(id, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
}

export function useDeactivateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => userService.deactivate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
}