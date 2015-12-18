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
package inr.numass.actions;

import hep.dataforge.actions.ActionResult;
import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.meta.Meta;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataPoint;
import hep.dataforge.data.DataSet;
import hep.dataforge.data.ListDataSet;
import hep.dataforge.data.MapDataPoint;
import hep.dataforge.description.ValueDef;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.io.ColumnedDataWriter;
import hep.dataforge.io.log.Logable;
import hep.dataforge.values.Value;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author Darksnake
 */
@TypedActionDef(name = "monitor", inputType = DataSet.class, outputType = DataSet.class)
@ValueDef(name = "monitorPoint", type = "UMBER", required = true, info = "The Uset for monitor point")
@ValueDef(name = "monitorFile", info = "The outputfile for monitor points", def = "monitor.out")
@ValueDef(name = "calculateRelative", info = "Calculate count rate relative to average monitor point", def = "false")
public class MonitorCorrectAction extends OneToOneAction<DataSet, DataSet> {

    private static final String[] monitorNames = {"Timestamp", "Total", "CR", "CRerr"};

    CopyOnWriteArrayList<DataPoint> monitorPoints = new CopyOnWriteArrayList<>();

    public MonitorCorrectAction(Context context, Meta an) {
        super(context, an);
    }

    @Override
    protected DataSet execute(Logable log, Meta reader, DataSet sourceData) throws ContentException {

        double monitor = reader.getDouble("monitorPoint", Double.NaN);

        TreeMap<LocalDateTime, DataPoint> index = getMonitorIndex(monitor, sourceData);
        if (index.isEmpty()) {
            log.logError("No monitor points found");
            return sourceData;
        }
        double norm = 0;
        double totalAv = 0;
        String head = "";
        head += String.format("%20s\t%10s\t%s%n", "Timestamp", "Total", "CR in window");
        for (DataPoint dp : index.values()) {
            head += String.format("%20s\t%10d\t%g%n", getTime(dp).toString(), getTotal(dp), getCR(dp));
            norm += getCR(dp) / index.size();
            totalAv += getTotal(dp) / index.size();
            monitorPoints.add(dp);
        }

        head += String.format("%20s\t%10g\t%g%n", "Average", totalAv, norm);

        List<DataPoint> dataList = new ArrayList<>();

        for (DataPoint dp : sourceData) {
            MapDataPoint point = new MapDataPoint(dp);
            point.putValue("Monitor", 1.0);
            if (!isMonitorPoint(monitor, dp) || index.isEmpty()) {
                LocalDateTime time = getTime(dp);
                Entry<LocalDateTime, DataPoint> previousMonitor = index.floorEntry(time);
                Entry<LocalDateTime, DataPoint> nextMonitor = index.ceilingEntry(time);

                if (previousMonitor == null) {
                    previousMonitor = nextMonitor;
                }

                if (nextMonitor == null) {
                    nextMonitor = previousMonitor;
                }

                double p;
//                p = (getTime(dp).toEpochMilli() - previousMonitor.getKey().toEpochMilli())/
                p = 0.5;

                double corrFactor = (getCR(previousMonitor.getValue()) * p + getCR(nextMonitor.getValue()) * (1 - p)) / norm;
                double corrErr = previousMonitor.getValue().getValue("CRerr").doubleValue() / getCR(previousMonitor.getValue());
                double pointErr = dp.getValue("CRerr").doubleValue() / getCR(dp);
                double err = Math.sqrt(corrErr * corrErr + pointErr * pointErr) * getCR(dp);

                if (dp.names().contains("Monitor")) {
                    point.putValue("Monitor", Value.of(dp.getValue("Monitor").doubleValue() / corrFactor));
                } else {
                    point.putValue("Monitor", corrFactor);
                }
                point.putValue("CR", Value.of(dp.getValue("CR").doubleValue() / corrFactor));
                point.putValue("Window", Value.of(dp.getValue("Window").doubleValue() / corrFactor));
                point.putValue("Corrected", Value.of(dp.getValue("Corrected").doubleValue() / corrFactor));
                point.putValue("CRerr", Value.of(err));
            }
            if (meta().getBoolean("calculateRelative", false)) {
                point.putValue("relCR", point.getValue("CR").doubleValue() / norm);
                point.putValue("relCRerr", point.getValue("CRerr").doubleValue() / norm);
            }
            dataList.add(point);
        }

//        DataFormat format;
//
//        if (!dataList.isEmpty()) {
//            //Генерируем автоматический формат по первой строчке
//            format = DataFormat.of(dataList.get(0));
//        } else {
//            format = DataFormat.of(parnames);
//        }
        DataSet data = new ListDataSet(sourceData.getName(), sourceData.meta(), dataList);

        OutputStream stream = buildActionOutput(data);

        ColumnedDataWriter.writeDataSet(stream, data, head);

        return data;
    }

    @Override
    protected void afterAction(ActionResult<DataSet> pack) throws ContentException {
        printMonitorData();
        super.afterAction(pack);
    }

    private void printMonitorData() {
        String monitorFileName = meta().getString("monitorFile", "monitor");
        OutputStream stream = buildActionOutput(monitorFileName);
        ListDataSet data = new ListDataSet("monitor", null, monitorPoints);
        ColumnedDataWriter.writeDataSet(stream, data.sort("Timestamp", true), "Monitor points", monitorNames);
    }

    private boolean isMonitorPoint(double monitor, DataPoint point) {
        return point.getValue("Uset").doubleValue() == monitor;
    }

    private LocalDateTime getTime(DataPoint point) {
        return LocalDateTime.ofInstant(point.getValue("Timestamp").timeValue(), ZoneId.of("GMT+3"));
    }

    private int getTotal(DataPoint point) {
        return point.getValue("Total").intValue();
    }

    private double getCR(DataPoint point) {
        return point.getValue("CR").doubleValue();
    }

    private TreeMap<LocalDateTime, DataPoint> getMonitorIndex(double monitor, Iterable<DataPoint> data) {
        TreeMap<LocalDateTime, DataPoint> res = new TreeMap<>();
        for (DataPoint dp : data) {
            if (isMonitorPoint(monitor, dp)) {
                res.put(getTime(dp), dp);
            }
        }
        return res;
    }

}