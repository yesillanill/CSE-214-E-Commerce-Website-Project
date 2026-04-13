-- =====================================================
-- Ödeme Sistemi Veritabanı Migration Script
-- Stripe, PayPal, Coinbase Commerce, Kapıda Ödeme
-- PCI DSS uyumlu güvenli kart saklama
-- =====================================================

-- 1. payment_cards tablosu güncellemeleri
-- CVV kolonunu tamamen kaldır (PCI DSS standardı: CVV asla saklanmaz)
ALTER TABLE payment_cards DROP COLUMN IF EXISTS cvv;

-- Mevcut kart numaralarını son 4 haneye dönüştür
UPDATE payment_cards SET card_number = RIGHT(card_number, 4) WHERE LENGTH(card_number) > 4;

-- card_number kolonunu VARCHAR(4) olarak güncelle (sadece son 4 hane)
ALTER TABLE payment_cards ALTER COLUMN card_number TYPE VARCHAR(4);

-- Yeni kolonları ekle
ALTER TABLE payment_cards ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(50) DEFAULT 'STRIPE';
ALTER TABLE payment_cards ADD COLUMN IF NOT EXISTS provider_token VARCHAR(255);
ALTER TABLE payment_cards ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;

-- 2. payments tablosu güncellemeleri (mevcut kolonlar korunuyor)
-- Mevcut: payment_id, order_id, payment_method, payment_status, amount, paid_at
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(50);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS card_id BIGINT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'USD';
ALTER TABLE payments ADD COLUMN IF NOT EXISTS provider_transaction_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS notes TEXT;

-- card_id için foreign key constraint ekle
ALTER TABLE payments ADD CONSTRAINT IF NOT EXISTS fk_payments_card
    FOREIGN KEY (card_id) REFERENCES payment_cards(card_id) ON DELETE SET NULL;

-- payment_id (harici ödeme ID) kolonu zaten yoksa ekle
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_id VARCHAR(255);
