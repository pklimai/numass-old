package inr.numass.scripts.tristan

import inr.numass.data.ProtoNumassPoint
import inr.numass.data.api.MetaBlock
import inr.numass.data.api.NumassBlock
import inr.numass.data.api.NumassPoint
import inr.numass.data.plotAmplitudeSpectrum
import java.io.File


private fun NumassPoint.getChannels(): Map<Int, NumassBlock> {
    return blocks.toList().groupBy { it.channel }.mapValues { entry ->
        if (entry.value.size == 1) {
            entry.value.first()
        } else {
            MetaBlock(entry.value)
        }
    }
}

fun main(args: Array<String>) {
    val file = File("D:\\Work\\Numass\\data\\17kV\\processed.df").toPath()
    val point = ProtoNumassPoint.readFile(file)
    println(point.meta)
    point.getChannels().forEach{ num, block ->
        block.plotAmplitudeSpectrum(plotName = num.toString())
    }
}