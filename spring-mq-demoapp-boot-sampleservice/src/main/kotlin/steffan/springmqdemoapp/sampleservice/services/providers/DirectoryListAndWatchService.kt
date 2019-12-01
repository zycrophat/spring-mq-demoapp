package steffan.springmqdemoapp.sampleservice.services.providers

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import io.methvin.watcher.hashing.FileHasher
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import steffan.springmqdemoapp.util.Logging
import java.nio.file.Files
import java.nio.file.Path
import javax.annotation.PostConstruct

@Component
class DirectoryListAndWatchService(
        @Value("\${steffan.springmqdemoapp.sampleservice.filewatch.dir-to-watch}")
        private val directoryToWatch: Path,

        @Autowired
        private val sender: WatchedFileSender

) : Logging {

    @PostConstruct
    fun start() {
        GlobalScope.launch {
            val channel = Channel<Path>()
            launch {
                val directoryWatcher = createDirectoryWatcher(channel)

                GlobalScope.launch(Dispatchers.IO) {
                    directoryWatcher.watch()
                }
            }

            listFiles(channel)
            processFiles(channel)
        }
    }

    private fun processFiles(channel: ReceiveChannel<Path>) {
        val numberOfWorkers = 25
        repeat(numberOfWorkers) {
            GlobalScope.launch {
                while (isActive) {
                    val path = channel.receive()
                    sender.send(path)
                    yield()
                }
            }
        }
    }

    private fun CoroutineScope.listFiles(channel: SendChannel<Path>) {
        launch(Dispatchers.IO) {
            val ctx = coroutineContext
            Files.list(directoryToWatch).use { stream ->
                stream.forEach {
                    launch(ctx) {
                        channel.send(it)
                    }
                }
            }
        }
    }

    private fun createDirectoryWatcher(channel: SendChannel<Path>): DirectoryWatcher {
        return DirectoryWatcher.builder()
                .path(directoryToWatch)
                .fileHasher(FileHasher.LAST_MODIFIED_TIME)
                .listener { event ->
                    runBlocking {
                        when (event.eventType()) {
                            DirectoryChangeEvent.EventType.CREATE -> {
                                val path = event.path()
                                when {
                                    Files.isRegularFile(path) -> channel.send(path)
                                }
                            }
                            else -> {
                                //NOP
                            }
                        }
                    }
                }
                .build()
    }

}