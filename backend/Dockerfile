# Imagen del backend de ReciclaKids para Cloud Run (ver docs/despliegue.md).

# --- Compilación ---
# El daemon de Gradle pide JDK 25 (gradle/gradle-daemon-jvm.properties) y el código compila con
# la toolchain 21: la imagen trae los dos para no descargar nada durante el build.
FROM eclipse-temurin:25-jdk AS compilacion
COPY --from=eclipse-temurin:21-jdk /opt/java/openjdk /opt/jdk21
RUN mkdir -p /root/.gradle && printf '%s\n' \
      'org.gradle.java.installations.paths=/opt/jdk21' \
      'org.gradle.java.installations.auto-download=false' > /root/.gradle/gradle.properties
WORKDIR /fuente
# Primero solo lo que define dependencias: Docker reutiliza esta capa si el código cambia.
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
# Un checkout en Windows puede traer gradlew con CRLF.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew --no-daemon --quiet dependencies > /dev/null
COPY src ./src
RUN ./gradlew --no-daemon --quiet buildFatJar -x test

# --- Ejecución ---
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S reciclakids && adduser -S reciclakids -G reciclakids
USER reciclakids
WORKDIR /app
COPY --from=compilacion /fuente/build/libs/reciclakids-backend.jar ./reciclakids-backend.jar
ENV RECICLAKIDS_ENTORNO=produccion \
    PORT=8080
EXPOSE 8080
# Cloud Run limita la memoria del contenedor: la JVM usa hasta el 75 % de ella.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/reciclakids-backend.jar"]
