package inr.numass.control

import ch.qos.logback.classic.Level
import hep.dataforge.control.connections.Roles
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.DeviceFactory
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta
import javafx.application.Platform
import javafx.scene.Scene
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import tornadofx.*
import java.util.*
import java.util.function.Predicate

/**
 * Created by darksnake on 14-May-17.
 */
abstract class NumassControlApplication<D : Device> : App() {
    private var device: D by singleAssign()

    override fun start(stage: Stage) {
        Locale.setDefault(Locale.US)// чтобы отделение десятичных знаков было точкой
        val rootLogger = LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        rootLogger.level = Level.INFO

        device = setupDevice()
        val controller = buildView(device)

        val scene = Scene(controller.pane)
        stage.scene = scene
        Platform.runLater { device.connect(controller, Roles.VIEW_ROLE, Roles.DEVICE_LISTENER_ROLE) }

        stage.show()


        setupStage(stage, device)
        setDFStageIcon(stage)
    }

    /**
     * Build a view connection

     * @return
     */
    protected abstract fun buildView(device: D): DeviceViewConnection<D>

    /**
     * Get a device factory for given device

     * @return
     */
    protected abstract val deviceFactory: DeviceFactory

    protected abstract fun setupStage(stage: Stage, device: D)

    protected abstract fun acceptDevice(meta: Meta): Boolean

    private fun setupDevice(): D {
        val config = getConfig(this)
                .orElseGet { readResourceMeta("/config/devices.xml") }

        val ctx = setupContext(config)
        val deviceConfig = findDeviceMeta(config, Predicate<Meta> { this.acceptDevice(it) })
                .orElseThrow { RuntimeException("Device configuration not found") }


        try {

            val d = deviceFactory.build(ctx, deviceConfig) as D
            d.init()
            connectStorage(d, config)

            return d
        } catch (e: ControlException) {
            throw RuntimeException("Failed to build device", e)
        }

    }

    override fun stop() {
        super.stop()
        device.shutdown()
        device.context.close()
    }


}
