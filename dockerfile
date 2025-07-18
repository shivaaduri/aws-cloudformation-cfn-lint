# Build stage
FROM maven:3.8.6-openjdk-11 AS build
WORKDIR C:/app

# Copy only the files needed for building
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:11-jre-windowsservercore-1809

WORKDIR C:/app

# Copy the built JAR from the build stage
COPY --from=build C:/app/target/java-swe-bench-*.jar C:/app/java-swe-bench.jar

# Install Git for Windows
RUN powershell -Command \
    $ErrorActionPreference = 'Stop'; \
    Invoke-WebRequest -Uri "https://github.com/git-for-windows/git/releases/download/v2.40.0.windows.1/Git-2.40.0-64-bit.exe" -OutFile git-installer.exe; \
    Start-Process -Wait -FilePath .\git-installer.exe -ArgumentList '/VERYSILENT', '/NORESTART', '/NOCANCEL', '/SP-', '/COMPONENTS=""'; \
    Remove-Item git-installer.exe

# Set environment variables
ENV REPO_URL="" \
    PR_URL="" \
    TRAINER_EMAIL=""

# Create directory for artifacts
RUN mkdir artifacts

# Entrypoint script
COPY entrypoint.ps1 C:/app/
ENTRYPOINT ["powershell", "-File", "C:\\app\\entrypoint.ps1"]