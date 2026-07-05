-- Ajouter la colonne status nullable d'abord
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS status VARCHAR(20);

-- Mettre à jour les enregistrements existants
UPDATE "user" SET status = 'APPROVED' WHERE status IS NULL;

-- Ajouter la contrainte NOT NULL et check
ALTER TABLE "user" ALTER COLUMN status SET NOT NULL;
ALTER TABLE "user" ADD CONSTRAINT IF NOT EXISTS user_status_check 
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));
