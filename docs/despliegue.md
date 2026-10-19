# Despliegue del backend en Google Cloud

Sigue los ADR del documento de arquitectura: el backend corre en **Cloud Run** (GCP, ADR-05) con
**Cloud SQL para PostgreSQL** (ADR-03), habla HTTPS con la app (ADR-07) y envía los correos por
**SendGrid** o **Amazon SES** (ADR-10; ver la sección 4). Los pasos usan `gcloud`; cada uno se hace una sola vez salvo el 6, que se
repite en cada versión.

Variables que se usan abajo (cámbialas por las del equipo):

```bash
PROYECTO=reciclakids-piloto
REGION=us-east1                 # la región con menor latencia hacia Colombia entre las de Cloud Run
INSTANCIA=reciclakids-bd
IMAGEN=$REGION-docker.pkg.dev/$PROYECTO/reciclakids/backend
```

## 1. Proyecto y servicios

```bash
gcloud config set project $PROYECTO
gcloud services enable run.googleapis.com sqladmin.googleapis.com artifactregistry.googleapis.com \
  secretmanager.googleapis.com cloudscheduler.googleapis.com cloudbuild.googleapis.com
gcloud artifacts repositories create reciclakids --repository-format=docker --location=$REGION
```

## 2. Base de datos (Cloud SQL)

```bash
gcloud sql instances create $INSTANCIA --database-version=POSTGRES_17 --edition=ENTERPRISE \
  --region=$REGION --tier=db-f1-micro --storage-auto-increase --backup-start-time=08:00
gcloud sql databases create reciclakids --instance=$INSTANCIA
gcloud sql users create reciclakids --instance=$INSTANCIA --password="<contraseña larga>"
```

`db-f1-micro` alcanza para el piloto de una institución. Necesita `--edition=ENTERPRISE`: desde
PostgreSQL 16, Cloud SQL crea por defecto instancias Enterprise Plus, que no aceptan esa máquina.
Las copias de respaldo diarias quedan activadas (`--backup-start-time` va en UTC: `08:00` son las
3 a. m. en Bogotá); la réplica de respaldo en AWS RDS del ADR-06 se configura aparte cuando el piloto crezca.
Las tablas las crea el backend al arrancar (migraciones de Flyway en `backend/src/main/resources/db/migration`).

## 3. Secretos

Ningún secreto va en el código ni en la imagen: viven en Secret Manager.

```bash
printf '%s' "<contraseña de la base>"            | gcloud secrets create database-password --data-file=-
openssl rand -base64 48 | tr -d '\n'             | gcloud secrets create jwt-secret --data-file=-
openssl rand -base64 32 | tr -d '\n'             | gcloud secrets create tareas-clave --data-file=-
# Con SendGrid (sección 4):
printf '%s' "<SENDGRID_API_KEY>"                 | gcloud secrets create sendgrid-api-key --data-file=-
# Con Amazon SES, en su lugar:
printf '%s' "<AWS_ACCESS_KEY_ID de SES>"         | gcloud secrets create aws-access-key-id --data-file=-
printf '%s' "<AWS_SECRET_ACCESS_KEY de SES>"     | gcloud secrets create aws-secret-access-key --data-file=-
```

La cuenta de servicio de Cloud Run necesita leerlos y conectarse a Cloud SQL:

```bash
CUENTA=$(gcloud iam service-accounts list --filter="displayName:Compute Engine default" --format='value(email)')
for s in database-password jwt-secret tareas-clave sendgrid-api-key; do   # con SES: aws-access-key-id aws-secret-access-key
  gcloud secrets add-iam-policy-binding $s --member="serviceAccount:$CUENTA" --role=roles/secretmanager.secretAccessor
done
gcloud projects add-iam-policy-binding $PROYECTO --member="serviceAccount:$CUENTA" --role=roles/cloudsql.client
```

## 4. Correo

Hay dos proveedores; se elige con `CORREO_PROVEEDOR`.

### Opción A: SendGrid (la más rápida, sin dominio propio)

