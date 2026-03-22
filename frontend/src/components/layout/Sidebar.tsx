import { NavLink } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { useLowStock } from "@/hooks/useProducts";
import { Button } from "@/components/ui/button";
import {
  Building2,
  DoorOpen,
  Package,
  ArrowLeftRight,
  Tags,
  LogOut,
  Users,
} from "lucide-react";

const navItems = [
  { to: "/buildings", label: "Bâtiments", icon: Building2, roles: ["ADMIN", "MANAGER", "READER"] },
  { to: "/rooms", label: "Zones", icon: DoorOpen, roles: ["ADMIN", "MANAGER", "READER"] },
  { to: "/categories", label: "Catégories", icon: Tags, roles: ["ADMIN", "MANAGER", "READER"] },
  { to: "/products", label: "Produits", icon: Package, roles: ["ADMIN", "MANAGER", "READER"], showLowStock: true },
  { to: "/movements", label: "Mouvements", icon: ArrowLeftRight, roles: ["ADMIN", "MANAGER", "READER"] },
  { to: "/users", label: "Utilisateurs", icon: Users, roles: ["ADMIN"] },
];

export default function Sidebar() {
  const { user, logout } = useAuth();
  const { data: lowStockProducts } = useLowStock();
  const lowStockCount = lowStockProducts?.length ?? 0;

  return (
    <aside className="w-64 border-r bg-card flex flex-col h-screen">
      <div className="p-6 border-b">
        <h1 className="text-lg font-bold">Inventory SaaS</h1>
        <p className="text-sm text-muted-foreground mt-1">
          {user?.firstName} {user?.lastName}
        </p>
        <span className="text-xs bg-primary/10 text-primary px-2 py-0.5 rounded">
          {user?.role}
        </span>
      </div>

      <nav className="flex-1 p-4 space-y-1">
        {navItems
          .filter((item) => user && item.roles.includes(user.role))
          .map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center justify-between px-3 py-2 rounded-md text-sm transition-colors ${
                  isActive
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                }`
              }
            >
              <div className="flex items-center gap-3">
                <item.icon className="h-4 w-4" />
                {item.label}
              </div>
              {item.showLowStock && lowStockCount > 0 && (
                <span className="bg-orange-500 text-white text-xs font-medium px-1.5 py-0.5 rounded-full">
                  {lowStockCount}
                </span>
              )}
            </NavLink>
          ))}
      </nav>

      <div className="p-4 border-t">
        <Button
          variant="ghost"
          className="w-full justify-start gap-3"
          onClick={logout}
        >
          <LogOut className="h-4 w-4" />
          Déconnexion
        </Button>
      </div>
    </aside>
  );
}