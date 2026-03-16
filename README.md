# charger-management-service

Spring Boot 4 / Java 21 service for charger inventory, lifecycle, and production configuration management.

## Local run

```bash
mvn spring-boot:run
```

Default port: `8086`

## Main APIs

- `POST /api/v1/chargers`
- `GET /api/v1/chargers`
- `GET /api/v1/chargers/{chargerId}`
- `GET /api/v1/chargers/{chargerId}/settings`
- `PUT /api/v1/chargers/{chargerId}`
- `PATCH /api/v1/chargers/{chargerId}/status`
- `DELETE /api/v1/chargers/{chargerId}`
- `GET /healthz`
- `GET /readyz`
- `GET /actuator/health`
- `GET /actuator/prometheus`
- `GET /api-docs`
- `GET /swagger-ui.html`

## TeamCity pipeline

Provision the TeamCity pipeline with:

```bash
cd /Users/amolsurjuse/development/projects/charger-management-service
TEAMCITY_TOKEN='<token>' ./ci/teamcity/setup_pipeline.sh
```
