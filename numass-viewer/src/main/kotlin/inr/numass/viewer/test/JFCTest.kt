package inr.numass.viewer.test

import hep.dataforge.plots.data.PlottableData
import hep.dataforge.plots.fx.PlotContainer
import hep.dataforge.plots.jfreechart.JFreeChartFrame
import hep.dataforge.tables.MapPoint
import hep.dataforge.tables.XYAdapter
import tornadofx.*
import java.util.*

/**
 * Created by darksnake on 16-Apr-17.
 */
class JFCTest : View("My View") {
    val rnd = Random();

    val plot = JFreeChartFrame();
    val container = PlotContainer();
    val data = PlottableData("data");

    val button = button("test") {
        action {

            data.fillData(
                    (1..1000).map { MapPoint(arrayOf(XYAdapter.X_VALUE_KEY, XYAdapter.Y_VALUE_KEY), it, rnd.nextDouble()) }
            )
            plot.add(data)
        }
    };

    override val root = borderpane {
        center {
            container.plot = plot
            add(container.root)
        }
        bottom {
            add(button)
        }
    }
}