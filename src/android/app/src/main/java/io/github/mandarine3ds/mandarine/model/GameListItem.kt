package io.github.mandarine3ds.mandarine.model

sealed class GameListItem {
    data class GameItem(val game: Game) : GameListItem()
    object Separator : GameListItem()
}