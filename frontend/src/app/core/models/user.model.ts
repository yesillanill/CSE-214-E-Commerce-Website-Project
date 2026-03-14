import { UserRole } from "./role.model";

export interface User{
  id: number;
  name: string,
  surname: string,
  email: string;
  password: string;
  role: UserRole;
  phone: string;
  address?: string;
  birthdate: Date
}
