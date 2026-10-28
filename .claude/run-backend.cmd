@echo off
rem Levanta el backend en local con el perfil dev (base de pruebas remota). Usa JDK 21: el JDK por defecto rompe Lombok.
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
cd /d "%~dp0..\Backend\dtodo-backend"
call "%CD%\mvnw.cmd" spring-boot:run -Dspring-boot.run.profiles=dev
