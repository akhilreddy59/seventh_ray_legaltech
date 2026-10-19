ALTER TABLE public.contracts
    ADD COLUMN IF NOT EXISTS owner_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'contracts_owner_id_fkey'
          AND conrelid = 'public.contracts'::regclass
    ) THEN
        ALTER TABLE public.contracts
            ADD CONSTRAINT contracts_owner_id_fkey
            FOREIGN KEY (owner_id) REFERENCES auth.users(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_contracts_owner_created_at
    ON public.contracts (owner_id, created_at DESC);
