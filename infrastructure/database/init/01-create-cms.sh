#!/bin/sh
set -eu
# Called once when PostgreSQL volume is first initialized.
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v password="$CMS_DB_PASSWORD" <<'EOSQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'cms', :'password') WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'cms')\gexec
EOSQL
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "CREATE DATABASE cms OWNER cms"
