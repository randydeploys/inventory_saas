import { useState } from "react";
import {
  useProducts,
  useLowStock,
  useCreateProduct,
  useUpdateProduct,
  useDeleteProduct,
} from "@/hooks/useProducts";
import { useCreateMovement } from "@/hooks/useMovements";
import { useCategories } from "@/hooks/useCategories";
import { useAuth } from "@/context/AuthContext";
import ProductDialog from "@/components/products/ProductDialog";
import type { InitialStock } from "@/components/products/ProductDialog";
import ConfirmDialog from "@/components/shared/ConfirmDialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Plus, Pencil, Trash2, ChevronLeft, ChevronRight, AlertTriangle } from "lucide-react";
import type { Product } from "@/types/api";
import { useDebounce } from "@/hooks/useDebounce";
import { toast } from "sonner";
import { getErrorMessage } from "@/lib/error";


export default function ProductsPage() {
  const { user } = useAuth();
  const canEdit = user?.role === "ADMIN" || user?.role === "MANAGER";

  // Filtres
  const [categoryId, setCategoryId] = useState("");
  const [trackingType, setTrackingType] = useState("");
  const [page, setPage] = useState(0);
  const [lowStockOnly, setLowStockOnly] = useState(false);

const [search, setSearch] = useState("");
const debouncedSearch = useDebounce(search, 300);

  const { data: categories } = useCategories();
  const { data: paginated, isLoading, error } = useProducts({
    page,
    size: 20,
    search: debouncedSearch || undefined,
    categoryId: categoryId || undefined,
    trackingType: trackingType || undefined,
  });
  const { data: lowStockProducts } = useLowStock();

  const createMutation = useCreateProduct();
  const updateMutation = useUpdateProduct();
  const deleteMutation = useDeleteProduct();
  const createMovementMutation = useCreateMovement();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmTargetId, setConfirmTargetId] = useState<string | null>(null);

  const handleCreate = () => {
    setEditingProduct(null);
    setDialogOpen(true);
  };

  const handleEdit = (product: Product) => {
    setEditingProduct(product);
    setDialogOpen(true);
  };

  const handleDelete = (id: string) => {
    setConfirmTargetId(id);
    setConfirmOpen(true);
  };

  const handleConfirmDelete = () => {
    if (!confirmTargetId) return;
    const id = confirmTargetId;
    setConfirmOpen(false);
    setConfirmTargetId(null);
    deleteMutation.mutate(id, {
      onSuccess: () => toast.success("Produit archivé"),
      onError: (err) => toast.error(getErrorMessage(err)),
    });
  };

  const handleSubmit = (
    data: Parameters<typeof createMutation.mutate>[0],
    initialStock?: InitialStock
  ) => {
    if (editingProduct) {
      updateMutation.mutate(
        { id: editingProduct.id, data },
        {
          onSuccess: () => {
            setDialogOpen(false);
            toast.success("Produit modifié");
          },
          onError: (err) => toast.error(getErrorMessage(err)),
        }
      );
    } else {
      createMutation.mutate(data, {
        onSuccess: (res) => {
          const newProductId = res.data.data.id;
          setDialogOpen(false);

          if (initialStock) {
            createMovementMutation.mutate(
              {
                productId: newProductId,
                type: "IN",
                quantity: initialStock.quantity,
                toRoomId: initialStock.toRoomId,
                reason: initialStock.reason,
              },
              {
                onSuccess: () => toast.success("Produit créé et stock ajouté"),
                onError: (err) =>
                  toast.error(
                    `Produit créé mais le stock initial n'a pas pu être ajouté : ${getErrorMessage(err)}`
                  ),
              }
            );
          } else {
            toast.success("Produit créé");
          }
        },
        onError: (err) => toast.error(getErrorMessage(err)),
      });
    }
  };

  const isLowStock = (product: Product) =>
    product.trackingType === "QUANTITY" &&
    product.minQuantity !== null &&
    product.totalQuantity < product.minQuantity;

  const displayedProducts = lowStockOnly
    ? lowStockProducts ?? []
    : paginated?.content ?? [];
  const showPagination = !lowStockOnly && paginated && paginated.totalPages > 1;

  if (isLoading) return <div>Chargement...</div>;
  if (error) return <div>Erreur lors du chargement</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold">Produits</h1>
          {(lowStockProducts?.length ?? 0) > 0 && (
            <button
              onClick={() => setLowStockOnly(!lowStockOnly)}
              className={`flex items-center gap-1.5 text-sm px-2.5 py-1 rounded-full font-medium transition-colors ${
                lowStockOnly
                  ? "bg-orange-500 text-white"
                  : "bg-orange-100 text-orange-700 hover:bg-orange-200"
              }`}
            >
              <AlertTriangle className="h-3.5 w-3.5" />
              {lowStockProducts!.length} stock bas
            </button>
          )}
        </div>
        {canEdit && (
          <Button onClick={handleCreate}>
            <Plus className="h-4 w-4 mr-2" />
            Nouveau produit
          </Button>
        )}
      </div>

      {/* Filtres */}
      <div className="flex gap-4 mb-6 flex-wrap">
        <Input
          placeholder="Rechercher par nom ou SKU..."
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
          className="max-w-xs"
        />
        <select
          value={categoryId}
          onChange={(e) => {
            setCategoryId(e.target.value);
            setPage(0);
          }}
          className="rounded-md border border-input bg-background px-3 py-2 text-sm"
        >
          <option value="">Toutes les catégories</option>
          {categories?.map((cat) => (
            <option key={cat.id} value={cat.id}>
              {cat.name}
            </option>
          ))}
        </select>
        <select
          value={trackingType}
          onChange={(e) => {
            setTrackingType(e.target.value);
            setPage(0);
          }}
          className="rounded-md border border-input bg-background px-3 py-2 text-sm"
        >
          <option value="">Tous les types</option>
          <option value="QUANTITY">Quantité</option>
          <option value="UNIQUE">Unique</option>
        </select>
      </div>

      {/* Liste des produits */}
      {displayedProducts.length === 0 ? (
        <p className="text-muted-foreground">
          {lowStockOnly ? "Aucun produit en stock bas." : "Aucun produit trouvé."}
        </p>
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {displayedProducts.map((product) => (
              <Card
                key={product.id}
                className={isLowStock(product) ? "border-orange-400" : ""}
              >
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <div className="space-y-1">
                    <CardTitle className="text-lg flex items-center gap-2">
                      {product.name}
                      {isLowStock(product) && (
                        <AlertTriangle className="h-4 w-4 text-orange-500" />
                      )}
                    </CardTitle>
                    <p className="text-xs text-muted-foreground">{product.sku}</p>
                  </div>
                  {canEdit && (
                    <div className="flex gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleEdit(product)}
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleDelete(product.id)}
                      >
                        <Trash2 className="h-4 w-4 text-red-500" />
                      </Button>
                    </div>
                  )}
                </CardHeader>
                <CardContent className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">
                      {product.trackingType === "QUANTITY" ? "Quantité" : "Unique"}
                    </span>
                    <span className="font-medium">
                      {product.trackingType === "QUANTITY"
                        ? `${product.totalQuantity} ${product.unit || ""}`
                        : product.serialNumber}
                    </span>
                  </div>
                  {product.categoryName && (
                    <p className="text-xs text-muted-foreground">
                      Catégorie : {product.categoryName}
                    </p>
                  )}
                  {product.stocks.length > 0 && (
                    <div className="text-xs text-muted-foreground">
                      {product.stocks.map((s) => (
                        <span key={s.roomId} className="mr-3">
                          {s.roomName}: {s.quantity}
                        </span>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            ))}
          </div>

          {/* Pagination */}
          {showPagination && (
            <div className="flex items-center justify-center gap-4 mt-6">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(page - 1)}
                disabled={page === 0}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-sm text-muted-foreground">
                Page {page + 1} / {paginated!.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(page + 1)}
                disabled={page >= paginated!.totalPages - 1}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}

      <ProductDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onSubmit={handleSubmit}
        product={editingProduct}
        isLoading={createMutation.isPending || updateMutation.isPending || createMovementMutation.isPending}
      />

      <ConfirmDialog
        open={confirmOpen}
        onClose={() => { setConfirmOpen(false); setConfirmTargetId(null); }}
        onConfirm={handleConfirmDelete}
        title="Archiver ce produit ?"
        description="Cette action est réversible. Le produit ne sera plus visible dans les listes actives."
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}