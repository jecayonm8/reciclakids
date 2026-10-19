# Servicios, claves y secretos para desplegar ReciclaKids en Google Cloud

Esta guía explica **cómo conseguir cada credencial** que necesita ReciclaKids, **dónde guardarla** y
**cómo conectarla** con el backend en Cloud Run, la app Android y GitHub Actions. Complementa a
[despliegue.md](despliegue.md), que cubre el despliegue en sí; aquí está todo lo que tiene que existir
antes. Está escrita para el estado del repositorio con los PR #25 (TestRail por casos CP) y #26 (app
conectada al backend) fusionados.

> **Regla de oro:** ningún secreto va en el repositorio, en un chat ni en un correo. Cada uno vive en
> un solo lugar seguro (Secret Manager, los secretos de GitHub o el gestor de contraseñas del equipo) y
> se copia de ahí directamente a donde se usa.

## Contenido

1. [Mapa de credenciales](#1-mapa-de-credenciales)
2. [Antes de empezar](#2-antes-de-empezar)
3. [Google Cloud: proyecto, servicios y cuentas de servicio](#3-google-cloud-proyecto-servicios-y-cuentas-de-servicio)
4. [Base de datos: Cloud SQL para PostgreSQL](#4-base-de-datos-cloud-sql-para-postgresql)
5. [Correo: SendGrid o Amazon SES](#5-correo-sendgrid-o-amazon-ses)
6. [Secret Manager: crear y dar acceso a los secretos](#6-secret-manager-crear-y-dar-acceso-a-los-secretos)
7. [Conectar todo en Cloud Run](#7-conectar-todo-en-cloud-run)
8. [Tarea de los viernes: Cloud Scheduler](#8-tarea-de-los-viernes-cloud-scheduler)
9. [App Android: URL de la API y firma de la release](#9-app-android-url-de-la-api-y-firma-de-la-release)
10. [Secretos y variables de GitHub](#10-secretos-y-variables-de-github)
11. [Comprobar que todo quedó bien](#11-comprobar-que-todo-quedó-bien)
12. [Rotar una credencial](#12-rotar-una-credencial)
13. [Problemas frecuentes](#13-problemas-frecuentes)

---

## 1. Mapa de credenciales

| Valor | De dónde sale | Dónde se guarda | Quién lo usa |
|---|---|---|---|
| ID del proyecto de GCP | Lo eliges al crear el proyecto | No es secreto (variable de GitHub `GCP_PROYECTO`) | `gcloud`, despliegue |
| Contraseña de la base (`DATABASE_PASSWORD`) | La generas tú (§4) | Secret Manager: `database-password` | Backend |
| Secreto de los tokens (`JWT_SECRET`) | Lo generas tú (§6) | Secret Manager: `jwt-secret` | Backend |
| Clave de tareas (`TAREAS_CLAVE`) | La generas tú (§6) | Secret Manager: `tareas-clave` | Backend y Cloud Scheduler |
| Clave de SendGrid (`SENDGRID_API_KEY`) | SendGrid → Settings → API Keys (§5.A) | Secret Manager: `sendgrid-api-key` | Backend, si se usa SendGrid |
| Llaves de AWS (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`) | Usuario de IAM de AWS (§5.B) | Secret Manager: `aws-access-key-id`, `aws-secret-access-key` | Backend, si se usa Amazon SES |
| Remitente de los correos (`CORREO_REMITENTE`) | Remitente verificado en SendGrid, o tu dominio verificado en SES (§5) | Variable de entorno de Cloud Run | Backend |
| URL pública del backend (`RECICLAKIDS_URL_PUBLICA`) | Cloud Run al desplegar (§7) | Variable de entorno de Cloud Run | Backend (enlaces de los correos) |
| URL de la API para la app (`RECICLAKIDS_API_URL`) | La misma URL + `/api/v1/` | No es secreto: variable de GitHub o de quien compila | Compilación de la release (§9) |
| Keystore de la app y sus 2 contraseñas + alias | Los generas tú con `keytool` (§9) | Gestor de contraseñas del equipo y, si la CI compila la release, secretos de GitHub | Firma de la release |
| Credenciales de TestRail (`TESTRAIL_URL`, `TESTRAIL_USER`, `TESTRAIL_API_KEY`) | TestRail → My Settings → API Keys (§10.1) | Secretos de GitHub | CI (subida de resultados) |
| Proveedor de identidad y cuenta de despliegue (`GCP_PROVEEDOR_WIF`, `GCP_CUENTA_DESPLIEGUE`) | Workload Identity Federation (§10.3) | Secretos de GitHub | Despliegue desde GitHub Actions (opcional) |

Fíjate en que **Google Cloud no necesita una "API key"** para que el backend use la base de datos ni
Secret Manager: Cloud Run corre con una **cuenta de servicio** que tiene permisos, y Google le entrega
sus credenciales sola. Las únicas llaves que se copian a mano son las del proveedor de correo (SendGrid o AWS), las de TestRail y
el keystore de la app.

## 2. Antes de empezar

### Cuentas

- **Google Cloud** con una cuenta de facturación activa (la prueba gratuita sirve). Sin facturación no
  se pueden crear Cloud SQL ni Cloud Run.
- **SendGrid** (lo más rápido) **o AWS**, para el correo (ADR-10).
- **Solo con Amazon SES: un dominio propio** (por ejemplo `reciclakids.co`) para el remitente de los correos. SendGrid no lo necesita: basta una dirección de correo verificada. Se compra en
  cualquier registrador o en Cloud Domains; la universidad también puede dar un subdominio. Sin dominio,
  SES solo envía a correos verificados uno por uno (sirve para probar, no para el piloto).
- **Administrador del repositorio** `jecayonm8/reciclakids` en GitHub, para crear secretos.
- **TestRail** con la API habilitada (ya está).

### Herramientas

La forma más simple es **Cloud Shell** (en la consola de GCP, botón `>_` arriba a la derecha): ya trae
`gcloud`, `openssl` y `bash`. Si prefieres tu computador:

- [Google Cloud CLI](https://cloud.google.com/sdk/docs/install) (`gcloud`); en Windows, usa los
  comandos desde **Git Bash**.
- `openssl` (viene con Git para Windows).
- `keytool` (viene con el JDK; lo usas en §9).
- `gh` (GitHub CLI) para los secretos de GitHub; también se pueden crear desde la web.

### Variables de la guía

Todos los comandos usan estas variables. Defínelas una vez en tu terminal (cámbialas por las tuyas):

```bash
export PROYECTO=reciclakids-piloto          # ID único en todo GCP: minúsculas, números y guiones
export REGION=us-east1                      # la región de Cloud Run con menor latencia hacia Colombia
export INSTANCIA=reciclakids-bd
export REPO=reciclakids
export IMAGEN=$REGION-docker.pkg.dev/$PROYECTO/$REPO/backend
export CUENTA_RUN=reciclakids-backend@$PROYECTO.iam.gserviceaccount.com
```

## 3. Google Cloud: proyecto, servicios y cuentas de servicio

### 3.1 Crear el proyecto y conectarle la facturación

```bash
gcloud auth login
gcloud projects create $PROYECTO --name="ReciclaKids"
gcloud billing accounts list                      # copia el ACCOUNT_ID de la cuenta de facturación
gcloud billing projects link $PROYECTO --billing-account=<ACCOUNT_ID>
gcloud config set project $PROYECTO
```

### 3.2 Habilitar los servicios (APIs de Google)

En GCP, "conseguir la API" de un servicio es **habilitarlo** en el proyecto:

```bash
gcloud services enable \
  run.googleapis.com sqladmin.googleapis.com artifactregistry.googleapis.com \
  secretmanager.googleapis.com cloudscheduler.googleapis.com cloudbuild.googleapis.com \
  iam.googleapis.com iamcredentials.googleapis.com
```

| Servicio | Para qué |
|---|---|
| Cloud Run | Corre el backend (la imagen de `backend/Dockerfile`) |
| Cloud SQL Admin | Base de datos PostgreSQL y conexión segura desde Cloud Run |
| Artifact Registry | Guarda las imágenes Docker del backend |
| Secret Manager | Guarda las contraseñas y llaves |
| Cloud Scheduler | Dispara el reporte semanal de los viernes |
| Cloud Build | Construye la imagen con `gcloud builds submit` |
| IAM / IAM Credentials | Cuentas de servicio y despliegue desde GitHub (§10.3) |

### 3.3 Repositorio de imágenes

```bash
gcloud artifacts repositories create $REPO --repository-format=docker --location=$REGION \
  --description="Imágenes del backend de ReciclaKids"
```

### 3.4 Cuenta de servicio del backend

El backend corre con su **propia** cuenta de servicio, con solo los permisos que necesita (mejor que la
cuenta por defecto de Compute Engine, que tiene permisos de más):

```bash
gcloud iam service-accounts create reciclakids-backend --display-name="Backend ReciclaKids (Cloud Run)"
gcloud projects add-iam-policy-binding $PROYECTO \
  --member="serviceAccount:$CUENTA_RUN" --role="roles/cloudsql.client"
```

El acceso a cada secreto se da en §6.2, secreto por secreto.

## 4. Base de datos: Cloud SQL para PostgreSQL

### 4.1 Crear la instancia

```bash
gcloud sql instances create $INSTANCIA \
  --database-version=POSTGRES_17 --edition=ENTERPRISE --tier=db-f1-micro \
  --region=$REGION --storage-auto-increase --backup-start-time=08:00
```

- **`--edition=ENTERPRISE` es obligatorio** con `db-f1-micro`: desde PostgreSQL 16, Cloud SQL crea por
  defecto instancias Enterprise Plus, que no aceptan esa máquina pequeña.
- `db-f1-micro` alcanza para el piloto de una institución. Es el costo principal del proyecto: la
  instancia cobra aunque nadie la use. Revisa la [calculadora de precios](https://cloud.google.com/products/calculator).
- `--backup-start-time` va en UTC: `08:00` son las 3 a. m. en Bogotá.

### 4.2 Base y usuario

Genera una contraseña larga sin símbolos raros (así no hay que escaparla en ninguna parte) y úsala sin
mostrarla:

```bash
CLAVE_BD=$(openssl rand -base64 48 | tr -d '\n/+=' | cut -c1-40)
gcloud sql databases create reciclakids --instance=$INSTANCIA
gcloud sql users create reciclakids --instance=$INSTANCIA --password="$CLAVE_BD"
```

No cierres la terminal: `CLAVE_BD` se guarda en Secret Manager en §6.1. Las tablas las crea el backend
al arrancar (migraciones de Flyway en `backend/src/main/resources/db/migration`).

### 4.3 Datos de conexión

El nombre de conexión de la instancia tiene la forma `PROYECTO:REGION:INSTANCIA`:

```bash
gcloud sql instances describe $INSTANCIA --format='value(connectionName)'
```

Con él se arma la URL JDBC que usa el backend (no es secreta):

```text
jdbc:postgresql:///reciclakids?cloudSqlInstance=<PROYECTO>:<REGION>:<INSTANCIA>&socketFactory=com.google.cloud.sql.postgres.SocketFactory
```

El conector de Cloud SQL (ya incluido en el backend) cifra la conexión y la autentica con la cuenta de
servicio: la base no queda expuesta a Internet.

## 5. Correo: SendGrid o Amazon SES

El backend envía tres correos: logro, reporte semanal y recuperación de contraseña. El proveedor se elige
con `CORREO_PROVEEDOR`:

| Proveedor | Cuándo conviene | Qué necesita |
|---|---|---|
| `sendgrid` | Piloto rápido, sin dominio propio | Un remitente verificado y una clave de API (§5.A) |
| `ses` | Producción con dominio propio y mejor entregabilidad | Dominio con DKIM, SPF y DMARC y llaves de AWS (§5.B) |

En producción el servidor **no arranca** con otro valor, ni con `sendgrid` sin `SENDGRID_API_KEY`.

### 5.A SendGrid (recomendado para el piloto)

**Remitente.** En SendGrid: **Settings → Sender Authentication → Single Sender Verification → Create New
Sender**. Escribe una dirección tuya y confirma el correo que SendGrid envía. Esa dirección exacta es el
`CORREO_REMITENTE` (por ejemplo `ReciclaKids <no-responder@tu-dominio>`).

- **No uses una dirección `@gmail.com`, `@yahoo.com` ni `@hotmail.com`:** esos proveedores rechazan o
  mandan a spam los correos enviados en su nombre por terceros. Usa una de tu institución o de tu dominio.
- Sin autenticar un dominio, algunos correos pueden caer en spam con más frecuencia que con SES. Para el
  piloto basta; si crece, autentica un dominio en **Sender Authentication → Authenticate Your Domain**.

**Clave de API.** **Settings → API Keys → Create API Key → Custom Access**. Deja todo en «No Access» salvo
**Mail Send**, que va en «Full Access». Pulsa **Create & View** y copia la clave (empieza por `SG.`):
SendGrid la muestra **una sola vez**.

**Guardarla en Secret Manager** (pide el valor sin mostrarlo):

```bash
read -rsp "SENDGRID_API_KEY: " V; echo; printf '%s' "$V" | gcloud secrets create sendgrid-api-key --replication-policy=automatic --data-file=-; unset V
```

**Plan.** El plan gratuito permanente de SendGrid se retiró y ahora es una prueba de 60 días. Revisa el
estado de tu cuenta: cuando venza, los correos dejan de salir (y los registros de Cloud Run mostrarán el
estado HTTP que devuelve SendGrid).

### 5.B Amazon SES

Usa la región `us-east-1` (`AWS_REGION`).

#### 5.B.1 Verificar el dominio

1. Entra a la consola de AWS, servicio **Amazon SES**, región **N. Virginia (us-east-1)**.
2. **Configuration → Identities → Create identity → Domain**. Escribe tu dominio y deja activado
   **Easy DKIM** (RSA 2048).
3. SES muestra **3 registros CNAME** (`xxxx._domainkey.tu-dominio` → `xxxx.dkim.amazonses.com`).
   Publícalos en el DNS de tu dominio.
4. Publica también **DMARC**: un registro TXT en `_dmarc.tu-dominio` con
   `v=DMARC1; p=none; rua=mailto:dmarc@tu-dominio`.
5. Opcional, para **SPF** alineado: en la identidad, **Custom MAIL FROM domain** con un subdominio
   (p. ej. `correo.tu-dominio`), y publica el MX `10 feedback-smtp.us-east-1.amazonses.com` y el TXT
   `v=spf1 include:amazonses.com ~all` que SES indica.
6. Espera a que la identidad quede **Verified** (minutos u horas, según el DNS).

#### 5.B.2 Salir del sandbox

Mientras la cuenta esté en el *sandbox*, SES solo envía a correos verificados. En **Account dashboard →
Request production access** describe el uso: correos transaccionales a acudientes que se registran en
la app (logros, reporte semanal, recuperación), con baja en cada correo. AWS suele responder en un día.

#### 5.B.3 Usuario de IAM con permiso solo para enviar

1. Consola de AWS → **IAM → Users → Create user**: nombre `reciclakids-ses`, **sin** acceso a la consola.
2. **Add permissions → Create inline policy → JSON**, con tu número de cuenta y tu dominio:

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": ["ses:SendEmail", "ses:SendRawEmail"],
         "Resource": "arn:aws:ses:us-east-1:<NUMERO_DE_CUENTA>:identity/<tu-dominio>"
       }
     ]
   }
   ```

3. En el usuario: **Security credentials → Create access key → Application running outside AWS**.
4. AWS muestra el **Access key ID** y el **Secret access key una sola vez**. Guárdalos de inmediato en
   Secret Manager (§6.1) y cierra la ventana; no los pegues en ningún otro lado.

#### 5.B.4 Remitente

`CORREO_REMITENTE` usa una dirección de tu dominio verificado, por ejemplo
`ReciclaKids <no-responder@tu-dominio>`.

## 6. Secret Manager: crear y dar acceso a los secretos

### 6.1 Crear los secretos

Cada comando lee el valor desde la entrada estándar (`--data-file=-`), así el valor no queda en el
historial ni en un archivo. `printf '%s'` evita el salto de línea final, que rompería la contraseña.

```bash
# Contraseña de la base (la de §4.2)
printf '%s' "$CLAVE_BD" | gcloud secrets create database-password --replication-policy=automatic --data-file=-

# Secreto de los tokens JWT: al menos 32 caracteres (el backend no arranca con menos)
openssl rand -base64 48 | tr -d '\n' | gcloud secrets create jwt-secret --replication-policy=automatic --data-file=-

# Clave que manda Cloud Scheduler: al menos 24 caracteres
openssl rand -base64 32 | tr -d '\n/+=' | gcloud secrets create tareas-clave --replication-policy=automatic --data-file=-

unset CLAVE_BD
```

El secreto del proveedor de correo se crea en §5: `sendgrid-api-key` (§5.A) o, con Amazon SES,
`aws-access-key-id` y `aws-secret-access-key` (§5.B.3):

```bash
read -rsp "AWS_ACCESS_KEY_ID: " V; echo; printf '%s' "$V" | gcloud secrets create aws-access-key-id --replication-policy=automatic --data-file=-
read -rsp "AWS_SECRET_ACCESS_KEY: " V; echo; printf '%s' "$V" | gcloud secrets create aws-secret-access-key --replication-policy=automatic --data-file=-
unset V
```

(Solo con SES; con SendGrid no hacen falta.)

Desde la consola web también se puede: **Security → Secret Manager → Create secret**, con el mismo
nombre y el valor pegado sin espacios ni saltos de línea al final.

### 6.2 Dar acceso a la cuenta del backend

Con SendGrid:

```bash
for s in database-password jwt-secret tareas-clave sendgrid-api-key; do
  gcloud secrets add-iam-policy-binding $s \
    --member="serviceAccount:$CUENTA_RUN" --role="roles/secretmanager.secretAccessor"
done
```

Con Amazon SES, cambia `sendgrid-api-key` por `aws-access-key-id aws-secret-access-key`.

### 6.3 Comprobar

```bash
gcloud secrets list                                   # deben salir los 5 nombres
gcloud secrets versions list jwt-secret               # una versión "enabled"
```

Para leer un valor (solo si de verdad hace falta, por ejemplo para Cloud Scheduler):
`gcloud secrets versions access latest --secret=tareas-clave`.

## 7. Conectar todo en Cloud Run

### 7.1 Construir la imagen

Desde la raíz del repositorio (con `main` actualizado):

```bash
gcloud builds submit backend --tag $IMAGEN:v1
```

### 7.2 Variables de entorno en un archivo

Un archivo YAML evita problemas con las comas, los espacios y los `<` `>` del remitente. Créalo fuera
del repositorio (no tiene secretos, pero así no se sube por error):

```bash
cat > ~/reciclakids-env.yaml <<EOF
RECICLAKIDS_ENTORNO: "produccion"
RECICLAKIDS_VISTA_PREVIA_CORREOS: "false"
RECICLAKIDS_URL_PUBLICA: "https://pendiente.invalid"
CORREO_PROVEEDOR: "sendgrid"
CORREO_REMITENTE: "ReciclaKids <direccion-verificada-en-sendgrid>"
DATABASE_USER: "reciclakids"
DATABASE_URL: "jdbc:postgresql:///reciclakids?cloudSqlInstance=$PROYECTO:$REGION:$INSTANCIA&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
DATABASE_MAX_POOL: "5"
EOF
```

Cambia `direccion-verificada-en-sendgrid` por el remitente que verificaste en §5.A. Con Amazon SES,
`CORREO_PROVEEDOR` es `"ses"`, el remitente es una dirección de tu dominio y se agrega
`AWS_REGION: "us-east-1"`.

`RECICLAKIDS_URL_PUBLICA` se corrige en §7.4, cuando ya se conoce la URL. El backend exige HTTPS en
producción, así que el valor provisional también lo es.

### 7.3 Desplegar

```bash
gcloud run deploy reciclakids-backend --image $IMAGEN:v1 --region $REGION \
  --service-account $CUENTA_RUN \
  --add-cloudsql-instances $PROYECTO:$REGION:$INSTANCIA \
  --env-vars-file ~/reciclakids-env.yaml \
  --set-secrets "DATABASE_PASSWORD=database-password:latest,JWT_SECRET=jwt-secret:latest,TAREAS_CLAVE=tareas-clave:latest,SENDGRID_API_KEY=sendgrid-api-key:latest" \
  --allow-unauthenticated --min-instances=0 --max-instances=3 --memory=512Mi
```

- `--set-secrets` conecta cada variable con su secreto: Cloud Run lo lee al arrancar cada instancia.
  Con Amazon SES, en lugar de `SENDGRID_API_KEY=…` van `AWS_ACCESS_KEY_ID=aws-access-key-id:latest` y
  `AWS_SECRET_ACCESS_KEY=aws-secret-access-key:latest`.
- `--allow-unauthenticated` es correcto: la app llama la API sin cuenta de Google. La seguridad la
  ponen los tokens del backend.
- Con 3 instancias de 5 conexiones cada una se usan como máximo 15 conexiones, por debajo del límite de
  `db-f1-micro`.

### 7.4 Fijar la URL pública

```bash
URL=$(gcloud run services describe reciclakids-backend --region $REGION --format='value(status.url)')
gcloud run services update reciclakids-backend --region $REGION --update-env-vars RECICLAKIDS_URL_PUBLICA=$URL
curl $URL/salud                                       # {"estado":"ok"}
```

Si hay dominio propio para la API, mapéalo con `gcloud beta run domain-mappings create` y usa ese
dominio en `RECICLAKIDS_URL_PUBLICA` y en la app.

## 8. Tarea de los viernes: Cloud Scheduler

El reporte semanal se envía los viernes a las 5 p. m. de Bogotá. Cloud Scheduler llama al backend con la
clave de tareas:

```bash
gcloud scheduler jobs create http reporte-semanal --location=$REGION \
  --schedule="0 17 * * 5" --time-zone="America/Bogota" \
  --uri="$URL/tareas/reporte-semanal" --http-method=POST \
  --headers="X-Tareas-Clave=$(gcloud secrets versions access latest --secret=tareas-clave)"
```

Si rotas `tareas-clave` (§12), actualiza el encabezado con
`gcloud scheduler jobs update http reporte-semanal --location=$REGION --update-headers=...`.

## 9. App Android: URL de la API y firma de la release

### 9.1 URL de la API

La release lee la URL al compilar, de la propiedad `reciclakids.apiUrl` o de la variable de entorno
`RECICLAKIDS_API_URL`. No es secreta. Es la URL del paso 7.4 más `/api/v1/`:

```bash
export RECICLAKIDS_API_URL="$URL/api/v1/"
```

### 9.2 Crear el keystore (una sola vez)

El keystore firma la app. **Si se pierde, no se pueden publicar actualizaciones en Google Play**, así que
guárdalo con copia de respaldo en un lugar seguro del equipo (no en el repositorio):

```bash
keytool -genkeypair -v -keystore reciclakids.jks -keyalg RSA -keysize 2048 -validity 10000 -alias reciclakids
```

`keytool` pide la contraseña del keystore, la de la llave y unos datos del responsable. Guarda en el
gestor de contraseñas del equipo: el archivo `.jks`, las dos contraseñas y el alias (`reciclakids`).

### 9.3 Compilar la release firmada

```bash
export RECICLAKIDS_KEYSTORE=/ruta/segura/reciclakids.jks
read -rsp "Contraseña del keystore: " RECICLAKIDS_KEYSTORE_CLAVE; echo; export RECICLAKIDS_KEYSTORE_CLAVE
export RECICLAKIDS_KEY_ALIAS=reciclakids
read -rsp "Contraseña de la llave: " RECICLAKIDS_KEY_CLAVE; echo; export RECICLAKIDS_KEY_CLAVE
cd app-movil
./gradlew bundleRelease          # AAB para Google Play: app/build/outputs/bundle/release/
./gradlew assembleRelease        # APK para instalar a mano: app/build/outputs/apk/release/
```

Con las cuatro variables, la compilación **falla** si la URL falta o no es HTTPS (así no se distribuye
una app apuntando a ninguna parte).

### 9.4 Distribuir al piloto

- **Google Play, prueba interna** (recomendado): cuenta de desarrollador (pago único de USD 25),
  crea la app, activa **Play App Signing** y sube el AAB a *Internal testing* con los correos de los
  probadores.
- **APK a mano**: instala el APK firmado en cada teléfono (hay que permitir "orígenes desconocidos").

## 10. Secretos y variables de GitHub

Se crean en **Settings → Secrets and variables → Actions** del repositorio, o con `gh` (pide el valor sin
mostrarlo). Usa **Secrets** para lo secreto y **Variables** para lo que no lo es.

### 10.1 TestRail (ya configurados)

| Secreto | Valor |
|---|---|
| `TESTRAIL_URL` | `https://<organizacion>.testrail.io` (sin barra final) |
| `TESTRAIL_USER` | Correo de la cuenta de TestRail |
| `TESTRAIL_API_KEY` | Clave de API: en TestRail, tu avatar → **My Settings → API Keys → Add Key** |

```bash
gh secret set TESTRAIL_API_KEY --repo jecayonm8/reciclakids
```

Si el CI responde `Authentication failed`, la clave o el correo están mal. Si responde
`The API is disabled`, un administrador de TestRail debe activarla en **Administration → Site Settings → API**.

### 10.2 Firma de la release en la CI (solo si se automatiza la compilación)

Hoy la release se compila en la máquina de quien publica (§9.3). Si se agrega un workflow que la
compile, necesita:

| Nombre | Tipo | Valor |
|---|---|---|
| `RECICLAKIDS_KEYSTORE_BASE64` | Secreto | El `.jks` en base64: `base64 -w0 reciclakids.jks` |
| `RECICLAKIDS_KEYSTORE_CLAVE` | Secreto | Contraseña del keystore |
| `RECICLAKIDS_KEY_ALIAS` | Secreto | `reciclakids` |
| `RECICLAKIDS_KEY_CLAVE` | Secreto | Contraseña de la llave |
| `RECICLAKIDS_API_URL` | Variable | `https://…run.app/api/v1/` |

```bash
base64 -w0 reciclakids.jks | gh secret set RECICLAKIDS_KEYSTORE_BASE64 --repo jecayonm8/reciclakids
gh secret set RECICLAKIDS_KEYSTORE_CLAVE --repo jecayonm8/reciclakids
gh secret set RECICLAKIDS_KEY_ALIAS --repo jecayonm8/reciclakids
gh secret set RECICLAKIDS_KEY_CLAVE --repo jecayonm8/reciclakids
gh variable set RECICLAKIDS_API_URL --repo jecayonm8/reciclakids --body "$URL/api/v1/"
```

El workflow decodifica el keystore a un archivo temporal (`echo "$RECICLAKIDS_KEYSTORE_BASE64" | base64 -d > reciclakids.jks`)
y apunta `RECICLAKIDS_KEYSTORE` a ese archivo.

### 10.3 Despliegue del backend desde GitHub Actions (opcional)

Hoy el backend se despliega a mano (§7). Para desplegar con un botón desde GitHub **sin guardar ninguna
llave de Google**, se usa **Workload Identity Federation**: GitHub demuestra su identidad con un token
OIDC de corta vida y Google le presta una cuenta de servicio solo a este repositorio.

**1. Pool y proveedor de identidad** (en GCP):

```bash
NUMERO=$(gcloud projects describe $PROYECTO --format='value(projectNumber)')
gcloud iam workload-identity-pools create github --location=global --display-name="GitHub Actions"
gcloud iam workload-identity-pools providers create-oidc reciclakids --location=global \
  --workload-identity-pool=github --display-name="Repositorio ReciclaKids" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
  --attribute-condition="assertion.repository=='jecayonm8/reciclakids'"
```

**2. Cuenta de servicio de despliegue**, con permiso para subir imágenes, desplegar en Cloud Run y usar la
cuenta del backend:

```bash
gcloud iam service-accounts create reciclakids-despliegue --display-name="Despliegue desde GitHub"
DESPLIEGUE=reciclakids-despliegue@$PROYECTO.iam.gserviceaccount.com
gcloud projects add-iam-policy-binding $PROYECTO --member="serviceAccount:$DESPLIEGUE" --role="roles/run.admin"
gcloud projects add-iam-policy-binding $PROYECTO --member="serviceAccount:$DESPLIEGUE" --role="roles/artifactregistry.writer"
gcloud iam service-accounts add-iam-policy-binding $CUENTA_RUN \
  --member="serviceAccount:$DESPLIEGUE" --role="roles/iam.serviceAccountUser"
gcloud iam service-accounts add-iam-policy-binding $DESPLIEGUE --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/$NUMERO/locations/global/workloadIdentityPools/github/attribute.repository/jecayonm8/reciclakids"
```

**3. Secretos y variables en GitHub:**

```bash
gh secret set GCP_PROVEEDOR_WIF --repo jecayonm8/reciclakids \
  --body "projects/$NUMERO/locations/global/workloadIdentityPools/github/providers/reciclakids"
gh secret set GCP_CUENTA_DESPLIEGUE --repo jecayonm8/reciclakids --body "$DESPLIEGUE"
gh variable set GCP_PROYECTO --repo jecayonm8/reciclakids --body "$PROYECTO"
gh variable set GCP_REGION --repo jecayonm8/reciclakids --body "$REGION"
```

**4. Workflow** (plantilla: todavía **no está en el repositorio** y hay que probarlo al agregarlo en un
PR). Archivo `.github/workflows/backend-despliegue.yml`, que solo corre a mano desde la pestaña Actions:

```yaml
name: Despliegue del backend
on: workflow_dispatch

permissions:
  contents: read
  id-token: write   # para el token OIDC de Workload Identity Federation

jobs:
  desplegar:
    runs-on: ubuntu-latest
    env:
      IMAGEN: ${{ vars.GCP_REGION }}-docker.pkg.dev/${{ vars.GCP_PROYECTO }}/reciclakids/backend:${{ github.sha }}
    steps:
      - uses: actions/checkout@v4
      - uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: ${{ secrets.GCP_PROVEEDOR_WIF }}
          service_account: ${{ secrets.GCP_CUENTA_DESPLIEGUE }}
      - uses: google-github-actions/setup-gcloud@v2
      - name: Construir y subir la imagen
        run: |
          gcloud auth configure-docker ${{ vars.GCP_REGION }}-docker.pkg.dev --quiet
          docker build -t "$IMAGEN" backend
          docker push "$IMAGEN"
      - name: Desplegar en Cloud Run
        # Solo cambia la imagen: variables, secretos y cuenta de servicio quedan como en §7.
        run: gcloud run deploy reciclakids-backend --image "$IMAGEN" --region ${{ vars.GCP_REGION }}
```

### 10.4 Resumen de lo que debe existir en GitHub

| Nombre | Tipo | Estado |
|---|---|---|
| `TESTRAIL_URL`, `TESTRAIL_USER`, `TESTRAIL_API_KEY` | Secretos | Ya configurados |
| `RECICLAKIDS_KEYSTORE_BASE64`, `RECICLAKIDS_KEYSTORE_CLAVE`, `RECICLAKIDS_KEY_ALIAS`, `RECICLAKIDS_KEY_CLAVE` | Secretos | Solo si la CI compila la release |
| `RECICLAKIDS_API_URL` | Variable | Solo si la CI compila la release |
| `GCP_PROVEEDOR_WIF`, `GCP_CUENTA_DESPLIEGUE` | Secretos | Solo para desplegar desde GitHub |
| `GCP_PROYECTO`, `GCP_REGION` | Variables | Solo para desplegar desde GitHub |

Los secretos de base de datos, JWT, tareas y correo (SendGrid o AWS) **no van en GitHub**: viven solo en Secret Manager y
Cloud Run los lee de ahí.

## 11. Comprobar que todo quedó bien

- [ ] `curl $URL/salud` responde `{"estado":"ok"}`.
- [ ] Los registros de Cloud Run muestran las migraciones de Flyway aplicadas y ningún error al arrancar.
      En la consola: **Cloud Run → reciclakids-backend → Registros**, o con
      `gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="reciclakids-backend"' --limit=50`.
- [ ] Desde la app release: registrar una docente, agregar un niño, publicar un reto y entrar con el
      código en otro teléfono.
- [ ] Llega el correo de recuperación de contraseña y no cae en spam (con SES, revisa SPF/DKIM/DMARC en los
      encabezados; con SendGrid, que el remitente sea el verificado).
- [ ] `gcloud scheduler jobs run reporte-semanal --location=$REGION` y en los registros de Cloud Run la
      tarea responde 200.
- [ ] Un push a `main` sube los resultados a TestRail sin avisos.

## 12. Rotar una credencial

| Qué | Cómo | Efecto |
|---|---|---|
| Contraseña de la base | `gcloud sql users set-password reciclakids --instance=$INSTANCIA --password=…`, luego nueva versión de `database-password` y nueva revisión de Cloud Run | Sin efecto para los usuarios |
| `jwt-secret` | Nueva versión del secreto y nueva revisión de Cloud Run | **Cierra todas las sesiones**: docentes y acudientes vuelven a iniciar sesión; los niños vuelven a escribir el código |
| `tareas-clave` | Nueva versión, nueva revisión y actualizar el encabezado de Cloud Scheduler (§8) | Ninguno si se hace todo junto |
| Clave de SendGrid | Crear otra clave en **Settings → API Keys**, nueva versión de `sendgrid-api-key`, nueva revisión de Cloud Run y **después** borrar la vieja | Ninguno |
| Llaves de AWS (si usas SES) | Crear una llave nueva en IAM, nueva versión de los dos secretos, nueva revisión, y **después** desactivar la vieja | Ninguno |
| Clave de TestRail | Crear otra en **My Settings → API Keys**, actualizar `TESTRAIL_API_KEY` y borrar la vieja | Ninguno |

Una versión nueva de un secreto:

```bash
read -rsp "Nuevo valor: " V; echo; printf '%s' "$V" | gcloud secrets versions add jwt-secret --data-file=-; unset V
```

Cloud Run lee `latest` al arrancar cada instancia. Para que tome el valor nuevo de inmediato, fuerza una
revisión: `gcloud run services update reciclakids-backend --region $REGION --update-env-vars ROTADO=$(date +%s)`.

## 13. Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| Cloud Run no arranca: `JWT_SECRET debe tener al menos 32 caracteres en producción` | Falta el secreto o es corto | Revisa §6.1 y el `--set-secrets` de §7.3 |
| `RECICLAKIDS_URL_PUBLICA debe usar HTTPS` | URL provisional sin `https://` | §7.2 y §7.4 |
| `Permission denied on secret` en los registros | La cuenta del backend no tiene acceso | §6.2 |
| `Connection refused` o tiempo de espera con la base | Falta `roles/cloudsql.client` o el nombre de conexión está mal | §3.4 y §4.3 |
| Al crear la instancia, un error de que el tier no es válido para la edición | Falta `--edition=ENTERPRISE` | §4.1 |
| En los registros: `SendGrid rechazó el envío (estado 403)` | La clave no tiene «Mail Send» o el remitente no está verificado en SendGrid | §5.A |
| En los registros: `SendGrid rechazó el envío (estado 401)` | La clave de API es incorrecta o se borró | §5.A y §12 |
| El servidor no arranca: `Con CORREO_PROVEEDOR=sendgrid hace falta SENDGRID_API_KEY` | Falta el secreto en `--set-secrets` | §6 y §7.3 |
| Los correos caen en spam | Remitente sin dominio autenticado (o `@gmail.com`) | §5.A |
| `MessageRejected: Email address is not verified` | SES sigue en el sandbox o el dominio no está verificado | §5.B.1 y §5.B.2 |
| `AccessDenied ... ses:SendEmail` | La política de IAM apunta a otro dominio o región | §5.B.3 |
| La tarea del viernes responde 401 | El encabezado de Cloud Scheduler no coincide con `tareas-clave` | §8 |
| La app release no conecta | Se compiló sin `RECICLAKIDS_API_URL` o con HTTP | §9.1 |
| GitHub: `Unable to acquire impersonated credentials` | Falta `roles/iam.workloadIdentityUser` o la condición del repositorio no coincide | §10.3 |
