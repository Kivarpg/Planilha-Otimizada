package com.example.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// APPROVED VISUAL CUSTOMIZATION
// DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
// Geometria global reconstruída: cantos recortados, mais arquitetônicos e menos
// próximos de Material arredondado. Não altera qualquer comportamento.
val Shapes = Shapes(
    extraSmall=CutCornerShape(3.dp),
    small=CutCornerShape(topStart = 8.dp, topEnd = 2.dp, bottomEnd = 8.dp, bottomStart = 2.dp),
    medium=CutCornerShape(topStart = 10.dp, topEnd = 3.dp, bottomEnd = 10.dp, bottomStart = 3.dp),
    large=CutCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomEnd = 14.dp, bottomStart = 4.dp),
    extraLarge=CutCornerShape(topStart = 18.dp, topEnd = 5.dp, bottomEnd = 18.dp, bottomStart = 5.dp)
)
