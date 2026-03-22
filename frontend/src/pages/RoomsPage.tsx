import { useState } from "react";
import { useBuildings } from "@/hooks/useBuildings";
import { useRooms, useCreateRoom, useUpdateRoom, useDeleteRoom } from "@/hooks/useRooms";
import { useAuth } from "@/context/AuthContext";
import RoomDialog from "@/components/rooms/RoomDialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Plus, Pencil, Trash2 } from "lucide-react";
import type { Room } from "@/types/api";
import { toast } from "sonner";
import { getErrorMessage } from "@/lib/error";

export default function RoomsPage() {
  const { user } = useAuth();
  const canEdit = user?.role === "ADMIN" || user?.role === "MANAGER";

  const { data: buildings, isLoading: buildingsLoading } = useBuildings();
  const [selectedBuildingId, setSelectedBuildingId] = useState<string>("");
  const { data: rooms, isLoading: roomsLoading } = useRooms(selectedBuildingId);

  const createMutation = useCreateRoom();
  const updateMutation = useUpdateRoom();
  const deleteMutation = useDeleteRoom();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingRoom, setEditingRoom] = useState<Room | null>(null);

  const handleCreate = () => {
    setEditingRoom(null);
    setDialogOpen(true);
  };

  const handleEdit = (room: Room) => {
    setEditingRoom(room);
    setDialogOpen(true);
  };

  const handleDelete = (id: string) => {
    if (window.confirm("Archiver cette zone ?")) {
      deleteMutation.mutate(id, {
        onSuccess: () => toast.success("Zone archivée"),
        onError: (err) => toast.error(getErrorMessage(err)),
      });
    }
  };

  const handleSubmit = (data: { name: string; description?: string }) => {
    if (editingRoom) {
      updateMutation.mutate(
        { id: editingRoom.id, data },
 {
        onSuccess: () => {
          setDialogOpen(false);
          toast.success("Zone modifiée");
        },
        onError: (err) => toast.error(getErrorMessage(err)),
      }      );
    } else {
      createMutation.mutate(
        { buildingId: selectedBuildingId, data },
        { 
          onSuccess: () => {
          setDialogOpen(false);
          toast.success("Zone créée");
        },
        onError: (err) => toast.error(getErrorMessage(err)),
      }
      );
    }
  };

  if (buildingsLoading) return <div>Chargement...</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Zones</h1>
        {canEdit && selectedBuildingId && (
          <Button onClick={handleCreate}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle zone
          </Button>
        )}
      </div>

      {/* Sélecteur de bâtiment */}
      <div className="mb-6 max-w-sm">
        <Select value={selectedBuildingId} onValueChange={setSelectedBuildingId}>
          <SelectTrigger>
            <SelectValue placeholder="Choisir un bâtiment" />
          </SelectTrigger>
          <SelectContent>
            {buildings?.map((building) => (
              <SelectItem key={building.id} value={building.id}>
                {building.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Liste des zones */}
      {!selectedBuildingId ? (
        <p className="text-muted-foreground">Sélectionnez un bâtiment pour voir ses zones.</p>
      ) : roomsLoading ? (
        <div>Chargement des zones...</div>
      ) : rooms?.length === 0 ? (
        <p className="text-muted-foreground">Aucune zone dans ce bâtiment.</p>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {rooms?.map((room) => (
            <Card key={room.id}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-lg">{room.name}</CardTitle>
                {canEdit && (
                  <div className="flex gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleEdit(room)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleDelete(room.id)}
                    >
                      <Trash2 className="h-4 w-4 text-red-500" />
                    </Button>
                  </div>
                )}
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  {room.description || "Pas de description"}
                </p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <RoomDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onSubmit={handleSubmit}
        room={editingRoom}
        isLoading={createMutation.isPending || updateMutation.isPending}
      />
    </div>
  );
}