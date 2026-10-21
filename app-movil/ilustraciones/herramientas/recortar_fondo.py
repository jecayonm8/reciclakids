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
    python recortar_fondo.py ficha.png out.png --ampliar 1 --recorte 0 --motas 0.0008
                                                              # IA 1024 px con estrellitas o «z» sueltas
    python recortar_fondo.py insignia.png out.png --circulo   # medallas redondas (insignias, acuario, logo)

Opciones útiles:
    --tol N     cuánto se parece al fondo para borrarlo (14 blanco puro; 22-32 si hay sombra).
    --ampliar N escala previa (2 = mejor borde en imágenes pequeñas; 1 si ya son grandes).
    --maximo PX lado máximo de salida (para no engordar el APK).
    --recorte N píxeles que se descartan en cada borde (2 por defecto, por la línea gris de los mockups; 0 en
                imágenes de IA).
    --motas F   tamaño mínimo de una pieza suelta, como fracción de la pieza más grande (0.02 por defecto;
                0.0008 conserva estrellitas y «z»).
    --paleta    guarda con 256 colores (PNG hasta 8 veces más liviano; úsalo en personajes y objetos, no en
                medallas con degradados porque se nota el escalonado).
    --circulo   para medallas redondas: en vez de adivinar el borde, calcula el círculo y lo recorta limpio.
La primera vez rembg descarga el modelo (~170 MB) a ~/.u2net.
"""
import argparse

import cv2
import numpy as np
from PIL import Image, ImageFilter
from pymatting import estimate_alpha_cf, estimate_foreground_ml
from rembg import new_session, remove


def recortar(ruta, ampliar=2, tol=14, objeto=False, borde=1, motas=0.02, recorte=2):
    modelo = "isnet-general-use" if objeto else "isnet-anime"
    sesion = new_session(modelo)
    im = Image.open(ruta).convert("RGB")
    # Los mockups exportados traen una línea gris en el borde: se descartan 2 px.
    if recorte:
        im = im.crop((recorte, recorte, im.width - recorte, im.height - recorte))
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
        grandes = [i + 1 for i, a in enumerate(areas) if a >= motas * areas.max()]
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


def recortar_circulo(ruta, margen=1.5, umbral=10):
    """Medalla redonda sobre fondo blanco: calcula el círculo (área y centro de lo que no es blanco)
    y lo recorta con borde suave. Devuelve la imagen y un diagnóstico (blanco dentro, color fuera)."""
    from scipy.ndimage import binary_fill_holes
    im = Image.open(ruta).convert("RGB")
    rgb = np.array(im)
    h, w, _ = rgb.shape
    no_blanco = binary_fill_holes((255 - rgb.astype(int)).max(axis=2) > umbral)
    ys, xs = np.nonzero(no_blanco)
    cx, cy = xs.mean() + 0.5, ys.mean() + 0.5
    r = np.sqrt(no_blanco.sum() / np.pi) - margen
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.hypot(xx + 0.5 - cx, yy + 0.5 - cy)
    mascara = np.clip(r - d + 0.5, 0, 1).astype(np.float32)   # borde suave de ~1 px
    salida = Image.fromarray(np.dstack([rgb, (mascara * 255 + 0.5).astype(np.uint8)]), "RGBA")
    caja = salida.split()[-1].point(lambda v: 255 if v > 8 else 0).getbbox()
    salida = salida.crop(caja)
    fuera = (no_blanco & (mascara < 0.5)).sum() / max(no_blanco.sum(), 1)
    dentro_blanco = (((255 - rgb.astype(int)).max(axis=2) <= 3) & (mascara > 0.5)).sum() / max((mascara > 0.5).sum(), 1)
    return salida, {"radio": round(float(r), 1), "color_fuera": round(float(fuera), 4), "blanco_dentro": round(float(dentro_blanco), 4)}


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description="Recorta el fondo claro de una ilustración (PNG transparente).")
    ap.add_argument("entrada")
    ap.add_argument("salida")
    ap.add_argument("--tol", type=int, default=14)
    ap.add_argument("--ampliar", type=int, default=2)
    ap.add_argument("--maximo", type=int, default=0)
    ap.add_argument("--objeto", action="store_true")
    ap.add_argument("--recorte", type=int, default=2)
    ap.add_argument("--motas", type=float, default=0.02)
    ap.add_argument("--circulo", action="store_true")
    ap.add_argument("--paleta", action="store_true")
    a = ap.parse_args()
    if a.circulo:
        r, info = recortar_circulo(a.entrada)
        print("Círculo:", info)
    else:
        r = recortar(a.entrada, ampliar=a.ampliar, tol=a.tol, objeto=a.objeto, motas=a.motas, recorte=a.recorte)
    if a.maximo and max(r.size) > a.maximo:
        r.thumbnail((a.maximo, a.maximo), Image.LANCZOS)
    if a.paleta:
        r = r.quantize(colors=256, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.FLOYDSTEINBERG)
    r.save(a.salida, optimize=True)
    print(f"Listo: {a.salida} {r.size}")
