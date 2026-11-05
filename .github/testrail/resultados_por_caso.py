"""Convierte los resultados JUnit de Gradle en un resultado por caso de prueba CP de TestRail.

Cada caso CP (CP-001…) lista en casos-automatizados.json las pruebas automáticas que lo cubren,
por proyecto («app» o «backend»). Este script lee los XML de Gradle de un proyecto, resume las
pruebas de cada caso (falla si alguna falló) y escribe un JUnit nuevo con un caso por CP, con el
nombre «[C<id>] CP-xxx: título». trcli lo sube con --case-matcher name, así que los resultados
quedan en los casos legibles que ya existen y no se crean casos técnicos.

El ID de TestRail de cada CP se busca por título con la API, con las mismas credenciales del CI
(TESTRAIL_URL, TESTRAIL_USER, TESTRAIL_API_KEY): un caso que se importe después queda conectado solo.

Con --crear-faltantes, antes crea en TestRail los casos de ese CSV (mismas columnas que el CSV de
importación) que todavía no existan, en la sección que dice su columna Section. No duplica: un caso
cuyo título ya empieza por su CP se deja como está. Sin --resultados, solo crea.

Uso: python3 resultados_por_caso.py --proyecto backend --resultados backend/build/test-results/test \
         --salida resultados-testrail.xml
     python3 resultados_por_caso.py --crear-faltantes .github/testrail/casos-para-crear.csv
"""
import argparse
import base64
import csv
import json
import os
import pathlib
import re
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET

AQUI = pathlib.Path(__file__).resolve().parent
NOMBRE_PROYECTO = "ReciclaKids"
TITULO_CP = re.compile(r"^(CP-\d{3})\b")

# Los mismos valores que usa la configuración de importación por CSV de este TestRail.
PRIORIDADES = {"high": 3, "medium": 2}
TIPOS = {
    "funcional": 6, "funcional (interfaz)": 6, "funcional (sistema)": 6, "funcional e integración": 6,
    "funcional y seguridad": 10, "integración": 7, "integración y seguridad": 10, "seguridad e integración": 10,
    "sistema": 6, "unitaria": 7, "unitaria (interfaz)": 7, "unitaria y funcional": 7,
    "unitaria y funcional (interfaz)": 7, "unitaria y sistema": 7,
}
# Columna del CSV → campo del caso en TestRail (plantilla «Test Case (Text)»).
CAMPOS_TEXTO = {
    "References": "refs",
    "Preconditions": "custom_preconds",
    "Steps": "custom_steps",
    "Expected Result": "custom_expected",
    "Test Data": "custom_case_datos_prueba",
    "Technique": "custom_case_tecnica",
    "Automated Test": "custom_case_prueba_auto_ci",
}


def aviso(mensaje: str) -> None:
    """Aviso visible en el resumen de la corrida de GitHub Actions."""
    print(f"::warning title=TestRail::{mensaje}")


def leer_resultados(carpeta: pathlib.Path) -> dict[str, str]:
    """«Clase.metodo» → passed, failed o skipped, desde los XML JUnit de Gradle."""
    resultados: dict[str, str] = {}
    for archivo in sorted(carpeta.glob("*.xml")):
        for caso in ET.parse(archivo).getroot().iter("testcase"):
            clase = caso.get("classname", "").rsplit(".", 1)[-1]
            # JUnit 5 (backend) escribe «metodo()»; JUnit 4 (app) solo «metodo».
            metodo = re.sub(r"\(.*\)$", "", caso.get("name", "")).strip()
            if caso.find("failure") is not None or caso.find("error") is not None:
                estado = "failed"
            elif caso.find("skipped") is not None:
                estado = "skipped"
            else:
                estado = "passed"
            clave = f"{clase}.{metodo}"
            # Si una prueba aparece dos veces (reintento), manda el peor resultado.
            if resultados.get(clave) != "failed":
                resultados[clave] = estado
    return resultados


def pedir(url: str, autorizacion: str, ruta: str, datos: dict | None = None):
    """GET a la API de TestRail, o POST si hay [datos]."""
    cuerpo = json.dumps(datos).encode() if datos is not None else None
    peticion = urllib.request.Request(f"{url}/index.php?/api/v2/{ruta}", data=cuerpo)
    peticion.add_header("Authorization", autorizacion)
    peticion.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(peticion, timeout=30) as respuesta:
        return json.loads(respuesta.read().decode())


