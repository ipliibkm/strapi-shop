#!/bin/sh
set -eu

root_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
config_file=$(mktemp /private/var/folders/zk/13r8g18x70g3yfsqc4153zgdy84g11/T/opencode/compose-prod.XXXXXX.json)
trap 'rm -f "$config_file"' EXIT INT TERM

cd "$root_dir"
docker compose \
  --env-file config/development.env.example \
  config --quiet
docker compose \
  --env-file config/production.env.example \
  config --quiet
docker compose \
  --env-file config/production.env.example \
  config --format json >"$config_file"

python3 - "$config_file" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as source:
    config = json.load(source)

services = config["services"]
traefik = services["traefik"]
published = sorted(int(port["published"]) for port in traefik.get("ports", []))
assert published == [80, 443], f"Traefik publishes unexpected ports: {published}"

for name, service in services.items():
    if name != "traefik":
        assert not service.get("ports"), f"{name} publishes a production port"

assert "@sha256:" in traefik["image"], "The Traefik image is not pinned by digest"
commands = set(traefik["command"])
assert "--providers.docker.exposedbydefault=false" in commands

storefront_labels = services["storefront"]["labels"]
admin_labels = services["admin"]["labels"]
assert storefront_labels["traefik.enable"] == "true"
assert admin_labels["traefik.enable"] == "true"
assert storefront_labels["traefik.http.routers.storefront.middlewares"] == "https-redirect@file"
assert admin_labels["traefik.http.routers.admin.middlewares"] == "https-redirect@file"
assert storefront_labels["traefik.http.routers.storefront-secure.middlewares"] == "security-headers@file"
assert admin_labels["traefik.http.routers.admin-secure.middlewares"] == "security-headers@file"
assert services["cms"]["labels"]["traefik.enable"] == "false"
assert services["storybook"]["labels"]["traefik.enable"] == "false"
assert services["backend"]["environment"]["SERVER_SERVLET_SESSION_COOKIE_SECURE"] == "true"
assert services["backend"]["environment"]["PLATFORM_SETUP_ENABLED"] == "false"

print("The production Compose configuration passed the exposure checks.")
PY

grep -q "contentSecurityPolicy" infrastructure/traefik/dynamic/production.yaml
grep -q "stsSeconds: 31536000" infrastructure/traefik/dynamic/production.yaml
