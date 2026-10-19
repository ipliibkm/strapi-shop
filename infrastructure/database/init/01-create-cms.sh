#!/bin/sh
set -eu
# PostgreSQL official entrypoint passes POSTGRES_* credentials and calls .sh scripts.
# Password supplied via psql variable; never interpolate secrets into shell SQL.
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v cms_password="$CMS_DB_PASSWORD" <<'EOSQL'
SELECT format('CREATE ROLE cms LOGIN PASSWORD %L', :'cms_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='cms') \gexec
EOSQL
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='cms'" | grep -q 1 || psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -c "CREATE DATABASE cms OWNER cms"
