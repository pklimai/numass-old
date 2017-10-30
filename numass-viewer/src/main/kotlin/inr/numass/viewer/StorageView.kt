package inr.numass.viewer

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.fx.fragments.FragmentWindow
import hep.dataforge.fx.fragments.LogFragment
import hep.dataforge.kodex.fx.dfIcon
import hep.dataforge.kodex.fx.runGoal
import hep.dataforge.kodex.fx.ui
import hep.dataforge.storage.api.Loader
import hep.dataforge.storage.api.Storage
import hep.dataforge.storage.filestorage.FileStorageFactory
import inr.numass.NumassProperties
import inr.numass.data.api.NumassPoint
import inr.numass.data.api.NumassSet
import inr.numass.data.storage.NumassDataLoader
import inr.numass.data.storage.NumassStorage
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.scene.control.ContextMenu
import javafx.scene.control.TreeItem
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.stage.DirectoryChooser
import org.controlsfx.control.StatusBar
import tornadofx.*
import java.io.File
import java.net.URI
import kotlin.streams.toList

class StorageView(private val context: Context = Global.instance()) : View(title = "Numass storage", icon = ImageView(dfIcon)) {


    val storageProperty = SimpleObjectProperty<Storage>()
    var storage by storageProperty


    private val storageNameProperty = SimpleStringProperty("")
    private var storageName by storageNameProperty

    private val statusBar = StatusBar();

    private val ampView: AmplitudeView by inject();
    private val spectrumView: SpectrumView by inject();
    private val hvView: HVView by inject();
    private val scView: SlowControlView by inject();

    private inner class Container(val id: String, val content: Any) {
        val checkedProperty = SimpleBooleanProperty(false)
        var checked by checkedProperty

        init {
            checkedProperty.onChange { selected ->
                when (content) {
                    is NumassPoint -> {
                        if (selected) {
                            ampView.add(id, content)
                        } else {
                            ampView.remove(id)
                        }
                    }
                    is NumassSet -> {
                        if (selected) {
                            spectrumView.add(id, content)
                            hvView.add(id, content)
                        } else {
                            spectrumView.remove(id)
                            hvView.remove(id)
                        }
                    }
                }
            }
        }

    }

    override val root = borderpane {
        top {
            toolbar {
                prefHeight = 40.0
                button("Load") {
                    action {
                        val chooser = DirectoryChooser()
                        chooser.title = "Select numass storage root"
                        val storageRoot = NumassProperties.getNumassProperty("numass.storage.root")
                        try {
                            if (storageRoot == null) {
                                chooser.initialDirectory = File(".").absoluteFile
                            } else {
                                chooser.initialDirectory = File(storageRoot)
                            }
                        } catch (ex: Exception) {
                            NumassProperties.setNumassProperty("numass.storage.root", null)
                        }

                        val rootDir = chooser.showDialog(primaryStage.scene.window)

                        if (rootDir != null) {
                            NumassProperties.setNumassProperty("numass.storage.root", rootDir.absolutePath)
                            loadDirectory(rootDir.toURI())
                        }
                    }
                }
                label(storageNameProperty) {
                    padding = Insets(0.0, 0.0, 0.0, 10.0);
                    font = Font.font("System Bold", 13.0);
                }
                pane {
                    hgrow = Priority.ALWAYS
                }
                togglebutton("Console") {
                    isSelected = false
                    FragmentWindow.build(this) {
                        LogFragment().apply {
                            addRootLogHandler()
                        }
                    }
                }
            }

        }
        center {
            splitpane {
                treeview<Container> {
                    storageProperty.onChange {
                        if (it != null) {
                            root = TreeItem(Container("root", it))
                            root.isExpanded = true
                            populate { parent ->
                                val value = parent.value.content
                                when (value) {
                                    is Storage -> (value.shelves() + value.loaders()).map { buildContainer(it, parent.value) }
                                    is NumassSet -> value.points.map { buildContainer(it, parent.value) }.toList()
                                    else -> null
                                }
                            }
                        }
                    }
                    cellFormat { value ->
                        contextMenu = null
                        when (value.content) {
                            is Storage -> text = value.id
                            is NumassSet -> {
                                text = null
                                graphic = checkbox(value.id, value.checkedProperty)
                                contextMenu = ContextMenu().apply {
                                    item("Info") {
                                        action {
                                            openInternalBuilderWindow(title = "Info: ${value.id}", escapeClosesWindow = true) {
                                                scrollpane {
                                                    textarea {
                                                        isEditable = false
                                                        isWrapText = true
                                                        text = value.content.meta.toString().replace("&#10;", "\n\t")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is NumassPoint -> {
                                text = null
                                graphic = checkbox(value.id, value.checkedProperty)
                            }
                            else -> {
                                text = (value as Loader).name
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
                }
                setDividerPosition(0, 0.3);
            }
        }


        bottom = statusBar;

    }

    private fun buildContainer(content: Any, parent: Container): Container {
        return when (content) {
            is Storage -> {
                Container(content.fullPath, content)
            }
            is NumassSet -> {
                val id = if (content is NumassDataLoader) {
                    content.path
                } else {
                    content.name
                }
                Container(id, content)
            }
            is NumassPoint -> {
                Container("${parent.id}/${content.voltage}".replace(".", "_"), content)
            }
            is Loader -> {
                Container(content.path, content);
            }
            else -> throw IllegalArgumentException("Unknown content type: ${content::class.java}");
        }

    }


//    private fun getSetName(value: NumassSet): String {
//        return if (value is NumassDataLoader) {
//            value.path
//        } else {
//            value.name
//        }
//    }

    private fun loadDirectory(path: URI) {
        runGoal("loadDirectory[$path]") {
            title = "Load storage ($path)"
            progress = -1.0
            message = "Building numass storage tree..."
            NumassStorage(context, FileStorageFactory.buildStorageMeta(path, true, true)).also {
                progress = 1.0
            }
        } ui {
            storage = it
            storageName = "Storage: " + path

        }
    }

//    fun setRootStorage(root: Storage) {
//        runGoal("loadStorage[${root.name}]") {
//            title = "Fill data to UI (" + root.name + ")"
//            progress = -1.0
//            runLater { statusBar.progress = -1.0 }
//
//            message = "Loading numass storage tree..."
//
//            runLater {
//                storage = root
//            }
//
//            //            callback.setProgress(1, 1);
//            runLater { statusBar.progress = 0.0 }
//            message = "Numass storage tree loaded."
//            progress = 1.0
//        }
//    }
}
