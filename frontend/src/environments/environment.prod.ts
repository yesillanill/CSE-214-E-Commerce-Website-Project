// Docker/Production environment configuration
// Uses empty string for apiUrl so requests go to the same origin (nginx reverse proxy)
// GÜVENLİK: Stripe publishable key frontend'de kullanılabilir (public key'dir)
// GÜVENLİK: Secret key'ler asla frontend'de tutulmaz
export const environment = {
  production: true,
  apiUrl: '',

  // Stripe Publishable Key — bu PUBLIC key'dir, frontend'de kullanılması güvenlidir
  // GÜVENLİK: Secret key (sk_test_xxx) asla frontend'e konmaz
  stripePublishableKey: 'pk_test_51TLmXwFenUoMJgzK0N8NRK7LDQoZGetPVa29t7cciiZGe3mjGg0vqIsMzS53wmBhaKMhEg3REiXDoUBglIhSqRu600HROlWNnl',
};