1. En SendGrid, **Settings → Sender Authentication → Single Sender Verification → Create New Sender**
   con una dirección que no sea `@gmail.com` (Gmail y Yahoo rechazan o mandan a spam lo que se envía
   en su nombre por terceros). Confirma el correo que SendGrid envía. Esa dirección es `CORREO_REMITENTE`.
2. **Settings → API Keys → Create API Key → Custom Access**, todo en «No Access» salvo **Mail Send** en
   «Full Access». Guarda la clave en el secreto `sendgrid-api-key` (paso 3).
3. En el despliegue (paso 5): `CORREO_PROVEEDOR=sendgrid`, `CORREO_REMITENTE` con la dirección verificada
   y `SENDGRID_API_KEY` desde el secreto.

El plan gratuito permanente de SendGrid se retiró: revisa en tu cuenta hasta cuándo dura la prueba.
Sin autenticar un dominio, algunos correos pueden caer en spam; para el piloto es suficiente.

### Opción B: Amazon SES

1. En la consola de SES (región `us-east-1`), verifica el dominio del remitente y publica en su DNS
   los registros **SPF, DKIM y DMARC** que SES indica.
2. Pide salir del *sandbox* de SES; mientras tanto solo se puede enviar a correos verificados.
3. Crea un usuario de IAM con permiso solo para `ses:SendEmail` y usa sus llaves en el paso 3.

## 5. Primera publicación del servicio

Desde la raíz del repositorio:

```bash
gcloud builds submit backend --tag $IMAGEN:v1

gcloud run deploy reciclakids-backend --image $IMAGEN:v1 --region $REGION \
  --allow-unauthenticated --min-instances=0 --max-instances=3 --memory=512Mi \
  --add-cloudsql-instances $PROYECTO:$REGION:$INSTANCIA \
  --set-env-vars "RECICLAKIDS_ENTORNO=produccion,RECICLAKIDS_VISTA_PREVIA_CORREOS=false,CORREO_PROVEEDOR=ses,AWS_REGION=us-east-1   # con SendGrid: CORREO_PROVEEDOR=sendgrid" \
  --set-env-vars "CORREO_REMITENTE=ReciclaKids <no-responder@tu-dominio>" \
  --set-env-vars "DATABASE_USER=reciclakids,DATABASE_URL=jdbc:postgresql:///reciclakids?cloudSqlInstance=$PROYECTO:$REGION:$INSTANCIA&socketFactory=com.google.cloud.sql.postgres.SocketFactory" \
  --set-secrets "DATABASE_PASSWORD=database-password:latest,JWT_SECRET=jwt-secret:latest,TAREAS_CLAVE=tareas-clave:latest" \
  --set-secrets "AWS_ACCESS_KEY_ID=aws-access-key-id:latest,AWS_SECRET_ACCESS_KEY=aws-secret-access-key:latest"
# Con SendGrid, en lugar de la línea anterior: --set-secrets "SENDGRID_API_KEY=sendgrid-api-key:latest"
```

Cloud Run responde con la URL del servicio (`https://reciclakids-backend-xxxx.a.run.app`). Ponla
como dirección pública, que se usa en los enlaces de los correos:

```bash
URL=$(gcloud run services describe reciclakids-backend --region $REGION --format='value(status.url)')
gcloud run services update reciclakids-backend --region $REGION --update-env-vars RECICLAKIDS_URL_PUBLICA=$URL
curl $URL/salud    # {"estado":"ok"}
```

Si el equipo tiene dominio propio, se mapea con `gcloud run domain-mappings create` y se cambia
`RECICLAKIDS_URL_PUBLICA` por ese dominio. Cloud Run ya sirve todo por HTTPS.

## 6. Nuevas versiones

```bash
gcloud builds submit backend --tag $IMAGEN:v2
gcloud run deploy reciclakids-backend --image $IMAGEN:v2 --region $REGION
```

Las migraciones nuevas de Flyway se aplican solas al arrancar la versión nueva.

## 7. Reporte semanal de los viernes

Cloud Run escala a cero, así que el envío lo dispara Cloud Scheduler: los viernes a las 5 p. m. de
Bogotá llama a la tarea con la clave.

