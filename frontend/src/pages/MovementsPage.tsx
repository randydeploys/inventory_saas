import { useState } from "react";
import { useMovements, useCreateMovement } from "@/hooks/useMovements";
import { useAuth } from "@/context/AuthContext";
import MovementDialog from "@/components/movements/MovementDialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { getErrorMessage } from "@/lib/error";
import {
  Plus,
  ChevronLeft,
  ChevronRight,
  ArrowDownToLine,
  ArrowUpFromLine,
  ArrowLeftRight,
} from "lucide-react";
import { toast } from "sonner";

const typeIcons = {
  IN: <ArrowDownToLine className="h-4 w-4 text-green-500" />,
  OUT: <ArrowUpFromLine className="h-4 w-4 text-red-500" />,
  TRANSFER: <ArrowLeftRight className="h-4 w-4 text-blue-500" />,
};

const typeLabels = {
  IN: "Entrée",
  OUT: "Sortie",
  TRANSFER: "Transfert",
};

export default function MovementsPage() {
  const { user } = useAuth();
  const canCreate = user?.role === "ADMIN" || user?.role === "MANAGER";

  const [type, setType] = useState("");
  const [page, setPage] = useState(0);
  const [formError, setFormError] = useState("");

  const { data: paginated, isLoading, error: queryError } = useMovements({
    page,
    size: 20,
    type: type || undefined,
  });

  const createMutation = useCreateMovement();
  const [dialogOpen, setDialogOpen] = useState(false);

  const handleSubmit = (data: Parameters<typeof createMutation.mutate>[0]) => {
    setFormError("");
    createMutation.mutate(data, {
      onSuccess: () => {
        setDialogOpen(false);
        toast.success("Mouvement créé");
      },
      onError: (err) => {
        setFormError(getErrorMessage(err));
        toast.error(getErrorMessage(err));
      },
    });
  };

  const handleOpen = () => {
    setFormError("");
    setDialogOpen(true);
  };

  if (isLoading) return <div>Chargement...</div>;
  if (queryError) return <div>Erreur lors du chargement</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Mouvements de stock</h1>
        {canCreate && (
          <Button onClick={handleOpen}>
            <Plus className="h-4 w-4 mr-2" />
            Nouveau mouvement
          </Button>
        )}
      </div>

      {/* Filtre par type */}
      <div className="mb-6">
        <select
          value={type}
          onChange={(e) => {
            setType(e.target.value);
            setPage(0);
          }}
          className="rounded-md border border-input bg-background px-3 py-2 text-sm"
        >
          <option value="">Tous les types</option>
          <option value="IN">Entrée</option>
          <option value="OUT">Sortie</option>
          <option value="TRANSFER">Transfert</option>
        </select>
      </div>

      {/* Liste des mouvements */}
      {paginated?.content.length === 0 ? (
        <p className="text-muted-foreground">Aucun mouvement enregistré.</p>
      ) : (
        <>
          <div className="space-y-3">
            {paginated?.content.map((movement) => (
              <Card key={movement.id}>
                <CardContent className="flex items-center gap-4 py-4">
                  <div className="flex-shrink-0">{typeIcons[movement.type]}</div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-medium">{movement.productName}</span>
                      <span className="text-xs text-muted-foreground">
                        {movement.productSku}
                      </span>
                    </div>
                    <div className="text-sm text-muted-foreground">
                      {movement.type === "IN" && (
                        <span>→ {movement.toRoomName}</span>
                      )}
                      {movement.type === "OUT" && (
                        <span>{movement.fromRoomName} →</span>
                      )}
                      {movement.type === "TRANSFER" && (
                        <span>
                          {movement.fromRoomName} → {movement.toRoomName}
                        </span>
                      )}
                      {movement.reason && (
                        <span className="ml-2">— {movement.reason}</span>
                      )}
                    </div>
                  </div>

                  <div className="text-right flex-shrink-0">
                    <span className="font-medium">
                      {movement.type === "IN" ? "+" : movement.type === "OUT" ? "-" : ""}
                      {movement.quantity}
                    </span>
                    <p className="text-xs text-muted-foreground">
                      {typeLabels[movement.type]}
                    </p>
                  </div>

                  <div className="text-right flex-shrink-0 text-xs text-muted-foreground">
                    <p>{new Date(movement.createdAt).toLocaleDateString("fr-FR")}</p>
                    <p>
                      {new Date(movement.createdAt).toLocaleTimeString("fr-FR", {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </p>
                    <p>{movement.performedByName}</p>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>

          {paginated && paginated.totalPages > 1 && (
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
                Page {page + 1} / {paginated.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(page + 1)}
                disabled={page >= paginated.totalPages - 1}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}

      <MovementDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onSubmit={handleSubmit}
        isLoading={createMutation.isPending}
        error={formError}
      />
    </div>
  );
}