import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { useAllRooms } from "@/hooks/useRooms";

interface ArchiveDialogProps {
  open: boolean;
  onClose: () => void;
  /** Appelé quand la zone est vide — archive directement */
  onConfirm: () => void;
  /** Appelé quand l'utilisateur choisit une zone cible — déplace puis archive */
  onReassign: (targetRoomId: string) => void;
  entityLabel: string; // "ce bâtiment" | "cette zone"
  activeProductCount: number;
  excludeRoomId?: string;
  isLoading?: boolean;
}

export default function ArchiveDialog({
  open,
  onClose,
  onConfirm,
  onReassign,
  entityLabel,
  activeProductCount,
  excludeRoomId,
  isLoading,
}: ArchiveDialogProps) {
  const [targetRoomId, setTargetRoomId] = useState("");
  const { data: rooms } = useAllRooms();

  useEffect(() => {
    if (!open) setTargetRoomId("");
  }, [open]);

  const availableRooms = rooms?.filter((r) => r.id !== excludeRoomId) ?? [];
  const hasProducts = activeProductCount > 0;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (hasProducts) {
      if (targetRoomId) onReassign(targetRoomId);
    } else {
      onConfirm();
    }
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>Archiver {entityLabel} ?</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          {hasProducts ? (
            <>
              <p className="text-sm text-muted-foreground">
                {entityLabel === "cette zone" ? "Cette zone contient" : "Ce bâtiment contient"}{" "}
                <span className="font-medium text-foreground">
                  {activeProductCount} produit{activeProductCount > 1 ? "s" : ""} actif{activeProductCount > 1 ? "s" : ""}
                </span>
                . Choisissez une zone cible pour les déplacer avant d'archiver.
              </p>
              <div className="space-y-2">
                <Label>Zone cible</Label>
                <Select value={targetRoomId} onValueChange={setTargetRoomId} required>
                  <SelectTrigger>
                    <SelectValue placeholder="Choisir une zone" />
                  </SelectTrigger>
                  <SelectContent>
                    {availableRooms.map((r) => (
                      <SelectItem key={r.id} value={r.id}>
                        {r.name}
                        <span className="text-muted-foreground ml-1 text-xs">— {r.buildingName}</span>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </>
          ) : (
            <p className="text-sm text-muted-foreground">
              Cette action est réversible. {entityLabel === "cette zone" ? "La zone" : "Le bâtiment"} ne sera plus visible dans les listes actives.
            </p>
          )}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose} disabled={isLoading}>
              Annuler
            </Button>
            <Button
              type="submit"
              variant="destructive"
              disabled={isLoading || (hasProducts && !targetRoomId)}
            >
              {isLoading
                ? "En cours..."
                : hasProducts
                ? "Déplacer et archiver"
                : "Archiver"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
