package commands

import akka.actor.typed.ActorRef
import java.time.Duration

data class SearchEngineRequest(
    val requestId: Long,
    val searchQuery: String,
    val returnResultSize: Int = 10,
    val timeout: Duration = Duration.ofMillis(10000),
    val replyTo: ActorRef<in SearchEngineResponseSuccess>
) : Command
