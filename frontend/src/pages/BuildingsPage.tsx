import { useState } from "react";
import { useBuildings, useCreateBuilding, useUpdateBuilding, useDeleteBuilding, useReassignBuilding } from "@/hooks/useBuildings";
import { useAuth } from "@/context/AuthContext";
import BuildingDialog from "@/components/buildings/BuildingDialog";
import ReassignDialog from "@/components/shared/ReassignDialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Plus, Pencil, Trash2 } from "lucide-react";
import type { Building } from "@/types/api";
import { toast } from "sonner";
import { getErrorMessage, isConflict } from "@/lib/error";
import { useNavigate } from "react-router-dom";

export default function BuildingsPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const { data: buildings, isLoading, error } = useBuildings();
  const createMutation = useCreateBuilding();
  const updateMutation = useUpdateBuilding();
  const deleteMutation = useDeleteBuilding();
  const reassignMutation = useReassignBuilding();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingBuilding, setEditingBuilding] = useState<Building | null>(null);
  const [reassignOpen, setReassignOpen] = useState(false);
  const [reassignBuildingId, setReassignBuildingId] = useState<string | null>(null);
  const [reassignMessage, setReassignMessage] = useState("");


  const navigate = useNavigate();
  const handleCreate = () => {
    setEditingBuilding(null);
    setDialogOpen(true);
  };

  const handleEdit = (building: Building) => {
    setEditingBuilding(building);
    setDialogOpen(true);
  };

const handleDelete = (id: string) => {
  if (!window.confirm("Archiver ce bâtiment ?")) return;
  deleteMutation.mutate(id, {
    onSuccess: () => toast.success("Bâtiment archivé"),
    onError: (err) => {
      if (isConflict(err)) {
        setReassignBuildingId(id);
        setReassignMessage(getErrorMessage(err));
        setReassignOpen(true);
      } else {
        toast.error(getErrorMessage(err));
      }
    },
  });
};

const handleReassign = (targetRoomId: string) => {
  if (!reassignBuildingId) return;
  reassignMutation.mutate(
    { id: reassignBuildingId, targetRoomId },
    {
      onSuccess: () => {
        setReassignOpen(false);
        setReassignBuildingId(null);
        toast.success("Produits réaffectés. Vous pouvez maintenant archiver le bâtiment.");
      },
      onError: (err) => toast.error(getErrorMessage(err)),
    }
  );
};

const handleSubmit = (data: { name: string; address?: string }) => {
  if (editingBuilding) {
    updateMutation.mutate(
      { id: editingBuilding.id, data },
      {
        onSuccess: () => {
          setDialogOpen(false);
          toast.success("Bâtiment modifié");
        },
        onError: (err) => toast.error(getErrorMessage(err)),
      }
    );
  } else {
    createMutation.mutate(data, {
      onSuccess: () => {
        setDialogOpen(false);
        toast.success("Bâtiment créé");
      },
      onError: (err) => toast.error(getErrorMessage(err)),
    });
  }
};

  if (isLoading) return <div>Chargement...</div>;
  if (error) return <div>Erreur lors du chargement</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Bâtiments</h1>
        {isAdmin && (
          <Button onClick={handleCreate}>
            <Plus className="h-4 w-4 mr-2" />
            Nouveau bâtiment
          </Button>
        )}
      </div>

      {buildings?.length === 0 ? (
        <p className="text-muted-foreground">Aucun bâtiment pour le moment.</p>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {buildings?.map((building) => (
            <Card key={building.id}   className="cursor-pointer hover:border-primary transition-colors"
  onClick={() => navigate(`/buildings/${building.id}`)} >
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-lg">{building.name}</CardTitle>
                {isAdmin && (
                  <div className="flex gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleEdit(building);
                      }}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDelete(building.id);
                      }}
                      
                    >
                      <Trash2 className="h-4 w-4 text-red-500" />
                    </Button>
                  </div>
                )}
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  {building.address || "Pas d'adresse"}
                </p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <BuildingDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onSubmit={handleSubmit}
        building={editingBuilding}
        isLoading={createMutation.isPending || updateMutation.isPending}
      />

      <ReassignDialog
        open={reassignOpen}
        onClose={() => {
          setReassignOpen(false);
          setReassignBuildingId(null);
        }}
        onSubmit={handleReassign}
        isLoading={reassignMutation.isPending}
        title="Réaffecter les produits du bâtiment"
        description={reassignMessage}
        excludeBuildingId={reassignBuildingId ?? undefined}
      />
    </div>
  );
}