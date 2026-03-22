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
import { useCategories } from "@/hooks/useCategories";
import type { Product } from "@/types/api";

interface ProductDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: {
    name: string;
    sku: string;
    description?: string;
    trackingType: "QUANTITY" | "UNIQUE";
    categoryId?: string;
    serialNumber?: string;
    minQuantity?: number;
    unit?: string;
  }) => void;
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

  const { data: categories } = useCategories();

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
  }, [product, open]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({
      name,
      sku,
      description: description || undefined,
      trackingType,
      categoryId: categoryId || undefined,
      serialNumber: trackingType === "UNIQUE" ? serialNumber || undefined : undefined,
      minQuantity: trackingType === "QUANTITY" && minQuantity ? parseInt(minQuantity) : undefined,
      unit: trackingType === "QUANTITY" ? unit || undefined : undefined,
    });
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

          {/* Champs conditionnels selon le tracking type */}
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