package commands

data class SearchResponse(val engineResults: Map<String, SearchEngineResponse>) : Command