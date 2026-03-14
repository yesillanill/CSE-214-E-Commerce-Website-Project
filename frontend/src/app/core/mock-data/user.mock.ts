import { User } from "../models/user.model";

export const USERS: User[] = [
    {
      id: 1,
      name: 'Anıl',
      surname: 'Yeşil',
      email: 'admin@test.com',
      password: '12345678',
      role: 'Admin',
      phone: '905063630247',
      birthdate: new Date('2005-01-14')
    },
    {
      id: 2,
      name: 'Anıl',
      surname: 'Yeşil',
      email: 'corp@test.com',
      password: '12345678',
      role: 'CorporateUser',
      phone: '905063630247',
      birthdate: new Date('2005-01-14')
    },
    {
      id: 3,
      name: 'Anıl',
      surname: 'Yeşil',
      email: 'ind@test.com',
      password: '12345678',
      role: 'IndividualUser',
      phone: '905063630247',
      birthdate: new Date('2005-01-14')
    }
  ];
