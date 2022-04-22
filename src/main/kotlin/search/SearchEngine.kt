package search

import java.time.Duration

abstract class SearchEngine(val name: String) {
    abstract fun query(searchQuery: String, returnResultSize: Int, timeout: Duration) : List<SearchResult>
}