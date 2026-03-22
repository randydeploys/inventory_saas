import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useBuilding, useDeleteBuilding, useUpdateBuilding, useReassignBuilding } from "@/hooks/useBuildings";
import { useRooms, useCreateRoom, useUpdateRoom, useDeleteRoom, useReassignRoom } from "@/hooks/useRooms";
import { useAuth } from "@/context/AuthContext";
import RoomDialog from "@/components/rooms/RoomDialog";
import BuildingDialog from "@/components/buildings/BuildingDialog";
import ReassignDialog from "@/components/shared/ReassignDialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ArrowLeft, Plus, Pencil, Trash2, MapPin, DoorOpen } from "lucide-react";
import { toast } from "sonner";
import { getErrorMessage, isConflict } from "@/lib/error";
import type { Room } from "@/types/api";

export default function BuildingDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const canEditRooms = user?.role === "ADMIN" || user?.role === "MANAGER";

  const { data: building, isLoading: buildingLoading } = useBuilding(id!);
  const { data: rooms, isLoading: roomsLoading } = useRooms(id!);
  const deleteBuildingMutation = useDeleteBuilding();
  const updateBuildingMutation = useUpdateBuilding();
  const reassignBuildingMutation = useReassignBuilding();
  const createRoomMutation = useCreateRoom();
  const updateRoomMutation = useUpdateRoom();
  const deleteRoomMutation = useDeleteRoom();
  const reassignRoomMutation = useReassignRoom();

  const [buildingDialogOpen, setBuildingDialogOpen] = useState(false);
  const [roomDialogOpen, setRoomDialogOpen] = useState(false);
  const [editingRoom, setEditingRoom] = useState<Room | null>(null);

  // Reassign building state
  const [reassignBuildingOpen, setReassignBuildingOpen] = useState(false);
  const [reassignBuildingMessage, setReassignBuildingMessage] = useState("");

  // Reassign room state
  const [reassignRoomOpen, setReassignRoomOpen] = useState(false);
  const [reassignRoomId, setReassignRoomId] = useState<string | null>(null);
  const [reassignRoomMessage, setReassignRoomMessage] = useState("");

  const handleDeleteBuilding = () => {
    if (!window.confirm("Archiver ce bâtiment ?")) return;
    deleteBuildingMutation.mutate(id!, {
      onSuccess: () => {
        toast.success("Bâtiment archivé");
        navigate("/buildings");
      },
      onError: (err) => {
        if (isConflict(err)) {
          setReassignBuildingMessage(getErrorMessage(err));
          setReassignBuildingOpen(true);
        } else {
          toast.error(getErrorMessage(err));
        }
      },
    });
  };

  const handleReassignBuilding = (targetRoomId: string) => {
    reassignBuildingMutation.mutate(
      { id: id!, targetRoomId },
      {
        onSuccess: () => {
          setReassignBuildingOpen(false);
          toast.success("Produits réaffectés. Vous pouvez maintenant archiver le bâtiment.");
        },
        onError: (err) => toast.error(getErrorMessage(err)),
      }
    );
  };

  const handleEditBuilding = () => {
    setBuildingDialogOpen(true);
  };

  const handleBuildingSubmit = (data: { name: string; address?: string }) => {
    updateBuildingMutation.mutate(
      { id: id!, data },
      {
        onSuccess: () => {
          setBuildingDialogOpen(false);
          toast.success("Bâtiment modifié");
        },
        onError: (err) => toast.error(getErrorMessage(err)),
      }
    );
  };

  const handleCreateRoom = () => {
    setEditingRoom(null);
    setRoomDialogOpen(true);
  };

  const handleEditRoom = (room: Room) => {
    setEditingRoom(room);
    setRoomDialogOpen(true);
  };

  const handleDeleteRoom = (roomId: string) => {
    if (!window.confirm("Archiver cette zone ?")) return;
    deleteRoomMutation.mutate(roomId, {
      onSuccess: () => toast.success("Zone archivée"),
      onError: (err) => {
        if (isConflict(err)) {
          setReassignRoomId(roomId);
          setReassignRoomMessage(getErrorMessage(err));
          setReassignRoomOpen(true);
        } else {
          toast.error(getErrorMessage(err));
        }
      },
    });
  };

  const handleReassignRoom = (targetRoomId: string) => {
    if (!reassignRoomId) return;
    reassignRoomMutation.mutate(
      { id: reassignRoomId, targetRoomId },
      {
        onSuccess: () => {
          setReassignRoomOpen(false);
          setReassignRoomId(null);
          toast.success("Produits réaffectés. Vous pouvez maintenant archiver la zone.");
        },
        onError: (err) => toast.error(getErrorMessage(err)),
      }
    );
  };

  const handleRoomSubmit = (data: { name: string; description?: string }) => {
    if (editingRoom) {
      updateRoomMutation.mutate(
        { id: editingRoom.id, data },
        {
          onSuccess: () => {
            setRoomDialogOpen(false);
            toast.success("Zone modifiée");
          },
          onError: (err) => toast.error(getErrorMessage(err)),
        }
      );
    } else {
      createRoomMutation.mutate(
        { buildingId: id!, data },
        {
          onSuccess: () => {
            setRoomDialogOpen(false);
            toast.success("Zone créée");
          },
          onError: (err) => toast.error(getErrorMessage(err)),
        }
      );
    }
  };

  if (buildingLoading) return <div>Chargement...</div>;
  if (!building) return <div>Bâtiment introuvable</div>;

  return (
    <div>
      {/* Header avec retour */}
      <div className="flex items-center gap-4 mb-6">
        <Button variant="ghost" size="icon" onClick={() => navigate("/buildings")}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div className="flex-1">
          <h1 className="text-2xl font-bold">{building.name}</h1>
          {building.address && (
            <p className="text-sm text-muted-foreground flex items-center gap-1 mt-1">
              <MapPin className="h-3 w-3" />
              {building.address}
            </p>
          )}
        </div>
        {isAdmin && (
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={handleEditBuilding}>
              <Pencil className="h-4 w-4 mr-2" />
              Modifier
            </Button>
            <Button variant="outline" size="sm" onClick={handleDeleteBuilding}>
              <Trash2 className="h-4 w-4 mr-2 text-red-500" />
              Archiver
            </Button>
          </div>
        )}
      </div>

      {/* Infos du bâtiment */}
      <div className="grid grid-cols-2 gap-4 mb-8">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <DoorOpen className="h-8 w-8 text-muted-foreground" />
              <div>
                <p className="text-2xl font-bold">{rooms?.length ?? 0}</p>
                <p className="text-sm text-muted-foreground">Zones</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-sm text-muted-foreground space-y-1">
              <p>Créé le {new Date(building.createdAt).toLocaleDateString("fr-FR")}</p>
              <p>Modifié le {new Date(building.updatedAt).toLocaleDateString("fr-FR")}</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Liste des zones */}
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-semibold">Zones</h2>
        {canEditRooms && (
          <Button onClick={handleCreateRoom} size="sm">
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle zone
          </Button>
        )}
      </div>

      {roomsLoading ? (
        <div>Chargement des zones...</div>
      ) : rooms?.length === 0 ? (
        <p className="text-muted-foreground">Aucune zone dans ce bâtiment.</p>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {rooms?.map((room) => (
            <Card key={room.id}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-lg">{room.name}</CardTitle>
                {canEditRooms && (
                  <div className="flex gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleEditRoom(room)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleDeleteRoom(room.id)}
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

      <BuildingDialog
        open={buildingDialogOpen}
        onClose={() => setBuildingDialogOpen(false)}
        onSubmit={handleBuildingSubmit}
        building={building}
        isLoading={updateBuildingMutation.isPending}
      />

      <RoomDialog
        open={roomDialogOpen}
        onClose={() => setRoomDialogOpen(false)}
        onSubmit={handleRoomSubmit}
        room={editingRoom}
        isLoading={createRoomMutation.isPending || updateRoomMutation.isPending}
      />

      <ReassignDialog
        open={reassignBuildingOpen}
        onClose={() => setReassignBuildingOpen(false)}
        onSubmit={handleReassignBuilding}
        isLoading={reassignBuildingMutation.isPending}
        title="Réaffecter les produits du bâtiment"
        description={reassignBuildingMessage}
        excludeBuildingId={id}
      />

      <ReassignDialog
        open={reassignRoomOpen}
        onClose={() => {
          setReassignRoomOpen(false);
          setReassignRoomId(null);
        }}
        onSubmit={handleReassignRoom}
        isLoading={reassignRoomMutation.isPending}
        title="Réaffecter les produits de la zone"
        description={reassignRoomMessage}
        excludeRoomId={reassignRoomId ?? undefined}
      />
    </div>
  );
}
