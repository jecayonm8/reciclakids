# Despliegue del backend en Google Cloud

Sigue los ADR del documento de arquitectura: el backend corre en **Cloud Run** (GCP, ADR-05) con
**Cloud SQL para PostgreSQL** (ADR-03), habla HTTPS con la app (ADR-07) y envía los correos por
**Amazon SES** (ADR-10). Los pasos usan `gcloud`; cada uno se hace una sola vez salvo el 6, que se
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
gcloud sql instances create $INSTANCIA --database-version=POSTGRES_17 --region=$REGION \
  --tier=db-f1-micro --storage-auto-increase --backup-start-time=03:00
gcloud sql databases create reciclakids --instance=$INSTANCIA
gcloud sql users create reciclakids --instance=$INSTANCIA --password="<contraseña larga>"
```

`db-f1-micro` alcanza para el piloto de una institución. Las copias de respaldo diarias quedan
activadas; la réplica de respaldo en AWS RDS del ADR-06 se configura aparte cuando el piloto crezca.
Las tablas las crea el backend al arrancar (migraciones de Flyway en `backend/src/main/resources/db/migration`).

## 3. Secretos

Ningún secreto va en el código ni en la imagen: viven en Secret Manager.

```bash
printf '%s' "<contraseña de la base>"            | gcloud secrets create database-password --data-file=-
openssl rand -base64 48 | tr -d '\n'             | gcloud secrets create jwt-secret --data-file=-
openssl rand -base64 32 | tr -d '\n'             | gcloud secrets create tareas-clave --data-file=-
printf '%s' "<AWS_ACCESS_KEY_ID de SES>"         | gcloud secrets create aws-access-key-id --data-file=-
printf '%s' "<AWS_SECRET_ACCESS_KEY de SES>"     | gcloud secrets create aws-secret-access-key --data-file=-
```

La cuenta de servicio de Cloud Run necesita leerlos y conectarse a Cloud SQL:

```bash
CUENTA=$(gcloud iam service-accounts list --filter="displayName:Compute Engine default" --format='value(email)')
for s in database-password jwt-secret tareas-clave aws-access-key-id aws-secret-access-key; do
  gcloud secrets add-iam-policy-binding $s --member="serviceAccount:$CUENTA" --role=roles/secretmanager.secretAccessor
done
gcloud projects add-iam-policy-binding $PROYECTO --member="serviceAccount:$CUENTA" --role=roles/cloudsql.client
```

## 4. Correo con Amazon SES

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
  --set-env-vars "RECICLAKIDS_ENTORNO=produccion,RECICLAKIDS_VISTA_PREVIA_CORREOS=false,CORREO_PROVEEDOR=ses,AWS_REGION=us-east-1" \
  --set-env-vars "CORREO_REMITENTE=ReciclaKids <no-responder@tu-dominio>" \
  --set-env-vars "DATABASE_USER=reciclakids,DATABASE_URL=jdbc:postgresql:///reciclakids?cloudSqlInstance=$PROYECTO:$REGION:$INSTANCIA&socketFactory=com.google.cloud.sql.postgres.SocketFactory" \
  --set-secrets "DATABASE_PASSWORD=database-password:latest,JWT_SECRET=jwt-secret:latest,TAREAS_CLAVE=tareas-clave:latest" \
  --set-secrets "AWS_ACCESS_KEY_ID=aws-access-key-id:latest,AWS_SECRET_ACCESS_KEY=aws-secret-access-key:latest"
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

La app de Android apunta a la URL de Cloud Run (`$URL/api/v1/`). Solo habla HTTPS con el backend;
en local se usa `docker compose up` dentro de `backend/` y la dirección `http://10.0.2.2:8080`
desde el emulador.

## Lista de verificación antes del piloto

- [ ] `curl $URL/salud` responde `{"estado":"ok"}`.
- [ ] Registrar una docente, agregar un niño, publicar un reto y entrar con el código desde la app.
- [ ] Llega el correo de recuperación de contraseña (prueba el remitente y SPF/DKIM).
- [ ] La tarea del viernes responde con `correosEnviados` (`gcloud scheduler jobs run reporte-semanal`).
- [ ] La política de tratamiento de datos definitiva está en la app (Ley 1581 de 2012).
