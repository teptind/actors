package actors

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import akka.actor.typed.ActorRef
import akka.actor.typed.javadsl.AskPattern
import commands.SearchEngineRequest
import commands.SearchEngineResponseSuccess
import commands.SearchEngineResponseTimeout
import commands.SearchRequest
import commands.SearchResponse
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.ClassRule
import org.junit.Test
import search.SearchResult
import search.StubSearchEngine

@ClassRule
val testKit = TestKitJunitResource()

class SearchEngineActorTest {

    @Test
    fun testReplySearchEngine() {
        val respondProbe = testKit.createTestProbe(SearchEngineResponseSuccess::class.java)

        val searchEngine = StubSearchEngine("test", Duration.ofMillis(0), TEST_RESOURCE.reader())
        val searchActor = testKit.spawn(SearchEngineActor.create(searchEngine))
        val request = SearchEngineRequest(
            0,
            "test",
            2,
            Duration.ofMillis(100),
            respondProbe.ref
        )
        searchActor.tell(request)
        val message = respondProbe.receiveMessage()
        assertEquals(0, message.requestId)

        assertEquals(
            listOf(
                SearchResult("content1", "link1"),
                SearchResult("content2", "link2")
            ),
            message.searchResults
        )
    }

    /**
     *  SearchEngineActor should send messages even with timeout on stubserver
     */
    @Test
    fun testReplyTimeout() {
        val respondProbe = testKit.createTestProbe(SearchEngineResponseSuccess::class.java)

        val searchEngine = StubSearchEngine("test", Duration.ofMillis(100), TEST_RESOURCE.reader())
        val searchActor = testKit.spawn(SearchEngineActor.create(searchEngine))
        val request = SearchEngineRequest(
            0,
            "test",
            2,
            Duration.ofMillis(0),
            respondProbe.ref
        )
        searchActor.tell(request)
        val message = respondProbe.receiveMessage()
        assertEquals(0, message.requestId)

        assertEquals(
            listOf(
                SearchResult("content1", "link1"),
                SearchResult("content2", "link2")
            ),
            message.searchResults
        )
    }


    @Test
    fun testSupervisorSingle() {
        val searchEngine = StubSearchEngine("test", Duration.ofMillis(100), TEST_RESOURCE.reader())
        val supervisorActor = testKit.spawn(SearchSupervisor.create(2, 500, listOf(searchEngine)))
        val reply = AskPattern.ask(
            supervisorActor,
            { replyTo: ActorRef<SearchResponse> -> SearchRequest("test", replyTo) },
            Duration.ofMillis(200),
            testKit.scheduler()
        ).toCompletableFuture().join()

        assertEquals(
            SearchEngineResponseSuccess(
                0,
                listOf(
                    SearchResult("content1", "link1"),
                    SearchResult("content2", "link2")
                )
            ),
            reply.engineResults["test"]
        )
    }

    @Test
    fun testSupervisorSingleTimeout() {
        val searchEngine = StubSearchEngine("test", Duration.ofMillis(100), TEST_RESOURCE.reader())
        val supervisorActor = testKit.spawn(SearchSupervisor.create(2, 50, listOf(searchEngine)))
        val reply = AskPattern.ask(
            supervisorActor,
            { replyTo: ActorRef<SearchResponse> -> SearchRequest("test", replyTo) },
            Duration.ofMillis(200),
            testKit.scheduler()
        ).toCompletableFuture().join()

        assertEquals(
            SearchEngineResponseTimeout,
            reply.engineResults["test"]
        )
    }

    @Test
    fun testSupervisorAggregate() {
        val searchEngines = listOf(
            StubSearchEngine("test", Duration.ofMillis(0), TEST_RESOURCE.reader()),
            StubSearchEngine("test2", Duration.ofMillis(100), TEST_RESOURCE.reader())
        )
        val supervisorActor = testKit.spawn(SearchSupervisor.create(2, 50, searchEngines))
        val reply = AskPattern.ask(
            supervisorActor,
            { replyTo: ActorRef<SearchResponse> -> SearchRequest("test", replyTo) },
            Duration.ofMillis(200),
            testKit.scheduler()
        ).toCompletableFuture().join()

        assertEquals(
            SearchEngineResponseTimeout,
            reply.engineResults["test2"]
        )
        assertEquals(
            SearchEngineResponseSuccess(
                0,
                listOf(
                    SearchResult("content1", "link1"),
                    SearchResult("content2", "link2")
                )
            ),
            reply.engineResults["test"]
        )
    }

}

const val TEST_RESOURCE = "{\n" +
        "  \"test\": [\n" +
        "    {\n" +
        "      \"content\": \"content1\",\n" +
        "      \"link\": \"link1\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"content\": \"content2\",\n" +
        "      \"link\": \"link2\"\n" +
        "    }],\n" +
        "  \"test2\": [\n" +
        "    {\n" +
        "      \"content\": \"!content1\",\n" +
        "      \"link\": \"!link1\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"content\": \"!content2\",\n" +
        "      \"link\": \"!link2\"\n" +
        "    }],\n" +
        "}"