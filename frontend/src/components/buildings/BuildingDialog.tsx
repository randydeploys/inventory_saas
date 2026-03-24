import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { Building } from "@/types/api";

interface BuildingDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: { name: string; address?: string }) => void;
  building?: Building | null; // null = création, Building = édition
  isLoading?: boolean;
}

export default function BuildingDialog({
  open,
  onClose,
  onSubmit,
  building,
  isLoading,
}: BuildingDialogProps) {
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");

  // Quand le dialog s'ouvre avec un building existant, pré-remplir les champs
  useEffect(() => {
    if (building) {
      setName(building.name);
      setAddress(building.address || "");
    } else {
      setName("");
      setAddress("");
    }
  }, [building, open]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({ name, address: address || undefined });
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {building ? "Modifier le bâtiment" : "Nouveau bâtiment"}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">Nom</Label>
            <Input
              id="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Entrepôt Nord"
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="address">Adresse</Label>
            <Input
              id="address"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder="12 rue de Paris"
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Annuler
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? "En cours..." : building ? "Modifier" : "Créer"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}