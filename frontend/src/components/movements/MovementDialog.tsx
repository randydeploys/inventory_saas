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
import { useProducts } from "@/hooks/useProducts";
import { useBuildings } from "@/hooks/useBuildings";
import { useRooms } from "@/hooks/useRooms";

interface MovementDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: {
    productId: string;
    type: "IN" | "OUT" | "TRANSFER";
    quantity: number;
    fromRoomId?: string;
    toRoomId?: string;
    reason?: string;
  }) => void;
  isLoading?: boolean;
  error?: string;
}

export default function MovementDialog({
  open,
  onClose,
  onSubmit,
  isLoading,
  error,
}: MovementDialogProps) {
  const [productId, setProductId] = useState("");
  const [type, setType] = useState<"IN" | "OUT" | "TRANSFER">("IN");
  const [quantity, setQuantity] = useState("1");
  const [fromBuildingId, setFromBuildingId] = useState("");
  const [fromRoomId, setFromRoomId] = useState("");
  const [toBuildingId, setToBuildingId] = useState("");
  const [toRoomId, setToRoomId] = useState("");
  const [reason, setReason] = useState("");

  const { data: products } = useProducts({ size: 100 });
  const { data: buildings } = useBuildings();
  const { data: fromRooms } = useRooms(fromBuildingId);
  const { data: toRooms } = useRooms(toBuildingId);

  // Reset les rooms quand on change de building
  useEffect(() => {
    setFromRoomId("");
  }, [fromBuildingId]);

  useEffect(() => {
    setToRoomId("");
  }, [toBuildingId]);

  // Reset le formulaire quand le dialog s'ouvre
  useEffect(() => {
    if (open) {
      setProductId("");
      setType("IN");
      setQuantity("1");
      setFromBuildingId("");
      setFromRoomId("");
      setToBuildingId("");
      setToRoomId("");
      setReason("");
    }
  }, [open]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({
      productId,
      type,
      quantity: parseInt(quantity),
      fromRoomId: type === "OUT" || type === "TRANSFER" ? fromRoomId : undefined,
      toRoomId: type === "IN" || type === "TRANSFER" ? toRoomId : undefined,
      reason: reason || undefined,
    });
  };

  // Trouver le produit sélectionné pour vérifier le tracking type
  const selectedProduct = products?.content.find((p) => p.id === productId);
  const isUnique = selectedProduct?.trackingType === "UNIQUE";

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Nouveau mouvement de stock</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">

          {error && (
            <div className="text-sm text-red-500 bg-red-50 p-3 rounded-md">
              {error}
            </div>
          )}
          {/* Produit */}
          <div className="space-y-2">
            <Label>Produit</Label>
            <select
              value={productId}
              onChange={(e) => setProductId(e.target.value)}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              required
            >
              <option value="">Choisir un produit</option>
              {products?.content.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.sku})
                </option>
              ))}
            </select>
          </div>

          {/* Type de mouvement */}
          <div className="space-y-2">
            <Label>Type</Label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value as "IN" | "OUT" | "TRANSFER")}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            >
              <option value="IN">Entrée (IN)</option>
              <option value="OUT">Sortie (OUT)</option>
              <option value="TRANSFER">Transfert (TRANSFER)</option>
            </select>
          </div>

          {/* Quantité — fixée à 1 pour les produits UNIQUE */}
          <div className="space-y-2">
            <Label>Quantité</Label>
            <Input
              type="number"
              min="1"
              value={isUnique ? "1" : quantity}
              onChange={(e) => setQuantity(e.target.value)}
              disabled={isUnique}
              required
            />
            {isUnique && (
              <p className="text-xs text-muted-foreground">
                Quantité fixée à 1 pour les produits uniques
              </p>
            )}
          </div>

          {/* From Room — pour OUT et TRANSFER */}
          {(type === "OUT" || type === "TRANSFER") && (
            <div className="space-y-2">
              <Label>Depuis</Label>
              <div className="grid grid-cols-2 gap-2">
                <select
                  value={fromBuildingId}
                  onChange={(e) => setFromBuildingId(e.target.value)}
                  className="rounded-md border border-input bg-background px-3 py-2 text-sm"
                  required
                >
                  <option value="">Bâtiment</option>
                  {buildings?.map((b) => (
                    <option key={b.id} value={b.id}>{b.name}</option>
                  ))}
                </select>
                <select
                  value={fromRoomId}
                  onChange={(e) => setFromRoomId(e.target.value)}
                  className="rounded-md border border-input bg-background px-3 py-2 text-sm"
                  required
                  disabled={!fromBuildingId}
                >
                  <option value="">Zone</option>
                  {fromRooms?.map((r) => (
                    <option key={r.id} value={r.id}>{r.name}</option>
                  ))}
                </select>
              </div>
            </div>
          )}

          {/* To Room — pour IN et TRANSFER */}
          {(type === "IN" || type === "TRANSFER") && (
            <div className="space-y-2">
              <Label>{type === "TRANSFER" ? "Vers" : "Destination"}</Label>
              <div className="grid grid-cols-2 gap-2">
                <select
                  value={toBuildingId}
                  onChange={(e) => setToBuildingId(e.target.value)}
                  className="rounded-md border border-input bg-background px-3 py-2 text-sm"
                  required
                >
                  <option value="">Bâtiment</option>
                  {buildings?.map((b) => (
                    <option key={b.id} value={b.id}>{b.name}</option>
                  ))}
                </select>
                <select
                  value={toRoomId}
                  onChange={(e) => setToRoomId(e.target.value)}
                  className="rounded-md border border-input bg-background px-3 py-2 text-sm"
                  required
                  disabled={!toBuildingId}
                >
                  <option value="">Zone</option>
                  {toRooms?.map((r) => (
                    <option key={r.id} value={r.id}>{r.name}</option>
                  ))}
                </select>
              </div>
            </div>
          )}

          {/* Raison */}
          <div className="space-y-2">
            <Label>Raison (optionnel)</Label>
            <Input
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Livraison fournisseur, vente client..."
            />
          </div>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Annuler
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? "En cours..." : "Enregistrer"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}