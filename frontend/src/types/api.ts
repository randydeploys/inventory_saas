// Match exactement ton ApiResponse<T> Java
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors: string[];
}

// Match ton UserResponse Java
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: "ADMIN" | "MANAGER" | "READER";
  isActive: boolean;
  tenantId: string;
}

// Match ton AuthResponse Java
export interface AuthResponse {
  user: User;
}

// Match ton BuildingResponse Java
export interface Building {
  id: string;
  name: string;
  address: string;
  activeProductCount: number;
  createdAt: string;
  updatedAt: string;
}

// Match ton RoomResponse Java
export interface Room {
  id: string;
  buildingId: string;
  buildingName: string;
  name: string;
  description: string;
  activeProductCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: string;
  name: string;
  color: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductStock {
  roomId: string;
  roomName: string;
  quantity: number;
}

export interface Product {
  id: string;
  name: string;
  sku: string;
  description: string;
  trackingType: "QUANTITY" | "UNIQUE";
  categoryId: string | null;
  categoryName: string | null;
  serialNumber: string | null;
  minQuantity: number | null;
  unit: string | null;
  totalQuantity: number;
  stocks: ProductStock[];
  createdAt: string;
  updatedAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  createdAt: string;
  updatedAt: string;
}

export interface Movement {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  type: "IN" | "OUT" | "TRANSFER";
  quantity: number;
  fromRoomId: string | null;
  fromRoomName: string | null;
  toRoomId: string | null;
  toRoomName: string | null;
  reason: string | null;
  performedBy: string;
  performedByName: string;
  createdAt: string;
}