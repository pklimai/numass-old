/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inr.numass.storage;

import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.MapPoint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Arrays.sort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Darksnake
 */
public class NMPoint {

    static final String[] dataNames = {"chanel", "count"};
    private Instant startTime;

//    private MonitorCorrector corrector = null;
//    private double deadTime;
    private long eventsCount;

    private int overflow;

    private double pointLength;
//    private final MeasurementPoint point;
    private final int[] spectrum;
    private double uread;
    private double uset;

    public NMPoint(RawNMPoint point) {
        if (point == null) {
            throw new IllegalArgumentException();
        }

        this.pointLength = point.getLength();
        this.uset = point.getUset();
        this.uread = point.getUread();
        this.startTime = point.getStartTime();
        this.eventsCount = point.getEventsCount();
//        this.point = point;
        spectrum = calculateSpectrum(point);
    }

//    public PointSpectrum(RawPoint point, double deadTime) {
//        this(point);
//        this.deadTime = deadTime;
//    }
    private int[] calculateSpectrum(RawNMPoint point) {
        assert point.getEventsCount() > 0;

        int[] result = new int[RawNMPoint.MAX_CHANEL];
        Arrays.fill(result, 0);
        point.getEvents().stream().forEach((event) -> {
            if (event.getChanel() >= RawNMPoint.MAX_CHANEL) {
                overflow++;
            } else {
                result[event.getChanel()]++;
            }
        });
        return result;
    }

    public DataPoint fastSlice(int... borders) {
        assert borders.length > 0;//FIXME replace by condition check
        sort(borders);
        assert borders[borders.length] < RawNMPoint.MAX_CHANEL;//FIXME replace by condition check

        Integer[] slices = new Integer[borders.length + 2];
        String[] names = new String[borders.length + 2];

        slices[0] = getCountInWindow(0, borders[0]);
        names[0] = Integer.toString(borders[0]);
        for (int i = 1; i < borders.length; i++) {
            slices[i] = getCountInWindow(borders[i - 1], borders[i]);
            names[i] = Integer.toString(borders[i]);
        }
        slices[borders.length + 1] = getCountInWindow(borders[borders.length], RawNMPoint.MAX_CHANEL);
        names[borders.length + 1] = Integer.toString(RawNMPoint.MAX_CHANEL);

        slices[borders.length + 2] = RawNMPoint.MAX_CHANEL;
        names[borders.length + 2] = "TOTAL";

        //FIXME fix it!
        return new MapPoint(names, slices);
    }

    /**
     * @return the absouteTime
     */
    public Instant getStartTime() {
        if (startTime == null) {
            return Instant.EPOCH;
        } else {
            return startTime;
        }
    }

    public int getCountInChanel(int chanel) {
        return spectrum[chanel];
    }

    public int getCountInWindow(int from, int to) {
        int res = 0;
        for (int i = from; i <= to; i++) {
            res += spectrum[i];
        }
        if (res == Integer.MAX_VALUE) {
            throw new RuntimeException("integer overflow in spectrum calculation");
        }
        return res;
    }

    
    //TODO move dead time out of here!
    public double getCountRate(int from, int to, double deadTime) {
        double wind = getCountInWindow(from, to) / getLength();
        double res;
        if (deadTime > 0) {
            double total = getEventsCount();
            double time = getLength();
            res = wind / (1 - total * deadTime / time);
        } else {
            res = wind;
        }
        return res;
    }

    public double getCountRateErr(int from, int to, double deadTime) {
        return Math.sqrt(getCountRate(from, to, deadTime) / getLength());
    }

    public List<DataPoint> getData() {
        List<DataPoint> data = new ArrayList<>();
        for (int i = 0; i < RawNMPoint.MAX_CHANEL; i++) {
            data.add(new MapPoint(dataNames, i, spectrum[i]));

        }
        return data;
    }

    /**
     * Events count - overflow
     *
     * @return
     */
    public long getEventsCount() {
        return eventsCount - getOverflow();
    }

    public List<DataPoint> getData(int binning, boolean normalize) {
        List<DataPoint> data = new ArrayList<>();

        double norm;
        if (normalize) {
            norm = getLength();
        } else {
            norm = 1d;
        }

        int i = 0;

        while (i < RawNMPoint.MAX_CHANEL - binning) {
            int start = i;
            double sum = spectrum[start] / norm;
            while (i < start + binning) {
                sum += spectrum[i] / norm;
                i++;
            }
            data.add(new MapPoint(dataNames, start + binning / 2d, sum));
        }
        return data;
    }

    public Map<Double, Double> getMapWithBinning(int binning, boolean normalize) {
        Map<Double, Double> res = new LinkedHashMap<>();

        double norm;
        if (normalize) {
            norm = getLength();
        } else {
            norm = 1d;
        }

        int i = 0;

        while (i < RawNMPoint.MAX_CHANEL - binning) {
            int start = i;
            double sum = spectrum[start] / norm;
            while (i < start + binning) {
                sum += spectrum[i] / norm;
                i++;
            }
            res.put(start + binning / 2d, sum);
        }
        return res;

    }

    public Map<Double, Double> getMapWithBinning(NMPoint reference, int binning) {
        Map<Double, Double> sp = this.getMapWithBinning(binning, true);
        Map<Double, Double> referenceSpectrum = reference.getMapWithBinning(binning, true);

        Map<Double, Double> res = new LinkedHashMap<>();

        sp.entrySet().stream().map((entry) -> entry.getKey()).forEach((bin) -> {
            res.put(bin, Math.max(sp.get(bin) - referenceSpectrum.get(bin), 0));
        });

        return res;

    }

    /**
     * @return the overflow
     */
    public int getOverflow() {
        return overflow;
    }

    /**
     * @return the pointLength
     */
    public double getLength() {
        return pointLength;
    }

    /**
     * @return the uread
     */
    public double getUread() {
        return uread;
    }

    /**
     * @return the uset
     */
    public double getUset() {
        return uset;
    }

}