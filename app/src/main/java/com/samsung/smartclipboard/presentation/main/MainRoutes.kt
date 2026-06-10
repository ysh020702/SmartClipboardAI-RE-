package com.samsung.smartclipboard.presentation.main

import com.samsung.smartclipboard.presentation.Screen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object MainRoutes {
    const val NavDataArg = "navData"

    val destinations: List<Destination> = Screen.entries.map { screen ->
        Destination(
            screen = screen,
            baseRoute = screen.routeBase,
            routePattern = "${screen.routeBase}?$NavDataArg={$NavDataArg}",
        )
    }

    fun routeFor(
        screen: Screen,
        data: Map<String, String>,
    ): String {
        val baseRoute = screen.routeBase
        if (data.isEmpty()) return baseRoute
        return "$baseRoute?$NavDataArg=${encodeNavData(data)}"
    }

    fun decodeNavData(encodedOrRaw: String?): Map<String, String> {
        if (encodedOrRaw.isNullOrBlank()) return emptyMap()

        val raw = decodeWholePayload(encodedOrRaw)
        if (raw.isBlank()) return emptyMap()

        return raw.split("&")
            .filter { it.isNotBlank() }
            .associate { pair ->
                val keyValue = pair.split("=", limit = 2)
                val key = decodeComponent(keyValue.getOrElse(0) { "" })
                val value = decodeComponent(keyValue.getOrElse(1) { "" })
                key to value
            }
    }

    private fun encodeNavData(data: Map<String, String>): String {
        val raw = data.entries.joinToString("&") { (key, value) ->
            "${encodeComponent(key)}=${encodeComponent(value)}"
        }
        return encodeComponent(raw)
    }

    private fun decodeWholePayload(value: String): String {
        val decoded = decodeComponent(value)
        return if ("=" in decoded || "&" in decoded) decoded else value
    }

    private fun encodeComponent(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }

    private fun decodeComponent(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
    }
}

internal data class Destination(
    val screen: Screen,
    val baseRoute: String,
    val routePattern: String,
)

private val Screen.routeBase: String
    get() = when (this) {
        Screen.Home -> "home"
        Screen.Data -> "data"
        Screen.Tasks -> "tasks"
        Screen.TopicDetail -> "topicDetail"
        Screen.ActionReview -> "actionReview"
        Screen.Storage -> "storage"
        Screen.History -> "history"
        Screen.AiSuggest -> "aiSuggest"
        Screen.Analyzing -> "analyzing"
        Screen.TopicDataSelect -> "topicDataSelect"
    }
