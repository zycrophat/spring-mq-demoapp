package steffan.springmqdemoapp.sampleservice.services.providers

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
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

@Component
class DirectoryListAndWatchService(
        @Value("\${steffan.springmqdemoapp.sampleservice.filewatch.dir-to-watch}")
        private val directoryToWatch: Path,

        @Autowired
        private val sender: WatchedFileSender

) : Logging {

    private val stateCheckMutex = Mutex()
    private var isWatching = false
    private var listingStatus = ListingStatus.INITIAL
    private val numberOfWorkers = 25
    private var processorSemaphore = Semaphore(numberOfWorkers)
    private val channel = Channel<Path>()
    private var optionalDirectoryWatcher: DirectoryWatcher? = null

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
                                        optionalDirectoryWatcher = directoryWatcher
                                        logger().info("Watching directory ${directoryToWatch}")

                                        isWatching = true
                                        directoryWatcher.watch()
                                    } catch (e: Exception) {
                                        logger().error("Exception while watching directory $directoryToWatch", e)
                                    } finally {
                                        isWatching = false
                                    }
                                }
                            } catch (e: Exception) {
                                logger().error("Exception while creating directory watcher for directory $directoryToWatch", e)
                            }
                        }
                    }

                    if (unwatchedFilesMayExist()) {
                        listFiles(channel)
                    }
                }
            }
        } else {
            logger().info("Already watching or listing. Skipping.")
        }

        if (processorSemaphore.availablePermits > 0) {
            processFiles(channel)
        }
    }

    private fun unwatchedFilesMayExist() =
            !isWatching || listingStatus == ListingStatus.INITIAL
                    || listingStatus == ListingStatus.LISTING_FAILED

    private fun processFiles(channel: ReceiveChannel<Path>) {
        repeat(processorSemaphore.availablePermits) {
            GlobalScope.launch {
                val semaphoreAcquired = processorSemaphore.tryAcquire()
                try {
                    while (isActive && semaphoreAcquired) {
                        val path = channel.receive()
                        sender.send(path)
                        yield()
                    }
                } finally {
                    if (semaphoreAcquired) {
                        processorSemaphore.release()
                    }
                }
            }
        }
    }

    private fun CoroutineScope.listFiles(channel: SendChannel<Path>) {
        launch(Dispatchers.IO) {
            val ctx = coroutineContext
            try {
                listingStatus = ListingStatus.IS_LISTING
                Files.list(directoryToWatch).use { stream ->
                    stream.forEach {
                        launch(ctx) {
                            channel.send(it)
                        }
                    }
                    listingStatus = ListingStatus.FINISHED_LISTING
                }
            } catch (e: Exception) {
                logger().error("Exception while listing files in directory $directoryToWatch", e)
                listingStatus = ListingStatus.LISTING_FAILED
            }
        }
    }

    private fun createDirectoryWatcher(channel: SendChannel<Path>): DirectoryWatcher {
        return DirectoryWatcher.builder()
                .path(directoryToWatch)
                .fileHashing(false)
                .listener { event ->
                    runBlocking {
                        when (event.eventType()) {
                            DirectoryChangeEvent.EventType.CREATE -> {
                                val path = event.path()
                                when {
                                    Files.isRegularFile(path) -> {
                                        channel.send(path)
                                    }
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
        optionalDirectoryWatcher?.close()
        channel.close()
    }

    private enum class ListingStatus {
        INITIAL,
        IS_LISTING,
        FINISHED_LISTING,
        LISTING_FAILED
    }

}