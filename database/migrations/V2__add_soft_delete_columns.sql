-- Add soft delete columns to patients table
ALTER TABLE patients
ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Add soft delete columns to ambulances table
ALTER TABLE ambulances
ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Add soft delete columns to requests table
ALTER TABLE requests
ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_patients_deleted ON patients(deleted);
CREATE INDEX IF NOT EXISTS idx_ambulances_deleted ON ambulances(deleted);
CREATE INDEX IF NOT EXISTS idx_requests_deleted ON requests(deleted);
