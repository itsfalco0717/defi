package com.tenisturnuva.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Enerjik/samimi bir his icin standart Material kosede kaviste (4dp) daha
// belirgin yuvarlatmalar kullaniyoruz — buyuk, net butonlar bu sekilde
// daha "dokunulabilir" gorunur.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
