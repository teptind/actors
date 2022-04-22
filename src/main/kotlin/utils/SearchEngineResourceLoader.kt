package utils

import com.beust.klaxon.JsonArray
import com.beust.klaxon.Klaxon
import search.SearchResult
import java.io.Reader

class SearchEngineResourceLoader(private val resourceReader: Reader) {
    fun loadSearchResults() : Map<String, List<SearchResult>> {
        val json = Klaxon().parseJsonObject(resourceReader)
        return json.mapValues { (_, array) ->
            array as JsonArray<*>
            Klaxon().parseFromJsonArray(array)!!
        }
    }
}