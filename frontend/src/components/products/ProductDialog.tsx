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
import { useCategories } from "@/hooks/useCategories";
import { useAllRooms } from "@/hooks/useRooms";
import type { Product } from "@/types/api";

export interface InitialStock {
  toRoomId: string;
  quantity: number;
  reason?: string;
}

interface ProductDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (
    data: {
      name: string;
      sku: string;
      description?: string;
      trackingType: "QUANTITY" | "UNIQUE";
      categoryId?: string;
      serialNumber?: string;
      minQuantity?: number;
      unit?: string;
    },
    initialStock?: InitialStock
  ) => void;
  product?: Product | null;
  isLoading?: boolean;
}

export default function ProductDialog({
  open,
  onClose,
  onSubmit,
  product,
  isLoading,
}: ProductDialogProps) {
  const [name, setName] = useState("");
  const [sku, setSku] = useState("");
  const [description, setDescription] = useState("");
  const [trackingType, setTrackingType] = useState<"QUANTITY" | "UNIQUE">("QUANTITY");
  const [categoryId, setCategoryId] = useState("");
  const [serialNumber, setSerialNumber] = useState("");
  const [minQuantity, setMinQuantity] = useState("");
  const [unit, setUnit] = useState("");

  // Stock initial (create mode only)
  const [toRoomId, setToRoomId] = useState("");
  const [initialQuantity, setInitialQuantity] = useState("1");
  const [initialReason, setInitialReason] = useState("");

  const { data: categories } = useCategories();
  const { data: rooms } = useAllRooms();

  useEffect(() => {
    if (product) {
      setName(product.name);
      setSku(product.sku);
      setDescription(product.description || "");
      setTrackingType(product.trackingType);
      setCategoryId(product.categoryId || "");
      setSerialNumber(product.serialNumber || "");
      setMinQuantity(product.minQuantity?.toString() || "");
      setUnit(product.unit || "");
    } else {
      setName("");
      setSku("");
      setDescription("");
      setTrackingType("QUANTITY");
      setCategoryId("");
      setSerialNumber("");
      setMinQuantity("");
      setUnit("");
    }
    setToRoomId("");
    setInitialQuantity("1");
    setInitialReason("");
  }, [product, open]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const productData = {
      name,
      sku,
      description: description || undefined,
      trackingType,
      categoryId: categoryId || undefined,
      serialNumber: trackingType === "UNIQUE" ? serialNumber || undefined : undefined,
      minQuantity: trackingType === "QUANTITY" && minQuantity ? parseInt(minQuantity) : undefined,
      unit: trackingType === "QUANTITY" ? unit || undefined : undefined,
    };

    const stock: InitialStock | undefined = toRoomId
      ? {
          toRoomId,
          quantity: trackingType === "UNIQUE" ? 1 : parseInt(initialQuantity) || 1,
          reason: initialReason || undefined,
        }
      : undefined;

    onSubmit(productData, stock);
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {product ? "Modifier le produit" : "Nouveau produit"}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="name">Nom</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Clavier USB"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="sku">SKU</Label>
              <Input
                id="sku"
                value={sku}
                onChange={(e) => setSku(e.target.value)}
                placeholder="KB-001"
                required
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Input
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Description du produit"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="trackingType">Type de suivi</Label>
              <select
                id="trackingType"
                value={trackingType}
                onChange={(e) => setTrackingType(e.target.value as "QUANTITY" | "UNIQUE")}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                disabled={!!product}
              >
                <option value="QUANTITY">Quantité</option>
                <option value="UNIQUE">Unique (numéro de série)</option>
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="categoryId">Catégorie</Label>
              <select
                id="categoryId"
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              >
                <option value="">Aucune</option>
                {categories?.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {trackingType === "UNIQUE" && (
            <div className="space-y-2">
              <Label htmlFor="serialNumber">Numéro de série</Label>
              <Input
                id="serialNumber"
                value={serialNumber}
                onChange={(e) => setSerialNumber(e.target.value)}
                placeholder="SN-2024-001"
                required
              />
            </div>
          )}

          {trackingType === "QUANTITY" && (
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="minQuantity">Seuil alerte stock bas</Label>
                <Input
                  id="minQuantity"
                  type="number"
                  min="0"
                  value={minQuantity}
                  onChange={(e) => setMinQuantity(e.target.value)}
                  placeholder="10"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="unit">Unité</Label>
                <Input
                  id="unit"
                  value={unit}
                  onChange={(e) => setUnit(e.target.value)}
                  placeholder="pièces, kg, litres..."
                />
              </div>
            </div>
          )}

          {/* Section stock initial — création uniquement */}
          {!product && (
            <>
              <div className="border-t pt-4">
                <p className="text-sm font-medium mb-3">
                  Stock initial{" "}
                  <span className="text-muted-foreground font-normal">(optionnel)</span>
                </p>
                <div className="space-y-3">
                  <div className="space-y-2">
                    <Label>Zone de destination</Label>
                    <Select value={toRoomId} onValueChange={setToRoomId}>
                      <SelectTrigger>
                        <SelectValue placeholder="Choisir une zone" />
                      </SelectTrigger>
                      <SelectContent>
                        {rooms?.map((r) => (
                          <SelectItem key={r.id} value={r.id}>
                            {r.name}
                            <span className="text-muted-foreground ml-1 text-xs">
                              — {r.buildingName}
                            </span>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  {toRoomId && trackingType === "QUANTITY" && (
                    <div className="space-y-2">
                      <Label htmlFor="initialQuantity">Quantité initiale</Label>
                      <Input
                        id="initialQuantity"
                        type="number"
                        min="1"
                        value={initialQuantity}
                        onChange={(e) => setInitialQuantity(e.target.value)}
                        required
                      />
                    </div>
                  )}

                  {toRoomId && (
                    <div className="space-y-2">
                      <Label htmlFor="initialReason">Raison (optionnel)</Label>
                      <Input
                        id="initialReason"
                        value={initialReason}
                        onChange={(e) => setInitialReason(e.target.value)}
                        placeholder="Livraison initiale, stock de départ..."
                      />
                    </div>
                  )}
                </div>
              </div>
            </>
          )}

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Annuler
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? "En cours..." : product ? "Modifier" : "Créer"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
