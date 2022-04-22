import actors.SearchSupervisor
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.AskPattern
import commands.SearchRequest
import commands.SearchResponse
import search.StubSearchEngine
import java.io.File
import java.time.Duration

const val RESOURCE_FILE = "src\\main\\resources\\stub_data.json"

fun main() {
    val engines = listOf(
        StubSearchEngine(
            "Google", Duration.ofMillis(1000),
            File(RESOURCE_FILE).reader()
        ),
        StubSearchEngine(
            "Yandex", Duration.ofMillis(2000),
            File(RESOURCE_FILE).reader()
        )
    )
    val timeout: Long = 1500
    val searchActor =
        ActorSystem.create(SearchSupervisor.create(timeoutMillis = timeout, engines = engines), "search-engine")
    println("PLEASE, TYPE YOUR QUERY:")
    val query = readLine()!!
    val reply = AskPattern.ask(
        searchActor,
        { replyTo: ActorRef<SearchResponse> -> SearchRequest(query, replyTo) },
        Duration.ofMillis(timeout + 100),
        searchActor.scheduler()
    ).toCompletableFuture().join()
    reply.engineResults.forEach { (name, response) ->
        println("$name:")
        println(response)
    }
}
