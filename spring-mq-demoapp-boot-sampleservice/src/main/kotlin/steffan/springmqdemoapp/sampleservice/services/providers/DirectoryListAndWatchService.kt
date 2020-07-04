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
import java.util.concurrent.Executors

@Component
class DirectoryListAndWatchService(
        @Value("\${steffan.springmqdemoapp.sampleservice.filewatch.dir-to-watch}")
        private val directoryToWatch: Path,

        @Autowired
        private val sender: WatchedFileSender

) : Logging {

    private val stateCheckMutex = Mutex()
    private val numberOfWorkers = 48
    private val processorSemaphore = Semaphore(numberOfWorkers)
    private val channel = Channel<Path>()
    private var optionalDirectoryWatcher: DirectoryWatcher? = null
    private val coroutineDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()

    private var isWatching = false
    private var listingStatus = ListingStatus.INITIAL

    @Scheduled(initialDelay = 1000 * 5, fixedDelay = 1000 * 15)
    private fun watchAndListFiles() {
        if (!stateCheckMutex.isLocked) {
            GlobalScope.launch(coroutineDispatcher) {
                stateCheckMutex.withLock {
                    if (!isWatching) {
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
            GlobalScope.launch(coroutineDispatcher) {
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
                            logger().info("sent $it")
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
                    GlobalScope.launch(coroutineDispatcher) {
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
        coroutineDispatcher.close()
    }

    private enum class ListingStatus {
        INITIAL,
        IS_LISTING,
        FINISHED_LISTING,
        LISTING_FAILED
    }

}