package commands

import akka.actor.typed.ActorRef

data class SearchRequest(val searchQuery: String, val replyTo: ActorRef<SearchResponse>) : Command