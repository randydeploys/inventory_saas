import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useBuildings } from "@/hooks/useBuildings";
import { useRooms } from "@/hooks/useRooms";

interface ReassignDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (targetRoomId: string) => void;
  isLoading: boolean;
  title: string;
  description: string;
  excludeBuildingId?: string;
  excludeRoomId?: string;
}

export default function ReassignDialog({
  open,
  onClose,
  onSubmit,
  isLoading,
  title,
  description,
  excludeBuildingId,
  excludeRoomId,
}: ReassignDialogProps) {
  const [selectedBuildingId, setSelectedBuildingId] = useState("");
  const [selectedRoomId, setSelectedRoomId] = useState("");

  const { data: buildings } = useBuildings();
  const { data: rooms } = useRooms(selectedBuildingId);

  const availableBuildings = buildings?.filter((b) => b.id !== excludeBuildingId) ?? [];
  const availableRooms = rooms?.filter((r) => r.id !== excludeRoomId) ?? [];

  const handleBuildingChange = (buildingId: string) => {
    setSelectedBuildingId(buildingId);
    setSelectedRoomId("");
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedRoomId) onSubmit(selectedRoomId);
  };

  const handleClose = () => {
    setSelectedBuildingId("");
    setSelectedRoomId("");
    onClose();
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <p className="text-sm text-muted-foreground">{description}</p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label>Bâtiment cible</Label>
            <Select value={selectedBuildingId} onValueChange={handleBuildingChange}>
              <SelectTrigger>
                <SelectValue placeholder="Choisir un bâtiment" />
              </SelectTrigger>
              <SelectContent>
                {availableBuildings.map((b) => (
                  <SelectItem key={b.id} value={b.id}>
                    {b.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Zone cible</Label>
            <Select
              value={selectedRoomId}
              onValueChange={setSelectedRoomId}
              disabled={!selectedBuildingId}
            >
              <SelectTrigger>
                <SelectValue
                  placeholder={
                    selectedBuildingId
                      ? "Choisir une zone"
                      : "Sélectionnez d'abord un bâtiment"
                  }
                />
              </SelectTrigger>
              <SelectContent>
                {availableRooms.map((r) => (
                  <SelectItem key={r.id} value={r.id}>
                    {r.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={handleClose}>
              Annuler
            </Button>
            <Button type="submit" disabled={!selectedRoomId || isLoading}>
              {isLoading ? "En cours..." : "Réaffecter"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
