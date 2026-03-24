import { useState } from "react";
import {
  useCategories,
  useCreateCategory,
  useUpdateCategory,
  useDeleteCategory,
} from "@/hooks/useCategories";
import { useAuth } from "@/context/AuthContext";
import CategoryDialog from "@/components/categories/CategoryDialog";
import ConfirmDialog from "@/components/shared/ConfirmDialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Plus, Pencil, Trash2 } from "lucide-react";
import type { Category } from "@/types/api";
import { toast } from "sonner";
import { getErrorMessage } from "@/lib/error";

export default function CategoriesPage() {
  const { user } = useAuth();
  const canEdit = user?.role === "ADMIN" || user?.role === "MANAGER";

  const { data: categories, isLoading, error } = useCategories();
  const createMutation = useCreateCategory();
  const updateMutation = useUpdateCategory();
  const deleteMutation = useDeleteCategory();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmTargetId, setConfirmTargetId] = useState<string | null>(null);

  const handleCreate = () => {
    setEditingCategory(null);
    setDialogOpen(true);
  };

  const handleEdit = (category: Category) => {
    setEditingCategory(category);
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
      onSuccess: () => toast.success("Catégorie archivée"),
      onError: (err) => toast.error(getErrorMessage(err)),
    });
  };

  const handleSubmit = (data: { name: string; color: string }) => {
    if (editingCategory) {
      updateMutation.mutate(
        { id: editingCategory.id, data },
         {
        onSuccess: () => {
          setDialogOpen(false);
          toast.success("Catégorie modifiée");
        },
        onError: (err) => toast.error(getErrorMessage(err)),
      }
      );
    } else {
      createMutation.mutate(data, {
        onSuccess: () => {
          setDialogOpen(false);
          toast.success("Catégorie créée");
        },
        onError: (err) => toast.error(getErrorMessage(err)),
      });
    }
  };

  if (isLoading) return <div>Chargement...</div>;
  if (error) return <div>Erreur lors du chargement</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Catégories</h1>
        {canEdit && (
          <Button onClick={handleCreate}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle catégorie
          </Button>
        )}
      </div>

      {categories?.length === 0 ? (
        <p className="text-muted-foreground">Aucune catégorie pour le moment.</p>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {categories?.map((category: Category) => (
            <Card key={category.id}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <div className="flex items-center gap-3">
                  <div
                    className="w-4 h-4 rounded-full"
                    style={{ backgroundColor: category.color }}
                  />
                  <CardTitle className="text-lg">{category.name}</CardTitle>
                </div>
                {canEdit && (
                  <div className="flex gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleEdit(category)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleDelete(category.id)}
                    >
                      <Trash2 className="h-4 w-4 text-red-500" />
                    </Button>
                  </div>
                )}
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  {category.color}
                </p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <CategoryDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onSubmit={handleSubmit}
        category={editingCategory}
        isLoading={createMutation.isPending || updateMutation.isPending}
      />

      <ConfirmDialog
        open={confirmOpen}
        onClose={() => { setConfirmOpen(false); setConfirmTargetId(null); }}
        onConfirm={handleConfirmDelete}
        title="Archiver cette catégorie ?"
        description="Les produits liés conserveront leur catégorie mais elle n'apparaîtra plus dans les listes actives."
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}