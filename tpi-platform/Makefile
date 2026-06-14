COMPOSE = docker compose --env-file .env -f infra/docker-compose.yml
ROOT_DIR = ..

build:
	cd $(ROOT_DIR) && mvn clean package -DskipTests

test:
	cd $(ROOT_DIR) && mvn test

up:
	$(COMPOSE) up -d

up-build:
	cd $(ROOT_DIR) && mvn clean package -DskipTests
	$(COMPOSE) up -d --build

down:
	$(COMPOSE) down

logs:
	$(COMPOSE) logs -f

ps:
	$(COMPOSE) ps

status:
	$(COMPOSE) ps

reset:
	$(COMPOSE) down -v

