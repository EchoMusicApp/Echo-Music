/*
 * Echo Music Project Original (2026)
 * Aditya (github.com/iad1tya)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package iad1tya.echo.music.innertube.pages

import iad1tya.echo.music.innertube.models.Menu
import iad1tya.echo.music.innertube.models.MusicResponsiveListItemRenderer.FlexColumn
import iad1tya.echo.music.innertube.models.Run

object PageHelper {
    private val LIBRARY_ADD_ICONS = setOf("LIBRARY_ADD", "BOOKMARK_BORDER")
    private val LIBRARY_SAVED_ICONS = setOf("LIBRARY_SAVED", "BOOKMARK", "LIBRARY_REMOVE")
    private val ALL_LIBRARY_ICONS = LIBRARY_ADD_ICONS + LIBRARY_SAVED_ICONS

    data class LibraryFeedbackTokens(
        val addToken: String?,
        val removeToken: String?
    )

    fun isLibraryIcon(iconType: String?): Boolean {
        if (iconType == null) return false
        if (iconType == "KEEP" || iconType == "KEEP_OFF") return false
        return iconType in ALL_LIBRARY_ICONS || iconType.startsWith("LIBRARY_")
    }

    fun isAddLibraryIcon(iconType: String?): Boolean {
        return iconType in LIBRARY_ADD_ICONS
    }

    fun isSavedLibraryIcon(iconType: String?): Boolean {
        return iconType in LIBRARY_SAVED_ICONS
    }

    fun extractRuns(columns: List<FlexColumn>, typeLike: String): List<Run> {
        val filteredRuns = mutableListOf<Run>()
        for (column in columns) {
            val runs = column.musicResponsiveListItemFlexColumnRenderer.text?.runs
                ?: continue

            for (run in runs) {
                val typeStr = run.navigationEndpoint?.watchEndpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                    ?: run.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
                    ?: continue

                if (typeLike in typeStr) {
                    filteredRuns.add(run)
                }
            }
        }
        return filteredRuns
    }

    fun extractLibraryTokensFromMenuItems(
        menuItems: List<Menu.MenuRenderer.Item>?
    ): LibraryFeedbackTokens {
        if (menuItems == null) return LibraryFeedbackTokens(null, null)

        var addToken: String? = null
        var removeToken: String? = null

        for (item in menuItems) {
            val toggleRenderer = item.toggleMenuServiceItemRenderer ?: continue
            val iconType = toggleRenderer.defaultIcon.iconType

            if (iconType == "KEEP" || iconType == "KEEP_OFF") continue

            if (!isLibraryIcon(iconType)) continue

            val defaultToken = toggleRenderer.defaultServiceEndpoint.feedbackEndpoint?.feedbackToken
            val toggledToken = toggleRenderer.toggledServiceEndpoint?.feedbackEndpoint?.feedbackToken

            when {
                isAddLibraryIcon(iconType) -> {
                    if (addToken == null) addToken = defaultToken
                    if (removeToken == null) removeToken = toggledToken
                }
                isSavedLibraryIcon(iconType) -> {
                    if (removeToken == null) removeToken = defaultToken
                    if (addToken == null) addToken = toggledToken
                }
            }
        }

        return LibraryFeedbackTokens(addToken, removeToken)
    }
}