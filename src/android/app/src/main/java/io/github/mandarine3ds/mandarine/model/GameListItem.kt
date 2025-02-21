// Copyright 2025 Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.model

sealed class GameListItem {
    data class GameItem(val game: Game) : GameListItem()
    object Separator : GameListItem()
}