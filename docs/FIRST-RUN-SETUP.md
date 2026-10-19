# Single-business first-run setup

This is an initial **development/staging bootstrap**, not a production one-click deployment. Requires Docker Engine + Compose v2, Python 3.10+, internet access to build images and checkout the repository. Clone repository, then run `sh setup.sh` or `python3 scripts/setup.py`. The script prompts for public company/brand name, slug, contact email, timezone, locale, currency, optional address and phone, then asks for confirmation.

It creates `.env` containing cryptographic random secrets and `config/business.json`, chmod 0600 via exclusive creation (files are untracked), runs `docker compose up -d --build`, checks the backend and Strapi health endpoints, then invokes Strapi's supported interactive admin:create-user CLI to create the first **technical Strapi admin**. The admin password is entered into the CLI rather than included in shell command arguments or persisted by the setup script. CMS bootstrap seeds business details as a Strapi single type on the first startup only. Restarting never overwrites edited business data.

Commands:
```sh
sh setup.sh
sh setup.sh --start-only
sh setup.sh --skip-admin
docker compose logs -f cms backend
docker compose down                 # retain volumes
```

**Important limitations and recovery:**
- A partial setup retains .env and business.json for resumability; running again resumes Compose startup and health checks.
- If Strapi admin CLI succeeded but script crashed before writing state, retrying creation may report account exists. It must never silently reset credentials; verify account manually.
- If the app's database was wiped but the `.setup-state.json` file persists, delete that local state file to rerun initial admin creation.
- `config/business.json` initializes a CMS single type, but updates after first boot must be done through authorized content editing workflows (future custom Angular admin).
- The setup currently starts **backend, Strapi and database only**. Production TLS, reverse proxy, custom Angular admin onboarding, backups, OIDC, Stripe and Google are not configured.
- Never run on a public server as-is; ports are loopback-bound and technical CMS admin still exists. Configure private hosting and production ingress/access first.
- Stop and investigate unhealthy containers; health checks alone do not prove migrations/business readiness.
- Never commit `.env`, `config/business.json` or state files. Keep encrypted off-host backups of the data and secrets; losing secrets may make persisted sessions/data inaccessible.
- Strapi API and Document Service compatibility, Docker build and admin CLI have not yet been validated end-to-end in CI.
