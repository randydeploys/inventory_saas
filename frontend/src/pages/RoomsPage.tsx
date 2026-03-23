import { useState } from "react";
import { useAllRooms, useCreateRoom, useUpdateRoom, useDeleteRoom, useReassignRoom } from "@/hooks/useRooms";
import { useBuildings } from "@/hooks/useBuildings";
import { useAuth } from "@/context/AuthContext";
import RoomDialog from "@/components/rooms/RoomDialog";
import ArchiveDialog from "@/components/shared/ArchiveDialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Plus, Pencil, Trash2 } from "lucide-react";
import type { Room } from "@/types/api";
import { toast } from "sonner";
import { getErrorMessage } from "@/lib/error";

export default function RoomsPage() {
  const { user } = useAuth();
  const canEdit = user?.role === "ADMIN" || user?.role === "MANAGER";

  const { data: rooms, isLoading } = useAllRooms();
  const { data: buildings } = useBuildings();

  const createMutation = useCreateRoom();
  const updateMutation = useUpdateRoom();
  const deleteMutation = useDeleteRoom();
  const reassignMutation = useReassignRoom();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingRoom, setEditingRoom] = useState<Room | null>(null);
  const [archiveRoom, setArchiveRoom] = useState<Room | null>(null);

  const handleCreate = () => {
    setEditingRoom(null);
    setDialogOpen(true);
  };

  const handleEdit = (room: Room) => {
    setEditingRoom(room);
    setDialogOpen(true);
  };

  const handleDelete = (room: Room) => {
    setArchiveRoom(room);
  };

  const handleConfirmArchive = () => {
    if (!archiveRoom) return;
    const id = archiveRoom.id;
    setArchiveRoom(null);
    deleteMutation.mutate(id, {
      onSuccess: () => toast.success("Zone archivée"),
      onError: (err) => toast.error(getErrorMessage(err)),
    });
  };

  const handleReassign = (targetRoomId: string) => {
    if (!archiveRoom) return;
    const id = archiveRoom.id;
    reassignMutation.mutate(
      { id, targetRoomId },
      {
        onSuccess: () => {
          setArchiveRoom(null);
          deleteMutation.mutate(id, {
            onSuccess: () => toast.success("Produits déplacés et zone archivée"),
            onError: (err) => toast.error(getErrorMessage(err)),
          });
        },
        onError: (err) => toast.error(getErrorMessage(err)),
      }
    );
  };

  const handleSubmit = (data: { name: string; description?: string; buildingId?: string }) => {
    if (editingRoom) {
      updateMutation.mutate(
        { id: editingRoom.id, data },
        {
          onSuccess: () => {
            setDialogOpen(false);
            toast.success("Zone modifiée");
          },
          onError: (err) => toast.error(getErrorMessage(err)),
        }
      );
    } else {
      if (!data.buildingId) return;
      createMutation.mutate(
        { buildingId: data.buildingId, data },
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

  if (isLoading) return <div>Chargement...</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Zones</h1>
        {canEdit && (
          <Button onClick={handleCreate}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle zone
          </Button>
        )}
      </div>

      {rooms?.length === 0 ? (
        <p className="text-muted-foreground">Aucune zone.</p>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {rooms?.map((room) => (
            <Card key={room.id}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-lg">{room.name}</CardTitle>
                {canEdit && (
                  <div className="flex gap-1">
                    <Button variant="ghost" size="icon" onClick={() => handleEdit(room)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => handleDelete(room)}>
                      <Trash2 className="h-4 w-4 text-red-500" />
                    </Button>
                  </div>
                )}
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground mb-1">{room.buildingName}</p>
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
        buildings={editingRoom ? undefined : buildings}
        isLoading={createMutation.isPending || updateMutation.isPending}
      />

      <ArchiveDialog
        open={!!archiveRoom}
        onClose={() => setArchiveRoom(null)}
        onConfirm={handleConfirmArchive}
        onReassign={handleReassign}
        entityLabel="cette zone"
        activeProductCount={archiveRoom?.activeProductCount ?? 0}
        excludeRoomId={archiveRoom?.id}
        isLoading={deleteMutation.isPending || reassignMutation.isPending}
      />
    </div>
  );
}
