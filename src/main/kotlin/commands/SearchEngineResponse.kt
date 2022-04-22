package commands

import search.SearchResult

sealed class SearchEngineResponse : Command

data class SearchEngineResponseSuccess(
    val requestId: Long,
    val searchResults: List<SearchResult>
) : SearchEngineResponse() {
    override fun toString(): String = searchResults.joinToString(separator = ",\n") { "$it" }
}

object SearchEngineResponseTimeout : SearchEngineResponse() {
    override fun toString(): String {
        return "TIMEOUT"
    }
}

