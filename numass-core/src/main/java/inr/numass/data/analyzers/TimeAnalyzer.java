package inr.numass.data.analyzers;

import hep.dataforge.meta.Meta;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.values.Values;
import inr.numass.data.api.NumassBlock;
import inr.numass.data.api.NumassEvent;
import inr.numass.data.api.NumassPoint;
import inr.numass.data.api.SignalProcessor;
import javafx.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * An analyzer which uses time information from events
 * Created by darksnake on 11.07.2017.
 */
public class TimeAnalyzer extends AbstractAnalyzer {

    public TimeAnalyzer(@Nullable SignalProcessor processor) {
        super(processor);
    }

    public TimeAnalyzer() {
    }

    @Override
    public Values analyze(NumassBlock block, Meta config) {
        int loChannel = config.getInt("window.lo", 0);
        int upChannel = config.getInt("window.up", Integer.MAX_VALUE);
        long t0 = config.getValue("t0").longValue();

        AtomicLong totalN = new AtomicLong(0);
        AtomicLong totalT = new AtomicLong(0);

        extendedEventStream(block, config)
                .filter(pair -> {
                    short channel = pair.getKey().getChanel();
                    return channel >= loChannel && channel < upChannel;
                })
                .forEach(pair -> {
                    totalN.incrementAndGet();
                    //TODO add progress listener here
                    totalT.addAndGet(pair.getValue());
                });

        double countRate = 1e6 * totalN.get() / (totalT.get() / 1000 - t0 * totalN.get() / 1000);//1e9 / (totalT.get() / totalN.get() - t0);
        double countRateError = countRate / Math.sqrt(totalN.get());
        long count = (long) (totalT.get() * (countRate / 1e9));
        double length = totalT.get();

        if (block instanceof NumassPoint) {
            return ValueMap.of(NAME_LIST_WITH_HV,
                    ((NumassPoint) block).getVoltage(),
                    length,
                    count,
                    countRate,
                    countRateError,
                    new int[]{loChannel, upChannel},
                    block.getStartTime());
        } else {
            return ValueMap.of(NAME_LIST,
                    length,
                    count,
                    countRate,
                    countRateError,
                    new int[]{loChannel, upChannel},
                    block.getStartTime()
            );
        }
    }

    /**
     * The chain of event times in nanos
     *
     * @param block
     * @param config
     * @return
     */
    public Stream<Pair<NumassEvent, Long>> extendedEventStream(NumassBlock block, Meta config) {
        long t0 = config.getValue("t0").longValue();

        AtomicReference<NumassEvent> lastEvent = new AtomicReference<>(null);

        return super.getEventStream(block, config) //using super implementation
                .sorted()
                .map(event -> {
                    if (lastEvent.get() == null) {
                        lastEvent.set(event);
                        return new Pair<>(event, 0L);
                    } else {
                        long res = event.getTimeOffset() - lastEvent.get().getTimeOffset();
                        if (res >= 0) {
                            lastEvent.set(event);
                            return new Pair<>(event, res);
                        } else {
                            lastEvent.set(null);
                            return new Pair<>(event, 0L);
                        }
                    }
                }).filter(pair -> pair.getValue() >= t0);
    }

    @Override
    public Stream<NumassEvent> getEventStream(NumassBlock block, Meta config) {
        return extendedEventStream(block,config).map(Pair::getKey);
    }
}
