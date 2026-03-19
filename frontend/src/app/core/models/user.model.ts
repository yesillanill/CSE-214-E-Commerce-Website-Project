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
  birthdate?: Date;
  street?: string;
  city?: string;
  postalCode?: string;
  country?: string;
  gender?: string;
  membershipType?: string;
  storeName?: string;
  companyName?: string;
  taxNumber?: string;
  taxOffice?: string;
  companyAddress?: string;
}
