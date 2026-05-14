/*
 * Echo Music Project Original (2026)
 * Aditya (github.com/iad1tya)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package iad1tya.echo.music.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Menu(
    val menuRenderer: MenuRenderer,
) {
    @Serializable
    data class MenuRenderer(
        val items: List<Item>?,
        val topLevelButtons: List<TopLevelButton>?,
    ) {
        @Serializable
        data class Item(
            val menuNavigationItemRenderer: MenuNavigationItemRenderer?,
            val menuServiceItemRenderer: MenuServiceItemRenderer?,
            val toggleMenuServiceItemRenderer: ToggleMenuServiceRenderer?,
        ) {
            @Serializable
            data class MenuNavigationItemRenderer(
                val text: Runs,
                val icon: Icon,
                val navigationEndpoint: NavigationEndpoint,
            )

            @Serializable
            data class MenuServiceItemRenderer(
                val text: Runs,
                val icon: Icon,
                val serviceEndpoint: NavigationEndpoint,
            )
            @Serializable
            data class ToggleMenuServiceRenderer(
                val defaultIcon: Icon,
                val defaultServiceEndpoint: DefaultServiceEndpoint,
                val toggledServiceEndpoint: ToggledServiceEndpoint? = null,
                val isSelected: Boolean = false,
            )
        }

        @Serializable
        data class TopLevelButton(
            val buttonRenderer: ButtonRenderer?,
        ) {
            @Serializable
            data class ButtonRenderer(
                val icon: Icon,
                val navigationEndpoint: NavigationEndpoint,
            )
        }
    }
}
