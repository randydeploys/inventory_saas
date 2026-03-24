import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { Building, Room } from "@/types/api";

interface RoomDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: { name: string; description?: string; buildingId?: string }) => void;
  room?: Room | null;
  buildings?: Building[];
  isLoading?: boolean;
}

export default function RoomDialog({
  open,
  onClose,
  onSubmit,
  room,
  buildings,
  isLoading,
}: RoomDialogProps) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [buildingId, setBuildingId] = useState("");

  useEffect(() => {
    if (room) {
      setName(room.name);
      setDescription(room.description || "");
      setBuildingId("");
    } else {
      setName("");
      setDescription("");
      setBuildingId("");
    }
  }, [room, open]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({ name, description: description || undefined, buildingId: buildingId || undefined });
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {room ? "Modifier la zone" : "Nouvelle zone"}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          {!room && buildings && (
            <div className="space-y-2">
              <Label htmlFor="building">Bâtiment</Label>
              <Select value={buildingId} onValueChange={setBuildingId} required>
                <SelectTrigger id="building">
                  <SelectValue placeholder="Choisir un bâtiment" />
                </SelectTrigger>
                <SelectContent>
                  {buildings.map((b) => (
                    <SelectItem key={b.id} value={b.id}>
                      {b.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}
          <div className="space-y-2">
            <Label htmlFor="name">Nom</Label>
            <Input
              id="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Zone A"
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Input
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Zone de stockage principale"
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Annuler
            </Button>
            <Button
              type="submit"
              disabled={isLoading || (!room && !!buildings && !buildingId)}
            >
              {isLoading ? "En cours..." : room ? "Modifier" : "Créer"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}