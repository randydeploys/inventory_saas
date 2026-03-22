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
  createdAt: string;
  updatedAt: string;
}

// Match ton RoomResponse Java
export interface Room {
  id: string;
  buildingId: string;
  name: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}