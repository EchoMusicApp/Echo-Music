




package iad1tya.echo.music.innertube.pages

import iad1tya.echo.music.innertube.models.*

data class ChartsPage(
    val sections: List<ChartSection>,
    val continuation: String?
) {
    data class ChartSection(
        val title: String,
        val items: List<YTItem>,
        val chartType: ChartType
    )

    enum class ChartType {
        TRENDING, TOP, GENRE, NEW_RELEASES
    }
}
