# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build, run, and test commands

- Build project (skip tests):
  - `mvn clean package -DskipTests`
- Run in development mode (default profile is `dev`):
  - `mvn spring-boot:run`
- Run with explicit profile:
  - `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
  - `mvn spring-boot:run -Dspring-boot.run.profiles=prod`
- Run all tests:
  - `mvn test`
- Run a single test class:
  - `mvn -Dtest=ClassName test`
- Run a single test method:
  - `mvn -Dtest=ClassName#methodName test`

Notes:
- There is currently no dedicated lint command configured in `pom.xml` (no Checkstyle/PMD/SpotBugs plugin).
- `src/test` is currently absent; test commands are standard Maven commands to use once tests are added.

## High-level architecture

This is a server-rendered blog application using Spring MVC + Thymeleaf templates, with MyBatis annotation-based mappers for persistence.

### Request flow and layering

- Entry point: `MyBlogApplication` bootstraps Spring Boot.
- Controllers under `com.murasame.controller` handle HTTP routes and prepare template models or JSON responses.
- Services under `com.murasame.service` and `com.murasame.service.impl` contain business logic.
- Mappers under `com.murasame.mapper` perform SQL via MyBatis annotations (`@Select`, `@Insert`, `@Update`, etc.).
- Entities/DTO/VO split:
  - `entity`: DB-facing models (`Blogs`, `Comments`, `Users`, `Tag`, `BlogsBin`)
  - `domain.dto`: API/transport data structures
  - `domain.vo`: view-oriented payloads for template rendering

### Rendering model

- Thymeleaf templates are in `src/main/resources/templates`.
- Static assets are in `src/main/resources/static` (`css`, `js`, icons/images).
- Main pages:
  - `index.html`: blog list, pagination, hot blogs, recent comments, weather widget
  - `readBlog.html`: blog detail, comments, like/read interactions
  - `writeBlog.html`: create/edit blog
- Fragment composition is used (`templates/fragment/header.html`, `templates/fragment/weather.html`).

### Core domain behaviors

- Blog CRUD + recycle bin behavior:
  - Delete moves blog rows from `blogs` to `blogsBin`; recover moves them back.
- Tag system:
  - Blog tags are stored as JSON (`TagWrapper`) in `blogs.t_id`.
  - `TagWrapperTypeHandler` serializes/deserializes between JSON and Java object.
  - Tag filtering uses MySQL JSON query (`JSON_CONTAINS`).
- Interaction metrics:
  - Read count and like count are incremented/decremented via dedicated mapper updates.
- Comment system:
  - Comment tree and recent comment list are assembled in service/controller layer for rendering.
- Weather component:
  - `/api/weather/*` endpoints call `SeniverseApiClient`.
  - `WeatherServiceImpl` includes a simple in-memory static cache (1 hour) keyed by location.

### Configuration and environment

- Configuration files:
  - `application.yml`: datasource defaults.
  - `application-dev.yml`: dev profile, server port, weather API config.
  - `application-prod.yml`: disables springdoc API docs.
- Security:
  - `SecurityConfiguration` currently permits all requests and disables CSRF.
  - MyBatis mapper scanning is configured in security config via `@MapperScan("com.murasame.mapper")`.
- API docs:
  - Springdoc OpenAPI is enabled via dependency + `SwaggerConfiguration` (disabled in prod profile).

## Dependency stack (important for future changes)

- Spring Boot 3.5.x
- Java 17
- Spring MVC + Thymeleaf
- Spring Security
- MyBatis Spring Boot Starter
- MySQL connector
- springdoc-openapi
- Jsoup + CommonMark (HTML sanitizing / markdown-html handling)

## Practical guidance for edits

- Keep controller/service/mapper separation consistent; SQL belongs in mapper interfaces.
- For tag-related changes, update both `TagWrapper` shape and `TagWrapperTypeHandler`/SQL JSON usage together.
- For page changes, verify both template (`templates`) and corresponding static JS/CSS (`static/js`, `static/css`) because interactive behavior is split across both.
- For behavior differences between environments, check both `application-dev.yml` and `application-prod.yml` rather than only `application.yml`.
