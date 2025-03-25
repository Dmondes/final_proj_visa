# Stage 1: Build Angular Frontend
FROM node:22 AS ngbuild

WORKDIR /app/client

# Install Angular CLI globally
RUN npm install -g @angular/cli@19.2.1

# Copy ONLY necessary files for npm install (for caching)
COPY client/package*.json ./
COPY client/angular.json ./
COPY client/tsconfig*.json ./

# Copy the entire source code
COPY client/src ./src

# Install dependencies
RUN npm ci

# Make the build script executable
RUN if [ -f src/build.sh ]; then \
    sed -i 's/\r$//' src/build.sh && \
    chmod +x src/build.sh; \
    fi

# Run the custom build script if it exists else ng build
RUN if [ -f src/build.sh ]; then \
    bash ./src/build.sh; \
    else \
    ng build; \
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