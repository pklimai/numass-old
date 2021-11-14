package inr.numass.viewer

import hep.dataforge.fx.dfIconView
import hep.dataforge.fx.meta.MetaViewer
import hep.dataforge.io.envelopes.Envelope
import hep.dataforge.meta.Meta
import hep.dataforge.meta.Metoid
import hep.dataforge.names.AlphanumComparator
import hep.dataforge.storage.Storage
import hep.dataforge.storage.files.FileStorage
import hep.dataforge.storage.files.FileTableLoader
import hep.dataforge.storage.tables.TableLoader
import inr.numass.data.NumassDataUtils
import inr.numass.data.NumassEnvelopeType
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDataLoader
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.scene.control.ContextMenu
import javafx.scene.control.TreeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tornadofx.*
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.WatchKey
import java.nio.file.WatchService


class StorageView : View(title = "Numass storage", icon = dfIconView) {

    val storageProperty = SimpleObjectProperty<Storage>()
    val storage by storageProperty

    private val pointCache by inject<PointCache>()

    private val ampView: AmplitudeView by inject()
    private val timeView: TimeView by inject()
    private val spectrumView: SpectrumView by inject()
    private val hvView: HVView by inject()
    private val scView: SlowControlView by inject()

    private var watcher: WatchService? = null


    fun clear() {
        watcher?.close()
        ampView.clear()
        timeView.clear()
        spectrumView.clear()
        hvView.clear()
        scView.clear()
    }

    private inner class Container(val id: String, val content: Any) {
        val checkedProperty = SimpleBooleanProperty(false)
        var checked by checkedProperty

        val infoView: UIComponent by lazy {
            when (content) {
                is NumassPoint -> PointInfoView(pointCache.getCachedPoint(id, content))
                is Metoid -> MetaViewer(content.meta, title = "Meta view: $id")
                else -> MetaViewer(Meta.empty(), title = "Meta view: $id")
            }
        }

        val watchedProperty = SimpleBooleanProperty(false)

        init {
            checkedProperty.onChange { selected ->
                when (content) {
                    is NumassPoint -> {
                        if (selected) {
                            ampView[id] = content
                            timeView[id] = content
                        } else {
                            ampView.remove(id)
                            timeView.remove(id)
                        }
                    }
                    is NumassSet -> {
                        if (selected) {
                            spectrumView[id] = content
                            hvView[id] = content
                        } else {
                            spectrumView.remove(id)
                            hvView.remove(id)
                        }
                    }
                    is TableLoader -> {
                        if (selected) {
                            scView[id] = content
                        } else {
                            scView.remove(id)
                        }
                    }
                }
            }

            watchedProperty.onChange {
                toggleWatch(it)
            }
        }

        val children: ObservableList<Container>? by lazy {
            when (content) {
                is Storage -> content.getChildren().map {
                    buildContainer(it, this)
                }.sortedWith(Comparator.comparing({ it.id }, AlphanumComparator)).asObservable()
                is NumassSet -> content.points
                    .sortedBy { it.index }
                    .map { buildContainer(it, this) }
                    .asObservable()
                else -> null
            }
        }

        val hasChildren: Boolean = (content is Storage) || (content is NumassSet)

        private var watchJob: Job? = null

        private fun toggleWatch(watch: Boolean) {
            if (watch) {
                if (watchJob != null && content is NumassDataLoader) {
                    watchJob = app.context.launch(Dispatchers.IO) {
                        val key: WatchKey = content.path.register(watcher!!, ENTRY_CREATE)
                        coroutineContext[Job]?.invokeOnCompletion {
                            key.cancel()
                        }
                        while (watcher != null && isActive) {
                            try {
                                key.pollEvents().forEach { event ->
                                    if (event.kind() == ENTRY_CREATE) {
                                        val path: Path = event.context() as Path
                                        if (path.fileName.toString().startsWith(NumassDataLoader.POINT_FRAGMENT_NAME)) {
                                            val envelope: Envelope = NumassEnvelopeType.infer(path)?.reader?.read(path)
                                                ?: kotlin.error("Can't read point file")
                                            val point = NumassDataUtils.read(envelope)
                                            children!!.add(buildContainer(point, this@Container))
                                        }
                                    }
                                }
                            } catch (x: Throwable) {
                                app.context.logger.error("Error during dynamic point read", x)
                            }
                        }
                    }
                }
            } else {
                watchJob?.cancel()
                watchJob = null
            }
        }
    }