def paginas(url: str, autorizacion: str, ruta: str, clave: str) -> list:
    elementos, siguiente = [], ruta
    while siguiente:
        datos = pedir(url, autorizacion, siguiente)
        if isinstance(datos, list):
            return datos
        elementos += datos.get(clave, [])
        enlace = (datos.get("_links") or {}).get("next")
        siguiente = enlace.split("/api/v2/", 1)[1] if enlace else None
    return elementos


def conectar() -> tuple[str, str, int]:
    """URL, encabezado de autorización e id del proyecto, con las credenciales del CI."""
    url = os.environ["TESTRAIL_URL"].rstrip("/")
    credencial = f"{os.environ['TESTRAIL_USER']}:{os.environ['TESTRAIL_API_KEY']}"
    autorizacion = "Basic " + base64.b64encode(credencial.encode()).decode()
    proyectos = paginas(url, autorizacion, "get_projects", "projects")
    proyecto = next((p for p in proyectos if p["name"].strip().lower() == NOMBRE_PROYECTO.lower()), None)
    if proyecto is None:
        raise SystemExit(f"No existe el proyecto «{NOMBRE_PROYECTO}» en TestRail")
    return url, autorizacion, proyecto["id"]


def casos_cp(url: str, autorizacion: str, proyecto: int) -> dict[str, tuple[int, str]]:
    """«CP-001» → (id de TestRail, título), leídos de la API."""
    encontrados: dict[str, tuple[int, str]] = {}
    for suite in paginas(url, autorizacion, f"get_suites/{proyecto}", "suites"):
        for caso in paginas(url, autorizacion, f"get_cases/{proyecto}&suite_id={suite['id']}", "cases"):
            coincidencia = TITULO_CP.match(caso["title"])
            if coincidencia:
                encontrados.setdefault(coincidencia.group(1), (caso["id"], caso["title"]))
    return encontrados


def datos_del_caso(fila: dict[str, str]) -> dict:
    """Una fila del CSV de importación → cuerpo de add_case."""
    datos = {
        "title": fila["Title"].strip(),
        "template_id": 1,
        "priority_id": PRIORIDADES[fila["Priority"].strip().lower()],
        "type_id": TIPOS[fila["Type"].strip().lower()],
    }
    for columna, campo in CAMPOS_TEXTO.items():
        if fila.get(columna, "").strip():
            datos[campo] = fila[columna].strip()
    return datos


def crear_faltantes(url: str, autorizacion: str, proyecto: int, definiciones: pathlib.Path,
                    existentes: dict[str, tuple[int, str]]) -> int:
    """Crea los casos de [definiciones] que no están en [existentes] y los agrega. Devuelve los fallos."""
    with definiciones.open(encoding="utf-8-sig", newline="") as archivo:
        filas = [fila for fila in csv.DictReader(archivo) if fila["ID"].strip() not in existentes]
    if not filas:
        print(f"Los casos de {definiciones.name} ya están en TestRail.")
        return 0
    secciones: dict[str, int] = {}
    for suite in paginas(url, autorizacion, f"get_suites/{proyecto}", "suites"):
        for seccion in paginas(url, autorizacion, f"get_sections/{proyecto}&suite_id={suite['id']}", "sections"):
            secciones.setdefault(seccion["name"].strip().lower(), seccion["id"])
    fallos = 0
    for fila in filas:
        cp = fila["ID"].strip()
        seccion = secciones.get(fila["Section"].strip().lower())
        if seccion is None:
            aviso(f"{cp}: no existe la sección «{fila['Section']}» en TestRail; no lo creé.")
            fallos += 1
            continue
        try:
            caso = pedir(url, autorizacion, f"add_case/{seccion}", datos_del_caso(fila))
        except urllib.error.HTTPError as error:
            aviso(f"{cp}: TestRail respondió {error.code}: {error.read().decode(errors='replace')[:300]}")
            fallos += 1
            continue
        existentes[cp] = (caso["id"], caso["title"])
        print(f"::notice title=TestRail::Creé {cp} como C{caso['id']} en «{fila['Section']}».")
    return fallos


