package com.dinhhuy258.vintellij.comrade.buffer

import com.dinhhuy258.vintellij.comrade.ComradeNeovimPlugin
import com.dinhhuy258.vintellij.comrade.ComradeScope
import com.dinhhuy258.vintellij.comrade.core.ComradeBufEnterParams
import com.dinhhuy258.vintellij.comrade.core.ComradeBufWriteParams
import com.dinhhuy258.vintellij.comrade.core.MSG_COMRADE_BUF_ENTER
import com.dinhhuy258.vintellij.comrade.core.MSG_COMRADE_BUF_WRITE
import com.dinhhuy258.vintellij.comrade.core.NvimInstance
import com.dinhhuy258.vintellij.comrade.invokeOnMainAndWait
import com.dinhhuy258.vintellij.comrade.invokeOnMainLater
import com.dinhhuy258.vintellij.neovim.BufChangedtickEvent
import com.dinhhuy258.vintellij.neovim.BufDetachEvent
import com.dinhhuy258.vintellij.neovim.BufLinesEvent
import com.dinhhuy258.vintellij.neovim.Constants.Companion.MSG_NVIM_BUF_CHANGEDTICK_EVENT
import com.dinhhuy258.vintellij.neovim.Constants.Companion.MSG_NVIM_BUF_DETACH_EVENT
import com.dinhhuy258.vintellij.neovim.Constants.Companion.MSG_NVIM_BUF_LINES_EVENT
import com.dinhhuy258.vintellij.neovim.annotation.NotificationHandler
import com.dinhhuy258.vintellij.neovim.annotation.RequestHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.containers.ContainerUtil.newConcurrentSet
import com.intellij.util.messages.Topic
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.launch

class SyncBufferManager(private val nvimInstance: NvimInstance) : Disposable {
    companion object {
        val TOPIC = Topic<SyncBufferManagerListener>(
                "SyncBuffer related events", SyncBufferManagerListener::class.java)
        private val publisher = ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC)
        private val allBuffers = newConcurrentSet<SyncBuffer>()

        /**
         * Return all opened [SyncBuffer] globally.
         */
        fun listAllBuffers(): Set<SyncBuffer> {
            return allBuffers.toHashSet()
        }
    }

    private val log = Logger.getInstance(SyncBufferManager::class.java)
    // Although this is a ConcurrentHashMap, all create/delete SyncBuffer operations still have to be happened on the
    // UI thread.
    private val bufferMap = ConcurrentHashMap<Int, SyncBuffer>()
    private val client = nvimInstance.client

    init {
        Disposer.register(nvimInstance, this)
    }

    fun findBufferById(id: Int): SyncBuffer? {
        return bufferMap[id]
    }

    suspend fun loadCurrentBuffer() {
        val bufId = client.api.getCurrentBuf()
        val path = client.bufferApi.getName(bufId)

        loadBuffer(bufId, path)
    }

    fun loadBuffer(bufId: Int, path: String) {
        invokeOnMainLater {
            doLoadBuffer(bufId, path)
        }
    }

    private fun doLoadBuffer(bufId: Int, path: String) {
        var syncedBuffer = findBufferById(bufId)
        if (syncedBuffer == null) {
            try {
                syncedBuffer = SyncBuffer(bufId, path, nvimInstance)
            } catch (e: BufferNotInProjectException) {
                log.debug("'$path' is not a part of any opened projects.", e)
                return
            }
            bufferMap[bufId] = syncedBuffer
            allBuffers.add(syncedBuffer)
            val synchronizer = Synchronizer(syncedBuffer)
            synchronizer.exceptionHandler = {
                t ->
                log.warn("Error happened when synchronize buffers.", t)
                invokeOnMainAndWait { releaseBuffer(syncedBuffer) }
                ComradeScope.launch {
                    client.bufferApi.detach(syncedBuffer.id)
                    loadBuffer(syncedBuffer.id, syncedBuffer.path)
                }
            }
            syncedBuffer.attachSynchronizer(synchronizer)
        }
        if (ComradeNeovimPlugin.showEditorInSync) {
            syncedBuffer.navigate()
        }
        if (!syncedBuffer.isReleased) {
            publisher.bufferCreated(syncedBuffer)
        }
    }

    fun releaseBuffer(syncBuffer: SyncBuffer) {
        log.debug("releaseBuffer $syncBuffer")
        ApplicationManager.getApplication().assertIsDispatchThread()
        val bufferInMap = bufferMap.remove(syncBuffer.id) != null
        allBuffers.remove(syncBuffer)
        syncBuffer.release()
        if (bufferInMap) {
            publisher.bufferReleased(syncBuffer)
        }
    }

    override fun dispose() {
        val list = bufferMap.map { it.value }
        allBuffers.removeAll(bufferMap.values)
        bufferMap.clear()
        ApplicationManager.getApplication().invokeLater {
            list.forEach {
                releaseBuffer(it)
            }
        }
    }

    /**
     * Clean up all resources which are related to the given project.
     */
    fun cleanUp(project: Project) {
        val entriesToRemove = bufferMap
            .filter { it.value.project == project }
        ApplicationManager.getApplication().invokeAndWait {
            entriesToRemove.forEach {
                releaseBuffer(it.value)
            }
        }
    }

    @NotificationHandler(MSG_COMRADE_BUF_ENTER)
    fun comradeBufEnter(event: ComradeBufEnterParams) {
        loadBuffer(event.id, event.path)
    }

    @NotificationHandler(MSG_NVIM_BUF_LINES_EVENT)
    fun nvimBufLinesEvent(event: BufLinesEvent) {
        invokeOnMainLater {
            val buf = findBufferById(event.id) ?: return@invokeOnMainLater
            val change = BufferChange.NeovimChangeBuilder(buf, event).build()
            buf.synchronizer.onChange(change)
            // Always navigate to the editing file otherwise the code insight doesn't work.
            buf.navigate()
            publisher.bufferSynced(buf)
        }
    }

    @NotificationHandler(MSG_NVIM_BUF_CHANGEDTICK_EVENT)
    fun nvimBufChangedtickEvent(event: BufChangedtickEvent) {
        invokeOnMainLater {
            val buf = findBufferById(event.id) ?: return@invokeOnMainLater
            val change = BufferChange.NeovimChangeBuilder(buf, event).build()
            buf.synchronizer.onChange(change)
        }
    }

    @NotificationHandler(MSG_NVIM_BUF_DETACH_EVENT)
    fun nvimBufDetachEvent(event: BufDetachEvent) {
        invokeOnMainLater {
            val buf = findBufferById(event.id) ?: return@invokeOnMainLater
            releaseBuffer(buf)
        }
    }

    /**
     * Handle write request. eg.: When :w called.
     * This has to be a request instead of notification since we have to handle cases like fugitive :Gwrite call.
     */
    @RequestHandler(MSG_COMRADE_BUF_WRITE)
    fun comradeBufWrite(event: ComradeBufWriteParams): Boolean {
        invokeOnMainAndWait {
            val syncedBuffer = findBufferById(event.id)
                ?: throw IllegalStateException("Buffer ${event.id} has been detached from JetBrain.")
            FileDocumentManager.getInstance().saveDocument(syncedBuffer.document)
        }
        return true
    }
}
