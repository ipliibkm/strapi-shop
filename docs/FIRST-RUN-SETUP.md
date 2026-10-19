# Set up one business

This setup is for development and testing. **Do not use it for a public production system.**

## 1. Check the required tools

Install these tools:
- Docker Engine.
- Docker Compose version 2.
- Python version 3.10 or later.
- Git.

You need an internet connection to build the application images.

## 2. Start the setup

Open a terminal in the repository directory.

Run:

```sh
sh setup.sh
```

The setup asks for these company details:
1. Company name and ID.
2. Email address.
3. Time zone.
4. Language and currency.
5. Address and phone number. These fields are optional.

Check the details. Enter `yes` to save them.

## 3. Let the setup start the services

The script creates two local files:
- `.env` contains generated secrets.
- `config/business.json` contains the company details.

The script starts PostgreSQL, Spring Boot, and Strapi.

The script then checks these health addresses:
- `http://127.0.0.1:8080/actuator/health`
- `http://127.0.0.1:1337/_health`

If a service does not become healthy, the script stops with an error. Read the logs to find the cause.

## 4. Create the first technical administrator

When the script asks, enter `yes` to start Strapi's administrator setup.

Enter the administrator name, email address, and password in the Strapi tool.

This administrator manages Strapi. It is **not** the user account for the custom Angular admin.

Open Strapi at `http://127.0.0.1:1337/admin` after setup.

## 5. Start or stop the services later

Start the existing system and check service health:

```sh
sh setup.sh --start-only
```

Read the service logs:

```sh
docker compose logs -f cms backend
```

Stop the services. This command keeps the database volumes:

```sh
docker compose down
```

## 6. Recover from an error

If setup stops, correct the reported problem. Then run `sh setup.sh` again.

The script does not replace existing company data during a restart.

If administrator creation stops, use the Strapi command:

```sh
docker compose exec cms npm run strapi -- admin:create-user
```

If you delete the database, the local setup state can become incorrect. Check the state before you remove `.setup-state.json`.

## 7. Protect the installation

Do not add `.env`, `config/business.json`, or `.setup-state.json` to Git.

Back up the database, media, and secrets. Store an encrypted backup outside the server.

Do not expose this test setup directly to the internet.

The script does not configure HTTPS, off-site backups, or production authentication.

It does not start the Angular apps. It does not connect Google Calendar, Gmail, or Stripe.

**Test status:** The setup files exist in GitHub. A complete Docker installation test has not passed yet.

For writing rules, read [WRITING-STYLE.md](WRITING-STYLE.md).