def resumir(cp: str, pruebas: list[str], resultados: dict[str, str]) -> tuple[str | None, list[str], list[str]]:
    """Estado del caso, pruebas que corrieron y pruebas que no aparecen en los resultados."""
    corrieron, faltan, estados = [], [], []
    for prueba in pruebas:
        if "." in prueba:
            encontradas = {prueba: resultados[prueba]} if prueba in resultados else {}
        else:  # una clase entera
            encontradas = {k: v for k, v in resultados.items() if k.startswith(prueba + ".")}
        if not encontradas:
            faltan.append(prueba)
        for nombre, estado in encontradas.items():
            corrieron.append(f"{nombre}: {estado}")
            estados.append(estado)
    if not estados:
        return None, corrieron, faltan
    if "failed" in estados:
        return "failed", corrieron, faltan
    return ("passed" if "passed" in estados else "skipped"), corrieron, faltan


def main() -> int:
    argumentos = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    argumentos.add_argument("--proyecto", choices=["app", "backend"])
    argumentos.add_argument("--resultados", type=pathlib.Path)
    argumentos.add_argument("--mapa", type=pathlib.Path, default=AQUI / "casos-automatizados.json")
    argumentos.add_argument("--salida", type=pathlib.Path)
    argumentos.add_argument("--crear-faltantes", type=pathlib.Path, metavar="CSV")
    opciones = argumentos.parse_args()
    solo_crear = opciones.resultados is None
    if solo_crear and opciones.crear_faltantes is None:
        argumentos.error("hace falta --resultados (con --proyecto y --salida) o --crear-faltantes")
    if not solo_crear and (opciones.proyecto is None or opciones.salida is None):
        argumentos.error("--resultados necesita --proyecto y --salida")

    url, autorizacion, proyecto = conectar()
    ids = casos_cp(url, autorizacion, proyecto)
    if opciones.crear_faltantes:
        fallos = crear_faltantes(url, autorizacion, proyecto, opciones.crear_faltantes, ids)
        if solo_crear:
            return 1 if fallos else 0

    mapa = json.loads(opciones.mapa.read_text(encoding="utf-8"))
    resultados = leer_resultados(opciones.resultados)
    if not resultados:
        raise SystemExit(f"No hay resultados JUnit en {opciones.resultados}")

    suite = ET.Element("testsuite", name=f"{NOMBRE_PROYECTO} · {opciones.proyecto}")
    resumen = {"passed": 0, "failed": 0, "skipped": 0}
    for cp, partes in mapa.items():
        pruebas = partes.get(opciones.proyecto)
        if not pruebas:
            continue  # este caso lo cubre solo el otro proyecto
        if cp not in ids:
            aviso(f"{cp} no está en TestRail todavía (impórtalo desde el CSV); su resultado no se sube.")
            continue
        estado, corrieron, faltan = resumir(cp, pruebas, resultados)
        if faltan:
            aviso(f"{cp}: no encontré {', '.join(faltan)} en los resultados de {opciones.proyecto}. "
                  "¿Se renombró la prueba? Actualiza .github/testrail/casos-automatizados.json.")
        if estado is None:
            continue
        caso_id, titulo = ids[cp]
        caso = ET.SubElement(suite, "testcase", classname=NOMBRE_PROYECTO, name=f"[C{caso_id}] {titulo}")
        detalle = "Pruebas automáticas:\n" + "\n".join(corrieron)
        if estado == "failed":
            fallidas = [linea.split(":")[0] for linea in corrieron if linea.endswith(": failed")]
            ET.SubElement(caso, "failure", message="Fallaron: " + ", ".join(fallidas)).text = detalle
        elif estado == "skipped":
            ET.SubElement(caso, "skipped")
        ET.SubElement(caso, "system-out").text = detalle
        resumen[estado] += 1

    suite.set("tests", str(sum(resumen.values())))
    suite.set("failures", str(resumen["failed"]))
    suite.set("skipped", str(resumen["skipped"]))
    raiz = ET.Element("testsuites")
    raiz.append(suite)
    ET.ElementTree(raiz).write(opciones.salida, encoding="utf-8", xml_declaration=True)
    print(f"Casos CP de {opciones.proyecto}: {resumen['passed']} pasaron, {resumen['failed']} fallaron, "
          f"{resumen['skipped']} omitidos; queda en {opciones.salida}")
    return 0 if sum(resumen.values()) else 1


if __name__ == "__main__":
    sys.exit(main())
