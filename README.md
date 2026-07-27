# Yu-Gi-Oh! Combo Search

A full-stack card search and combo simulator for exploring legal Yu-Gi-Oh! card interactions. Search the card database, inspect card details, follow suggested combo branches, pay material costs, and track cards as they move between simulated zones.

> This is an unofficial fan project. Yu-Gi-Oh! is a trademark of its respective owners. Card data and images are provided by the [YGOPRODeck API](https://ygoprodeck.com/api-guide/).

## Live App

Access the deployed project at [**yugioh-combo.vercel.app**](https://yugioh-combo.vercel.app/).

> **Cold-start notice:** The application may need **6-8 minutes** to start after a period of inactivity. If it does not respond immediately, leave it open and try again after the backend finishes starting.

You can also view the repository's latest development history on the [GitHub Activity page](https://github.com/AimeLih/Yugioh-Combo/activity).

## Features

- Search cards by partial name or exact name
- View card text, stats, type, archetype, artwork, and once-per-turn information
- Explore ranked combo starters, extenders, and enders
- See why a route is available, when it can be used, its destination, and its cost
- Select legal Fusion Materials and other effect costs
- Simulate the field, Graveyard, banished cards, and face-up Extra Deck
- Advance turns and phases to unlock delayed effects
- Step backward through a combo while restoring the previous game state
- Enforce a conservative subset of Official Rulebook Version 10

See [RULEBOOK_COMPLIANCE.md](RULEBOOK_COMPLIANCE.md) for the rules currently modeled and the mechanics intentionally excluded.

## Tech Stack

- **Frontend:** React 19, Vite 8
- **Backend:** Java 17, Spring Boot 4, Spring Data JPA
- **Database:** PostgreSQL 16
- **Card data:** YGOPRODeck API
- **Testing:** JUnit 5, Mockito

## Project Structure

```text
.
├── frontend/                  # React/Vite client
├── src/main/java/             # Spring Boot API and combo engine
├── src/main/resources/        # Application configuration
├── src/test/                  # Backend tests
├── docker-compose.yml         # Local PostgreSQL service
├── Dockerfile                 # Production backend image
└── RULEBOOK_COMPLIANCE.md     # Supported rule behavior
```

## Getting Started

### Prerequisites

- Java 17
- Node.js and npm
- PostgreSQL 16, or Docker Desktop

### 1. Configure the database

Create a PostgreSQL database, then copy the example environment file:

```bash
cp .env.example .env
```

On PowerShell:

```powershell
Copy-Item .env.example .env
```

Set the connection values in `.env`:

```dotenv
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yugioh
SPRING_DATASOURCE_USERNAME=your_database_user
SPRING_DATASOURCE_PASSWORD=your_database_password
SPRING_CACHE_TYPE=simple
```

To use the included Docker Compose service, first set `POSTGRES_PASSWORD` in `docker-compose.yml` to the same password used in `.env`, then run:

```bash
docker compose up -d db
```

### 2. Start the backend

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts at `http://localhost:8080`.

### 3. Import card data

With the backend running against an empty database, import the current card catalog:

```bash
curl -X POST http://localhost:8080/yugioh/import
```

The import can take a moment because it fetches and stores card details and images from YGOPRODeck.

### 4. Start the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

The frontend uses `http://localhost:8080` by default. To point it at another backend, create `frontend/.env.local`:

```dotenv
VITE_API_URL=https://your-api.example.com
```

## Using the App

1. Search for a card by name.
2. Select a result to start a combo route.
3. Choose an available extender or ender.
4. Select required materials when the route has a cost.
5. Activate eligible cards from simulated zones or advance the phase when an effect is timing-locked.
6. Use **Back** to explore another branch.

The combo engine only presents routes it can support from card text and the modeled game state. Synchro, Xyz, Link, and Ritual endpoints are currently hidden rather than guessed.

## API

All endpoints use the `/yugioh` prefix.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/import` | Import card data from YGOPRODeck |
| `GET` | `/card?name=...` | Find one card by exact name |
| `GET` | `/card/substring?name=...` | Search cards by partial name |
| `GET` | `/card/all` | Return every stored card |
| `GET` | `/card/image?name=...` | Return a card image URL |
| `GET` | `/card/combos?name=...&zone=...` | Find supported combo routes |
| `GET` | `/card/fusion-materials?source=...&target=...` | Plan legal Fusion Materials |
| `GET` | `/card/cost-materials?source=...&target=...` | Plan legal effect costs |
| `GET` | `/card/pattern?name=...` | Classify extender behavior |
| `GET` | `/card/onceprturn?name=...` | Inspect once-per-turn wording |
| `PUT` | `/card/update/database` | Refresh stored card metadata |
| `PUT` | `/card/update` | Recalculate card weights |
| `PUT` | `/card/update/zero` | Reset all card weights |

Example:

```bash
curl "http://localhost:8080/yugioh/card/combos?name=Branded%20Fusion"
```

## Tests and Builds

Run the backend tests:

```bash
./mvnw test
```

On Windows, use `.\mvnw.cmd test`.

Check and build the frontend:

```bash
cd frontend
npm run lint
npm run build
```

Build the backend container:

```bash
docker build -t yugioh-combo-search .
```

## Current Scope

The simulator validates general rulebook behavior and parsed card text; it is not a tournament judge. Individual rulings, errata, Forbidden/Limited lists, and tournament policy are outside the current scope.
