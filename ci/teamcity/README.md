# TeamCity pipeline setup

This folder contains an idempotent script to provision the `charger-management-service` TeamCity pipeline using the TeamCity REST API.

## Prerequisites
- TeamCity server running at `http://localhost:8111`
- Super-user token or personal access token
- `jq` installed
- A reachable git repository for `charger-management-service`

## Usage
```bash
cd /Users/amolsurjuse/development/projects/charger-management-service
TEAMCITY_TOKEN='<token>' ./ci/teamcity/setup_pipeline.sh
```

## Optional environment overrides
- `TEAMCITY_URL` default `http://localhost:8111`
- `TEAMCITY_PARENT_PROJECT_ID` default `Amy`
- `TEAMCITY_PROJECT_ID` default `Amy_ChargerManagementService`
- `TEAMCITY_BUILD_TYPE_ID` default `Amy_ChargerManagementService_Build`
- `CHARGER_MANAGEMENT_GIT_URL` default `https://github.com/amolsurjuse/charger-management-service`
- `CHARGER_MANAGEMENT_GIT_BRANCH` default `main`
- `CHARGER_MANAGEMENT_DOCKER_IMAGE` default `amolsurjuse/charger-management-service`
