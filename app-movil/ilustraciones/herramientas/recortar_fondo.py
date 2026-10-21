#!/usr/bin/env python3
"""Quita el fondo claro de una ilustración y deja un PNG transparente con el borde suave.

Es la misma técnica con la que se recortaron las ilustraciones del 0.2 UI:
  1. El fondo blanco que toca el borde de la imagen se marca como «fondo seguro» (inundación).
  2. Un modelo de segmentación (rembg) ayuda a decidir qué es personaje y qué es fondo.
  3. Un trimap (fondo / personaje / franja dudosa) alimenta un matting que calcula la
     transparencia solo en el borde (por eso queda suave y sin halo blanco).
  4. Se encoge el borde 1 px y se limpia el color de la franja para que no quede hilo blanco.
  5. Se recorta al contenido.

Uso:
    pip install rembg onnxruntime pymatting opencv-python pillow numpy scipy
    python recortar_fondo.py entrada.png salida.png            # personaje (anime/cartoon)
    python recortar_fondo.py caneca.jpg caneca.png --objeto    # objetos (caneca, botella…)
    python recortar_fondo.py img.png out.png --tol 24          # fondo con sombra gris suave

Opciones útiles:
    --tol N     cuánto se parece al fondo para borrarlo (14 blanco puro; 22-32 si hay sombra).
    --ampliar N escala previa (2 = mejor borde en imágenes pequeñas; 1 si ya son grandes).
    --maximo PX lado máximo de salida (para no engordar el APK).
La primera vez rembg descarga el modelo (~170 MB) a ~/.u2net.
"""
import argparse

import cv2
import numpy as np
from PIL import Image, ImageFilter
from pymatting import estimate_alpha_cf, estimate_foreground_ml
from rembg import new_session, remove


def recortar(ruta, ampliar=2, tol=14, objeto=False, borde=1):
    modelo = "isnet-general-use" if objeto else "isnet-anime"
    sesion = new_session(modelo)
    im = Image.open(ruta).convert("RGB")
    # Los mockups exportados traen una línea gris en el borde: se descartan 2 px.
    im = im.crop((2, 2, im.width - 2, im.height - 2))
    if ampliar != 1:
        im = im.resize((im.width * ampliar, im.height * ampliar), Image.LANCZOS)
    rgb = np.array(im)
    h, w, _ = rgb.shape

    # 1) máscara del modelo y fondo seguro por inundación desde el borde
    bruta = np.array(remove(im, session=sesion, only_mask=True, post_process_mask=True)).astype(np.float32) / 255
    color_fondo = np.median(np.concatenate([rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]]), axis=0)
    parecido = (np.abs(rgb.astype(int) - color_fondo.astype(int)).max(axis=2) <= tol).astype(np.uint8)
    _, etiquetas = cv2.connectedComponents(parecido, connectivity=4)
    del_borde = set(np.unique(np.concatenate([etiquetas[0], etiquetas[-1], etiquetas[:, 0], etiquetas[:, -1]]))) - {0}
    fondo = np.isin(etiquetas, list(del_borde))

    # 2) trimap: 0 fondo, 1 personaje, 0.5 franja dudosa
    fondo_seguro = cv2.erode(fondo.astype(np.uint8), np.ones((3, 3), np.uint8)).astype(bool)
    k = max(3, ampliar * 2)
    personaje_seguro = cv2.erode((~fondo).astype(np.uint8), np.ones((k, k), np.uint8)).astype(bool)
    trimap = np.full((h, w), 0.5)
    trimap[fondo_seguro] = 0.0
    trimap[personaje_seguro] = 1.0
    alfa = np.clip(estimate_alpha_cf(rgb.astype(np.float64) / 255, trimap), 0, 1)

    # 3) para objetos con sombra: limitarse a la silueta que ve el modelo
    if objeto:
        from scipy.ndimage import binary_fill_holes
        silueta = binary_fill_holes(bruta > 0.5).astype(np.uint8)
        alfa = alfa * cv2.dilate(silueta, np.ones((2 * ampliar + 1, 2 * ampliar + 1), np.uint8))

    # 4) quitar motas sueltas
    n, lab, st, _ = cv2.connectedComponentsWithStats((alfa > 0.5).astype(np.uint8), connectivity=8)
    if n > 1:
        areas = st[1:, cv2.CC_STAT_AREA]
        grandes = [i + 1 for i, a in enumerate(areas) if a >= 0.02 * areas.max()]
        cerca = cv2.dilate(np.isin(lab, grandes).astype(np.uint8), np.ones((7, 7), np.uint8))
        alfa = alfa * cerca

    # 5) borde: encoger 1 px (quita el hilo blanco) y suavizar
    if borde:
        alfa = cv2.erode(alfa.astype(np.float32), cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * borde + 1, 2 * borde + 1)))
    alfa = np.array(Image.fromarray((alfa * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.9))).astype(np.float32) / 255
    alfa[alfa < 0.04] = 0
    alfa[alfa > 0.96] = 1
    color = np.clip(estimate_foreground_ml(rgb.astype(np.float64) / 255, alfa.astype(np.float64)), 0, 1)
    salida = Image.fromarray((np.dstack([color, alfa]) * 255 + 0.5).astype(np.uint8), "RGBA")
    caja = salida.split()[-1].point(lambda v: 255 if v > 8 else 0).getbbox()
    if caja:
        m = 4
        salida = salida.crop((max(caja[0] - m, 0), max(caja[1] - m, 0), min(caja[2] + m, salida.width), min(caja[3] + m, salida.height)))
    return salida


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description="Recorta el fondo claro de una ilustración (PNG transparente).")
    ap.add_argument("entrada")
    ap.add_argument("salida")
    ap.add_argument("--tol", type=int, default=14)
    ap.add_argument("--ampliar", type=int, default=2)
    ap.add_argument("--maximo", type=int, default=0)
    ap.add_argument("--objeto", action="store_true")
    a = ap.parse_args()
    r = recortar(a.entrada, ampliar=a.ampliar, tol=a.tol, objeto=a.objeto)
    if a.maximo and max(r.size) > a.maximo:
        r.thumbnail((a.maximo, a.maximo), Image.LANCZOS)
    r.save(a.salida, optimize=True)
    print(f"Listo: {a.salida} {r.size}")
