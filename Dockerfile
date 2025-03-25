# Stage 1: Build Angular Frontend
FROM node:22 AS ngbuild

WORKDIR /app/client

# Install Angular CLI globally
RUN npm install -g @angular/cli@19.2.1

# Copy ONLY necessary files for npm install (for caching)
COPY client/package*.json ./
COPY client/angular.json ./
COPY client/tsconfig*.json ./

# Install dependencies first (for better caching)
RUN npm ci 

# Copy the entire source code
COPY client/src ./src
COPY client/public ./public

# Simply run ng build for the frontend
RUN echo "Building Angular application..." && ng build

# Verify build output exists
RUN ls -la dist/ && \
    if [ -d "dist/client" ]; then \
      ls -la dist/client/; \
      if [ -d "dist/client/browser" ]; then \
        echo "Build succeeded with expected output"; \
      else \
        echo "ERROR: Expected dist/client/browser directory not found"; \
        exit 1; \
      fi \
    else \
      echo "ERROR: dist/client directory not found"; \
      exit 1; \
    fi

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