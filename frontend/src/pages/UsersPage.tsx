import { useState } from "react";
import {
  useUsers,
  useCreateUser,
  useUpdateRole,
  useDeactivateUser,
} from "@/hooks/useUsers";
import { useAuth } from "@/context/AuthContext";
import UserDialog from "@/components/users/UserDialog";
import ConfirmDialog from "@/components/shared/ConfirmDialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Plus, UserX } from "lucide-react";
import { toast } from "sonner";
import { getErrorMessage } from "@/lib/error";

export default function UsersPage() {
  const { user: currentUser } = useAuth();
  const { data: users, isLoading, error: queryError } = useUsers();
  const createMutation = useCreateUser();
  const updateRoleMutation = useUpdateRole();
  const deactivateMutation = useDeactivateUser();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [formError, setFormError] = useState("");
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmTargetId, setConfirmTargetId] = useState<string | null>(null);

  const handleCreate = (data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    role: string;
  }) => {
    setFormError("");
    createMutation.mutate(data, {
      onSuccess: () => {
        setDialogOpen(false);
        toast.success("Utilisateur créé");
      },
      onError: (err) => setFormError(getErrorMessage(err)),
    });
  };

  const handleRoleChange = (userId: string, newRole: string) => {
    updateRoleMutation.mutate(
      { id: userId, role: newRole },
      {
        onSuccess: () => toast.success("Rôle modifié"),
        onError: (err) => toast.error(getErrorMessage(err)),
      }
    );
  };

  const handleDeactivate = (userId: string) => {
    setConfirmTargetId(userId);
    setConfirmOpen(true);
  };

  const handleConfirmDeactivate = () => {
    if (!confirmTargetId) return;
    const userId = confirmTargetId;
    setConfirmOpen(false);
    setConfirmTargetId(null);
    deactivateMutation.mutate(userId, {
      onSuccess: () => toast.success("Utilisateur désactivé"),
      onError: (err) => toast.error(getErrorMessage(err)),
    });
  };

  const handleOpen = () => {
    setFormError("");
    setDialogOpen(true);
  };

  if (isLoading) return <div>Chargement...</div>;
  if (queryError) return <div>Erreur lors du chargement</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Utilisateurs</h1>
        <Button onClick={handleOpen}>
          <Plus className="h-4 w-4 mr-2" />
          Nouvel utilisateur
        </Button>
      </div>

      {users?.length === 0 ? (
        <p className="text-muted-foreground">Aucun utilisateur.</p>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {users?.map((user) => (
            <Card
              key={user.id}
              className={!user.isActive ? "opacity-50" : ""}
            >
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <div>
                  <CardTitle className="text-lg">
                    {user.firstName} {user.lastName}
                  </CardTitle>
                  <p className="text-sm text-muted-foreground">{user.email}</p>
                </div>
                {user.id !== currentUser?.id && user.isActive && (
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => handleDeactivate(user.id)}
                  >
                    <UserX className="h-4 w-4 text-red-500" />
                  </Button>
                )}
              </CardHeader>
              <CardContent>
                <div className="flex items-center justify-between">
                  {user.id === currentUser?.id ? (
                    <span className="text-xs bg-primary/10 text-primary px-2 py-0.5 rounded">
                      {user.role} (vous)
                    </span>
                  ) : user.isActive ? (
                    <select
                      value={user.role}
                      onChange={(e) => handleRoleChange(user.id, e.target.value)}
                      className="rounded-md border border-input bg-background px-2 py-1 text-xs"
                    >
                      <option value="ADMIN">Admin</option>
                      <option value="MANAGER">Manager</option>
                      <option value="READER">Lecteur</option>
                    </select>
                  ) : (
                    <span className="text-xs text-red-500">Désactivé</span>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <UserDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onSubmit={handleCreate}
        isLoading={createMutation.isPending}
        error={formError}
      />

      <ConfirmDialog
        open={confirmOpen}
        onClose={() => { setConfirmOpen(false); setConfirmTargetId(null); }}
        onConfirm={handleConfirmDeactivate}
        title="Désactiver cet utilisateur ?"
        description="L'utilisateur ne pourra plus se connecter. Cette action est réversible par un Admin."
        confirmLabel="Désactiver"
        isLoading={deactivateMutation.isPending}
      />
    </div>
  );
}