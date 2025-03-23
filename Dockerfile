# Stage 1: Build Angular Frontend
FROM node:22 AS ngbuild

WORKDIR /app/client

# Install Angular CLI globally
RUN npm install -g @angular/cli@19.2.1

# Copy ONLY necessary files for npm install (for caching)
COPY client/package*.json ./
COPY client/angular.json ./
COPY client/tsconfig*.json ./

# Install dependencies (using npm ci for consistency)
RUN npm ci

# Copy the entire source code (after dependency installation)
COPY client/src ./src

# Build the Angular app with verbose output
RUN ng build --verbose

# Stage 2: Build Spring Boot Backend
FROM openjdk:23 AS javabuild

WORKDIR /app

# Copy Maven files
COPY server/pom.xml .
COPY server/.mvn/ .mvn/
COPY server/mvnw .
COPY server/src ./src

COPY --from=ngbuild /app/client/dist/client/browser ./src/main/resources/static

# Build Spring Boot application
RUN chmod a+x mvnw
RUN ./mvnw package -Dmaven.test.skip=true

# Stage 3: Final Runtime Image
FROM openjdk:23

WORKDIR /app

# Copy the built jar file from the build stage
COPY --from=javabuild /app/target/*.jar app.jar

# Expose the port
EXPOSE ${PORT}

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]