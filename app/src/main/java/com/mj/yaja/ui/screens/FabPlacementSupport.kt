package com.mj.yaja.ui.screens

import androidx.compose.ui.Alignment
import com.mj.yaja.data.FabPlacement

internal fun FabPlacement.fabAlignment(): Alignment =
    when (this) {
        FabPlacement.LEFT -> Alignment.BottomStart
        FabPlacement.RIGHT -> Alignment.BottomEnd
    }

internal fun FabPlacement.fabHorizontalAlignment(): Alignment.Horizontal =
    when (this) {
        FabPlacement.LEFT -> Alignment.Start
        FabPlacement.RIGHT -> Alignment.End
    }
