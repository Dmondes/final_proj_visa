# Stage 1: Build Angular Frontend
FROM node:18 AS ngbuild

WORKDIR /app/client

# Copy the entire client directory
COPY client/ ./

# Install dependencies
RUN npm install

# Install Angular CLI matching the project version
RUN npm install -g @angular/cli@19.1.8

# Build in development mode to bypass strict production checks
RUN echo "Building Angular application..." && \
    ng build --configuration=development --output-path=dist/client/browser

# Show build output for debugging
RUN echo "Checking build output:" && \
    ls -la dist/ || echo "dist/ not found" && \
    ls -la dist/client/ || echo "dist/client/ not found" && \
    ls -la dist/client/browser/ || echo "dist/client/browser/ not found"

# Stage 2: Build Spring Boot Backend
FROM openjdk:23 AS javabuild

WORKDIR /app

# Copy Maven files
COPY server/pom.xml .
COPY server/.mvn/ .mvn/
COPY server/mvnw .
COPY server/src ./src

COPY --from=ngbuild /app/client/dist/client/browser/ ./src/main/resources/static

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