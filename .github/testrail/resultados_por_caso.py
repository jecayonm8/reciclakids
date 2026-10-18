"""Convierte los resultados JUnit de Gradle en un resultado por caso de prueba CP de TestRail.

Cada caso CP (CP-001…) lista en casos-automatizados.json las pruebas automáticas que lo cubren,
por proyecto («app» o «backend»). Este script lee los XML de Gradle de un proyecto, resume las
pruebas de cada caso (falla si alguna falló) y escribe un JUnit nuevo con un caso por CP, con el
nombre «[C<id>] CP-xxx: título». trcli lo sube con --case-matcher name, así que los resultados
quedan en los casos legibles que ya existen y no se crean casos técnicos.

El ID de TestRail de cada CP se busca por título con la API, con las mismas credenciales del CI
(TESTRAIL_URL, TESTRAIL_USER, TESTRAIL_API_KEY): un caso que se importe después queda conectado solo.

Uso: python3 resultados_por_caso.py --proyecto backend --resultados backend/build/test-results/test \
         --salida resultados-testrail.xml
"""
import argparse
import base64
import json
import os
import pathlib
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET

AQUI = pathlib.Path(__file__).resolve().parent
NOMBRE_PROYECTO = "ReciclaKids"
TITULO_CP = re.compile(r"^(CP-\d{3})\b")


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


def pedir(url: str, autorizacion: str, ruta: str):
    peticion = urllib.request.Request(f"{url}/index.php?/api/v2/{ruta}")
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


def casos_cp() -> dict[str, tuple[int, str]]:
    """«CP-001» → (id de TestRail, título), leídos de la API."""
    url = os.environ["TESTRAIL_URL"].rstrip("/")
    credencial = f"{os.environ['TESTRAIL_USER']}:{os.environ['TESTRAIL_API_KEY']}"
    autorizacion = "Basic " + base64.b64encode(credencial.encode()).decode()
    proyectos = paginas(url, autorizacion, "get_projects", "projects")
    proyecto = next((p for p in proyectos if p["name"].strip().lower() == NOMBRE_PROYECTO.lower()), None)
    if proyecto is None:
        raise SystemExit(f"No existe el proyecto «{NOMBRE_PROYECTO}» en TestRail")
    encontrados: dict[str, tuple[int, str]] = {}
    for suite in paginas(url, autorizacion, f"get_suites/{proyecto['id']}", "suites"):
        for caso in paginas(url, autorizacion, f"get_cases/{proyecto['id']}&suite_id={suite['id']}", "cases"):
            coincidencia = TITULO_CP.match(caso["title"])
            if coincidencia:
                encontrados.setdefault(coincidencia.group(1), (caso["id"], caso["title"]))
    return encontrados


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
    argumentos.add_argument("--proyecto", required=True, choices=["app", "backend"])
    argumentos.add_argument("--resultados", required=True, type=pathlib.Path)
    argumentos.add_argument("--mapa", type=pathlib.Path, default=AQUI / "casos-automatizados.json")
    argumentos.add_argument("--salida", required=True, type=pathlib.Path)
    opciones = argumentos.parse_args()

    mapa = json.loads(opciones.mapa.read_text(encoding="utf-8"))
    resultados = leer_resultados(opciones.resultados)
    if not resultados:
        raise SystemExit(f"No hay resultados JUnit en {opciones.resultados}")
    ids = casos_cp()

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
