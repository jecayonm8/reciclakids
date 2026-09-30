package com.reciclakids.ui.theme

import androidx.compose.ui.graphics.Color

// Color + forma por dígito del código del reto.
// DEBE ser idéntico en el teclado del niño (UI-08) y en la pantalla de código del docente (UI-24):
// el niño empareja figuras, no lee números.
enum class FormaDigito { Circulo, Cuadrado, Rombo, Gota, Ovalo }

data class EstiloDigito(val fondo: Color, val texto: Color, val forma: FormaDigito)

val EstilosDigito: Map<Char, EstiloDigito> = mapOf(
    '1' to EstiloDigito(Color(0xFFE2603C), Color.White, FormaDigito.Circulo),
    '2' to EstiloDigito(Color(0xFFE8A33D), Color(0xFF1A1C1E), FormaDigito.Cuadrado),
    '3' to EstiloDigito(Color(0xFF2E9E63), Color.White, FormaDigito.Rombo),
    '4' to EstiloDigito(Color(0xFF16A3B8), Color.White, FormaDigito.Gota),
    '5' to EstiloDigito(Color(0xFF2D6FD1), Color.White, FormaDigito.Ovalo),
    '6' to EstiloDigito(Color(0xFF7A57C9), Color.White, FormaDigito.Circulo),
    '7' to EstiloDigito(Color(0xFFD64C8E), Color.White, FormaDigito.Cuadrado),
    '8' to EstiloDigito(Color(0xFFA8724A), Color.White, FormaDigito.Rombo),
    '9' to EstiloDigito(Color(0xFFC9C93F), Color(0xFF1A1C1E), FormaDigito.Gota),
    '0' to EstiloDigito(Color(0xFF4B6269), Color.White, FormaDigito.Ovalo),
)
