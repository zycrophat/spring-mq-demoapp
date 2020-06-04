package steffan.springmqdemoapp.sampleservice.services.providers

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import io.methvin.watcher.hashing.FileHasher
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@Component
class DirectoryListAndWatchService(
        @Value("\${steffan.springmqdemoapp.sampleservice.filewatch.dir-to-watch}")
        private val directoryToWatch: Path,

        @Autowired
        private val sender: WatchedFileSender

) : Logging {


    private val stateCheckMutex = Mutex()
    private var isWatching = false
    private var isListing = false
    private var finishedListing = false
    private var watchingStoppedBeforeListing = false
    private val numberOfWorkers = 25
    private var processorSemaphore = Semaphore(numberOfWorkers)
    private val channel = Channel<Path>()
    private var optionalDirectoryWatcher = Optional.empty<DirectoryWatcher>()

    @Scheduled(initialDelay = 1000 * 5, fixedDelay = 1000 * 15)
    private fun watchAndListFiles() {
        if (!stateCheckMutex.isLocked) {
            GlobalScope.launch {
                stateCheckMutex.withLock {
                    if (!isWatching) {
                        launch {
                            try {
                                GlobalScope.launch(Dispatchers.IO) {
                                    try {
                                        val directoryWatcher = createDirectoryWatcher(channel)
                                        optionalDirectoryWatcher = Optional.of(directoryWatcher)
                                        logger().info("Watching directory ${directoryWatcher}")

                                        isWatching = true
                                        watchingStoppedBeforeListing = false
                                        directoryWatcher.watch()
                                    } catch (e: Exception) {
                                        logger().error("Exception while creating directory watcher for $directoryToWatch", e)
                                    } finally {
                                        isWatching = false
                                        if (!isListing && !finishedListing) {
                                            watchingStoppedBeforeListing = true
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                logger().error("Exception while creating directory watcher for directory $directoryToWatch", e)
                            }
                        }
                    }

                    if ((!isListing && !finishedListing) || watchingStoppedBeforeListing) {
                        isListing = true
                        listFiles(channel)
                    }

                    if (processorSemaphore.availablePermits > 0) {
                        processFiles(channel)
                    }

                }
            }
        } else {
            logger().info("Already watching or listing. Skipping.")
        }

    }

    private fun processFiles(channel: ReceiveChannel<Path>) {
        repeat(processorSemaphore.availablePermits) {
            GlobalScope.launch {
                try {
                    processorSemaphore.acquire()
                    while (isActive) {
                        val path = channel.receive()
                        sender.send(path)
                        yield()
                    }
                } finally {
                    processorSemaphore.release()
                }
            }
        }
    }

    private fun CoroutineScope.listFiles(channel: SendChannel<Path>) {
        launch(Dispatchers.IO) {
            val ctx = coroutineContext
            try {
                isListing = true
                Files.list(directoryToWatch).use { stream ->
                    stream.forEach {
                        launch(ctx) {
                            channel.send(it)
                        }
                    }
                    finishedListing = true
                }
            } catch (e: Exception) {
                logger().error("Exception while listing files in directory $directoryToWatch", e)
            }
            finally {
                isListing = false
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

    fun close() {
        optionalDirectoryWatcher.ifPresent { it.close() }
    }

}