    override val root = splitpane {
        treeview<Container> {
            //isShowRoot = false
            storageProperty.onChange { storage ->
                clear()
                if (storage == null) return@onChange
                root = TreeItem(Container(storage.name, storage))
                root.isExpanded = true
                lazyPopulate(leafCheck = {
                    !it.value.hasChildren
                }) {
                    it.value.children
                }
                watcher?.close()
                watcher = if (storage is FileStorage) {
                    storage.path.fileSystem.newWatchService()
                } else {
                    null
                }
            }

            cellFormat { value: Container ->
                when (value.content) {
                    is Storage -> {
                        text = value.content.name
                        graphic = null
                    }
                    is NumassSet -> {
                        text = null
                        graphic = checkbox(value.content.name).apply {
                            selectedProperty().bindBidirectional(value.checkedProperty)
                        }
                    }
                    is NumassPoint -> {
                        text = null
                        graphic = checkbox("${value.content.voltage}[${value.content.index}]").apply {
                            selectedProperty().bindBidirectional(value.checkedProperty)
                        }
                    }
                    is TableLoader -> {
                        text = null
                        graphic = checkbox(value.content.name).apply {
                            selectedProperty().bindBidirectional(value.checkedProperty)
                        }
                    }
                    else -> {
                        text = value.id
                        graphic = null
                    }
                }
                contextMenu = ContextMenu().apply {
                    item("Clear all") {
                        action {
                            this@cellFormat.treeItem.uncheckAll()
                        }
                    }
                    item("Info") {
                        action {
                            value.infoView.openModal(escapeClosesWindow = true)
                        }
                    }
                    if(value.content is NumassDataLoader) {
                        checkmenuitem("Watch") {
                            selectedProperty().bindBidirectional(value.watchedProperty)
                        }
                    }
                }
            }
        }

        tabpane {
            tab("Amplitude spectra") {
                content = ampView.root
                isClosable = false
                //visibleWhen(ampView.isEmpty.not())
            }
            tab("Time spectra") {
                content = timeView.root
                isClosable = false
                //visibleWhen(ampView.isEmpty.not())
            }
            tab("HV") {
                content = hvView.root
                isClosable = false
                //visibleWhen(hvView.isEmpty.not())
            }
            tab("Numass spectra") {
                content = spectrumView.root
                isClosable = false
                //visibleWhen(spectrumView.isEmpty.not())
            }
            tab("Slow control") {
                content = scView.root
                isClosable = false
                //visibleWhen(scView.isEmpty.not())
            }
        }
        setDividerPosition(0, 0.3);
    }


    private fun TreeItem<Container>.uncheckAll() {
        this.value.checked = false
        this.children.forEach { it.uncheckAll() }
    }


    private fun buildContainer(content: Any, parent: Container): Container =
        when (content) {
            is Storage -> Container(content.fullName.toString(), content)
            is NumassSet -> {
                val id: String = if (content is NumassDataLoader) {
                    content.fullName.unescaped
                } else {
                    content.name
                }
                Container(id, content)
            }
            is NumassPoint -> {
                Container("${parent.id}/${content.voltage}[${content.index}]", content)
            }
            is FileTableLoader -> Container(content.path.toString(), content)
            else -> throw IllegalArgumentException("Unknown content type: ${content::class.java}");
        }
}
