import { useState, useEffect } from "react";
import { useTenant, useUpdateTenant } from "@/hooks/useTenant";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "sonner";
import { getErrorMessage } from "@/lib/error";

export default function TenantPage() {
  const { data: tenant, isLoading } = useTenant();
  const updateMutation = useUpdateTenant();

  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");

  useEffect(() => {
    if (tenant) {
      setName(tenant.name);
      setSlug(tenant.slug);
    }
  }, [tenant]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateMutation.mutate(
      { name, slug },
      {
        onSuccess: () => toast.success("Paramètres mis à jour"),
        onError: (err) => toast.error(getErrorMessage(err)),
      }
    );
  };

  if (isLoading) return <div>Chargement...</div>;

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Paramètres du tenant</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Informations de votre organisation
        </p>
      </div>

      <Card className="max-w-lg">
        <CardHeader>
          <CardTitle>Informations générales</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Nom de l'organisation</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="slug">Slug (identifiant URL)</Label>
              <Input
                id="slug"
                value={slug}
                onChange={(e) => setSlug(e.target.value)}
                pattern="^[a-z0-9-]+$"
                title="Lettres minuscules, chiffres et tirets uniquement"
                required
              />
              <p className="text-xs text-muted-foreground">
                Lettres minuscules, chiffres et tirets uniquement
              </p>
            </div>
            <div className="pt-2">
              <Button type="submit" disabled={updateMutation.isPending}>
                {updateMutation.isPending ? "Enregistrement..." : "Enregistrer"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <div className="mt-6 text-xs text-muted-foreground">
        <p>Créé le {tenant && new Date(tenant.createdAt).toLocaleDateString("fr-FR")}</p>
        <p>Modifié le {tenant && new Date(tenant.updatedAt).toLocaleDateString("fr-FR")}</p>
      </div>
    </div>
  );
}
