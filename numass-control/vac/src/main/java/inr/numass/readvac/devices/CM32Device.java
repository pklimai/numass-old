/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.readvac.devices;

import hep.dataforge.context.Context;
import hep.dataforge.control.measurements.RegularMeasurement;
import hep.dataforge.control.measurements.SingleMeasurementDevice;
import hep.dataforge.control.ports.ComPortHandler;
import hep.dataforge.control.ports.PortHandler;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.ControlException;
import hep.dataforge.meta.Meta;
import hep.dataforge.values.Value;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 *
 * @author Alexander Nozik
 */
@ValueDef(name = "port")
@ValueDef(name = "delay")
@ValueDef(name = "timeout")
public class CM32Device extends SingleMeasurementDevice<RegularMeasurement<Double>> {

    private PortHandler handler;

    public CM32Device(String name, Context context, Meta meta) {
        super(name, context, meta);
    }

    /**
     * @return the handler
     */
    private PortHandler getHandler() throws ControlException {
        if (handler == null || !handler.isOpen()) {
            String port = meta().getString("port");
            getLogger().info("Connecting to port {}", port);
            handler = new ComPortHandler(port, 2400, 8, 1, 0);
            handler.setDelimeter("T--");
            handler.open();
        }
        return handler;
    }

    @Override
    protected RegularMeasurement<Double> createMeasurement() {
        return new CMVacMeasurement();
    }

    @Override
    public String type() {
        return meta().getString("type", "Leibold CM32");
    }

    @Override
    protected Object calculateState(String stateName) throws ControlException {
        if (getHandler() == null) {
            notifyError("No port connection", null);
            return null;
        }

        notifyError("State not found: " + stateName, null);
        return null;
        //TODO add connection check here
//        switch (stateName) {
//            case "connection":
//                return !talk("T?").isEmpty();
//            default:
//                notifyError("State not found: " + stateName, null);
//                return null;
//        }
    }

    public boolean isConnected() {
        return getState("connection").booleanValue();
    }

    private int timeout() {
        return meta().getInt("timeout", 400);
    }

    private class CMVacMeasurement extends RegularMeasurement<Double> {

        private static final String CM32_QUERY = "MES R PM 1\r\n";

        @Override
        protected Double doMeasurement() throws Exception {

            String answer = handler.sendAndWait(CM32_QUERY, timeout());

            if (answer.isEmpty()) {
                this.progressUpdate("No signal");
                updateState("connection", false);
                return null;
            } else if (answer.indexOf("PM1:mbar") < -1) {
                this.progressUpdate("Wrong answer: " + answer);
                updateState("connection", false);
                return null;
            } else if (answer.substring(14, 17).equals("OFF")) {
                this.progressUpdate("Off");
                updateState("connection", true);
                return null;
            } else {
                this.progressUpdate("OK");
                updateState("connection", true);
                return Double.parseDouble(answer.substring(14, 17) + answer.substring(19, 23));
            }
        }

        @Override
        protected Duration getDelay() {
            return Duration.of(meta().getInt("delay", 5000), ChronoUnit.MILLIS);
        }

    }

}
