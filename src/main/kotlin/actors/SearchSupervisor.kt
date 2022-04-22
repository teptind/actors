package actors

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.TimerScheduler
import commands.Command
import commands.SearchEngineRequest
import commands.SearchEngineResponse
import commands.SearchEngineResponseSuccess
import commands.SearchEngineResponseTimeout
import commands.SearchRequest
import commands.SearchResponse
import commands.SearchTimeout
import search.SearchEngine
import java.time.Duration

class SearchSupervisor private constructor(
    context: ActorContext<Command>,
    searchEngines: List<SearchEngine>,
    private val timers: TimerScheduler<Command>,
    private val returnResultSize: Int,
    private val timeout: Duration
) : AbstractBehavior<Command>(context) {
    private val engineActors = searchEngines.associateWithTo(mutableMapOf()) {
        context.spawn(SearchEngineActor.create(it), it.name)
    }
    private val idToEngine = mutableMapOf<Long, SearchEngine>()
    private val waiting = mutableListOf<Long>()
    private val results = mutableMapOf<String, SearchEngineResponse>()
    lateinit var replyTo: ActorRef<SearchResponse>

    companion object {
        fun create(
            returnResultSize: Int = 10,
            timeoutMillis: Long = 10000L,
            engines: List<SearchEngine>
        ): Behavior<Command> =
            Behaviors.setup { context ->
                Behaviors.withTimers { timers ->
                    SearchSupervisor(context, engines, timers, returnResultSize, Duration.ofMillis(timeoutMillis))
                }
            }
    }

    override fun createReceive(): Receive<Command> =
        newReceiveBuilder()
            .onSignal(PostStop::class.java) { onPostStop() }
            .onMessage(SearchRequest::class.java) { onSearchRequest(it) }
            .onMessage(SearchTimeout::class.java) { onSearchTimeout() }
            .onMessage(SearchEngineResponseSuccess::class.java) { onSearchResponse(it) }
            .build()

    private fun onSearchResponse(searchResponse: SearchEngineResponseSuccess): Behavior<Command> {
        if (waiting.remove(searchResponse.requestId)) {
            idToEngine.remove(searchResponse.requestId)?.let { engine ->
                results[engine.name] = searchResponse
            }
        }
        return if (waiting.isEmpty()) {
            replyTo.tell(SearchResponse(results))
            Behaviors.stopped()
        } else {
            this
        }
    }

    private fun onSearchTimeout(): Behavior<Command> {
        waiting.forEach { id ->
            idToEngine.remove(id)?.let {
                results[it.name] = SearchEngineResponseTimeout
            }
        }
        waiting.clear()
        replyTo.tell(SearchResponse(results))

        return Behaviors.stopped()
    }

    private var counter = 0L

    private fun onSearchRequest(request: SearchRequest): Behavior<Command> {
        replyTo = request.replyTo
        engineActors.forEach { (engine, actor) ->
            idToEngine[counter] = engine
            waiting += counter
            val msg = SearchEngineRequest(counter++, request.searchQuery, returnResultSize, timeout, context.self)
            actor.tell(msg)
        }
        timers.startSingleTimer(SearchTimeout, timeout)

        return this
    }

    private fun onPostStop(): SearchSupervisor {
        context.log.info("Search Engine application stopped")

        return this
    }
}