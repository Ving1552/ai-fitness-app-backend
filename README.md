# FitTrack — AI-Powered Fitness Tracking App

A full-stack fitness tracking application built on a **7-service microservices architecture**. Users log workouts, and the app automatically generates AI-powered insights and recommendations for every activity using the Gemini API — delivered asynchronously via Kafka.

---

## How it works

When a user logs an activity (running, cycling, HIIT, etc.), the **Activity Service** validates the user via the **User Service**, saves the activity to MongoDB, and immediately publishes an event to a **Kafka topic**. The **AI Service** consumes that event asynchronously, builds a prompt from the activity data, calls the **Gemini API**, and saves the structured recommendation back to MongoDB. The user can then view their AI insights — including improvements, suggestions, and safety notes — on the AI Insights page.

Authentication supports both **username/password** and **Google OAuth2**. On login, the **Auth Service** issues a short-lived JWT access token and a long-lived refresh token. Every request passes through the **API Gateway**, which validates the JWT reactively before routing to the appropriate service. User IDs are forwarded via the `X-User-Id` header so downstream services never need to decode tokens themselves.

Activities are fetched with **cursor-based pagination** (10 per page, sorted by start time descending), so the activity feed stays fast regardless of how many entries a user has.

---

## Architecture

<img width="1440" height="1220" alt="image" src="https://github.com/user-attachments/assets/efeb5db4-652f-4173-9772-d96651ceef25" />

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot, Spring Cloud |
| Service Discovery | Spring Cloud Eureka |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Config | Spring Cloud Config Server |
| Messaging | Apache Kafka (Aiven, SSL/TLS) |
| AI | Google Gemini API |
| Auth | Spring Security, OAuth2, JWT |
| Databases | PostgreSQL (Supabase), MongoDB |
| Frontend | React, Vite, Tailwind CSS, Axios |

---

## Services

| Service | Port | Description |
|---|---|---|
| API Gateway | 8080 | Routes requests, validates JWT reactively |
| Auth Service | 8084 | Register, login, Google OAuth2, JWT + refresh tokens |
| User Service | 8081 | User profile management |
| Activity Service | 8082 | Log and retrieve activities with pagination |
| AI Service | 8083 | Kafka consumer, Gemini API, recommendations |
| Eureka | 8761 | Service registry and discovery |
| Config Server | 8888 | Centralized YAML config for all services |

---

## Frontend Pages

- **Login** — username/password or Google OAuth2
- **Register** — create account
- **Dashboard** — overview
- **Activities** — log workouts, view paginated history (10/page)
- **AI Insights** — Gemini-generated recommendations with improvements, suggestions, and safety notes per activity

All pages behind `/dashboard`, `/activities`, `/ai-insights` are protected routes — unauthenticated users are redirected to `/login`. Auth state is persisted in `localStorage` via `AuthContext`.

---

## Setup

### Prerequisites
- Java 17+
- Node.js 18+
- Maven
- MongoDB (local or Atlas free tier)
- PostgreSQL (Supabase free tier)
- Kafka (Aiven free trial or local)
- Google Gemini API key (free at aistudio.google.com)
- Google OAuth2 credentials (Google Cloud Console)

### 1. Clone the repos
```bash
git clone https://github.com/Ving1552/ai-fitness-app-backend.git
git clone https://github.com/Ving1552/ai-fitness-app-frontend.git
```

### 2. Configure the backend
Copy the example configs and fill in your credentials:
```bash
cd ai-fitness-app-backend/configserver/src/main/resources
cp -r config-example config
```
Fill in all `${VARIABLE}` placeholders in each YAML file under `config/` with your actual values. See `.env.example` at the root for the full list of required variables.

### 3. Start services in order
Each service is a standard Spring Boot app. Start them in this order to avoid dependency issues:

```
1. Config Server    (port 8888)
2. Eureka           (port 8761)
3. Auth Service     (port 8084)
4. User Service     (port 8081)
5. Activity Service (port 8082)
6. AI Service       (port 8083)
7. API Gateway      (port 8080)
```

Run each with:
```bash
cd <service-folder>
./mvnw spring-boot:run
```

### 4. Start the frontend
```bash
cd ai-fitness-app-frontend/fitness-frontend
npm install
npm run dev
```
Frontend runs on `http://localhost:3000` by default.

---