```bash
CLAVE=$(gcloud secrets versions access latest --secret=tareas-clave)
gcloud scheduler jobs create http reporte-semanal --location=$REGION \
  --schedule="0 17 * * 5" --time-zone="America/Bogota" \
  --uri="$URL/tareas/reporte-semanal" --http-method=POST \
  --headers="X-Tareas-Clave=$CLAVE"
```

## 8. Conectar la app

La app de Android lee la dirección de la API de `BuildConfig.API_URL`, que se fija al compilar.
La release solo habla HTTPS y solo confía en los certificados del sistema
(`app-movil/app/src/main/res/xml/red_produccion.xml`).

### Release contra Cloud Run

Con la `$URL` del paso 5 (o el dominio propio, si se mapeó), desde `app-movil/`:

```bash
export RECICLAKIDS_API_URL="$URL/api/v1/"     # o -Preciclakids.apiUrl=$URL/api/v1/
./gradlew assembleRelease                      # APK en app/build/outputs/apk/release/
./gradlew bundleRelease                        # AAB para Google Play en app/build/outputs/bundle/release/
```

La firma sale de cuatro variables de entorno; el keystore y sus claves viven en la máquina de
quien publica (o en los secretos de la CI), nunca en el repositorio:

| Variable | Qué es |
|---|---|
| `RECICLAKIDS_KEYSTORE` | Ruta al archivo `.jks` de la app |
| `RECICLAKIDS_KEYSTORE_CLAVE` | Contraseña del keystore |
| `RECICLAKIDS_KEY_ALIAS` | Alias de la llave |
| `RECICLAKIDS_KEY_CLAVE` | Contraseña de la llave |

Si están las cuatro, la release sale firmada y **la compilación falla** si falta la URL o no es
HTTPS. Sin ellas sale un APK sin firmar, que no se puede instalar: es lo que compila la CI
(`./gradlew build`) para comprobar que la release compila. El keystore se crea una sola vez con
`keytool -genkeypair -v -keystore reciclakids.jks -keyalg RSA -keysize 2048 -validity 10000 -alias reciclakids`
y se guarda con copia de respaldo: sin él no se pueden publicar actualizaciones en Google Play.

### En local (debug)

Con el backend de `docker compose up --build` dentro de `backend/`:

- **Emulador**: `./gradlew installDebug` apunta a `http://10.0.2.2:8080/api/v1/` (el PC visto
  desde el emulador).
- **Teléfono en la misma red Wi-Fi**: con la IP del PC,
  `./gradlew installDebug -Preciclakids.apiUrl=http://192.168.1.5:8080/api/v1/`. Si Windows
  pregunta, permite el puerto 8080 en el firewall para redes privadas.

Solo la app debug acepta HTTP sin cifrar, y solo hacia `10.0.2.2`, `localhost`, `127.0.0.1` y el
host de esa URL: la tarea `generarRedDesarrolloDebug` escribe su network security config.

### Qué guarda el teléfono

- La sesión del adulto (token de acceso de 1 h y de refresco de 30 días) y el token de cada niño
  (7 días) van cifrados con una llave del Android Keystore y se excluyen de los respaldos. Al
  pasar la puerta para adultos, una sesión vigente abre directo su modo (`GET /cuenta`); si el
  refresco falla, la app vuelve al inicio de sesión.
- Las partidas se guardan en Room antes de salir. El sincronizador las manda completas a
  `POST /nino/partidas` con el token de cada niño (un teléfono puede ser de varios) y solo marca
  como enviado lo que el backend confirmó (ADR-04).

## Lista de verificación antes del piloto

- [ ] `curl $URL/salud` responde `{"estado":"ok"}`.
- [ ] Registrar una docente, agregar un niño, publicar un reto y entrar con el código desde la app.
- [ ] Llega el correo de recuperación de contraseña (prueba el remitente y SPF/DKIM).
- [ ] La tarea del viernes responde con `correosEnviados` (`gcloud scheduler jobs run reporte-semanal`).
- [ ] La política de tratamiento de datos definitiva está en la app (Ley 1581 de 2012).
