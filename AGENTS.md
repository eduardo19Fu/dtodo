# Repository Guidelines

## Project Structure & Module Organization

This repository contains two applications. `Frontend/` is an Angular 11 client; feature code lives under `src/app/components`, shared API access under `src/app/services`, domain types under `src/app/models` and `src/app/dtos`, and static files under `src/assets`. Unit tests sit beside source files as `*.spec.ts`; browser tests are in `Frontend/e2e/`.

`Backend/dtodo-backend/` is a Java 8 Spring Boot service. Production code is organized by responsibility beneath `src/main/java/xyz/pangosoft/dtodo/` (`controller`, `service`, `repository`, `model`, `dto`, and `fel`). Tests mirror that package structure in `src/test/java`. SQL changes are kept in `Backend/dtodo-backend/sql/` or `src/main/resources/sql/`; report templates are in `src/main/resources/reports/`.

## Build, Test, and Development Commands

- `cd Frontend && npm ci`: install the locked frontend dependencies.
- `npm start`: serve Angular locally with live reload.
- `npm run build`: compile into `Frontend/dist/frontend`.
- `npm test`: run Jasmine unit tests through Karma.
- `npm run lint`: apply the configured TSLint and Codelyzer rules.
- `npm run e2e`: run the legacy Protractor suite.
- `cd Backend/dtodo-backend && .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev`: run the API on Windows using the development profile.
- `.\mvnw.cmd test` or `.\mvnw.cmd clean package`: run JUnit tests or build the backend JAR.

## Coding Style & Naming Conventions

Follow existing formatting: four-space Java indentation and two-space TypeScript/HTML indentation. Java types use `PascalCase`, methods and fields use `camelCase`, and interfaces retain the established `I...Service`/`I...Repository` pattern. Angular files use kebab-case suffixes such as `create-nota.component.ts`; classes use `PascalCase`. TSLint requires single quotes, semicolons, braces, and a 140-character line limit.

## Testing Guidelines

Name frontend tests `*.spec.ts` and backend tests `*Test.java`. Add focused tests beside changed components or in the matching Java package. No coverage threshold is configured; prioritize service logic, controllers, FEL XML behavior, and regression paths. Run both relevant test suites before opening a pull request.

## Commit & Pull Request Guidelines

Recent commits use concise Spanish, present-tense summaries (for example, `Agrega script SQL correctivo...`). Keep each commit scoped to one change. Pull requests should explain behavior and configuration changes, link the issue, list verification commands, and include screenshots for UI work. Call out SQL migrations and report-template updates explicitly.

## Security & Configuration

Do not commit new credentials, private keys, certificates, production database values, or customer uploads. Treat `application-*.properties`, `resources/keystore/`, and `uploads/` as sensitive; use local overrides or environment variables for secrets.
