package search

import utils.SearchEngineResourceLoader
import java.io.Reader
import java.lang.Integer.min
import java.time.Duration
import java.util.concurrent.TimeoutException

class StubSearchEngine(
    name: String,
    private val delay: Duration,
    resourceReader: Reader
) : SearchEngine(name) {
    private val searchResults = SearchEngineResourceLoader(resourceReader).loadSearchResults()

    override fun query(searchQuery: String, returnResultSize: Int, timeout: Duration): List<SearchResult> {
        if (delay >= timeout) {
            Thread.sleep(delay.toMillis())
        }
        val results = searchResults.getOrDefault(searchQuery, emptyList())
        return results.subList(0, min(returnResultSize, results.size))
    }
}