package com.sports.redaccion.data

data class RssChannel(
    val id: String,
    val name: String,
    val url: String,
    val sport: String,
    var enabled: Boolean = true,
    val color: Int // Discord embed hexadecimal color (e.g. 0xE62429)
)

object RssSource {
    val defaultChannels = listOf(
        RssChannel(
            id = "marca_futbol",
            name = "Marca - Fútbol",
            url = "https://e00-marca.uecdn.es/rss/futbol.xml",
            sport = "Fútbol",
            color = 0xE62429 // Marca Red
        ),
        RssChannel(
            id = "as_futbol",
            name = "Diario AS - Fútbol",
            url = "https://as.com/rss/futbol/portada.xml",
            sport = "Fútbol",
            color = 0xFED100 // AS Yellow/Gold
        ),
        RssChannel(
            id = "mundodeportivo",
            name = "Mundo Deportivo - Fútbol",
            url = "https://www.mundodeportivo.com/feed/rss/futbol",
            sport = "Fútbol",
            color = 0x0078C2 // Mundo Deportivo Blue
        ),
        RssChannel(
            id = "sport",
            name = "Diario Sport - Fútbol",
            url = "https://www.sport.es/es/rss/futbol/",
            sport = "Fútbol",
            color = 0xED1C24 // Sport Orange/Red
        ),
        RssChannel(
            id = "espn_deportes",
            name = "ESPN Deportes - Fútbol",
            url = "https://www.espn.com.mx/espn/rss/futbol",
            sport = "Fútbol",
            color = 0xCC0000 // ESPN Red
        ),
        RssChannel(
            id = "tyc_sports",
            name = "TyC Sports",
            url = "https://www.tycsports.com/rss/rss.xml",
            sport = "Multideporte",
            color = 0x0093D1 // TyC Light Blue
        ),
        RssChannel(
            id = "mediotiempo",
            name = "MedioTiempo",
            url = "https://www.mediotiempo.com/rss",
            sport = "Fútbol",
            color = 0x222222 // MedioTiempo Dark Gray
        )
    )
}
