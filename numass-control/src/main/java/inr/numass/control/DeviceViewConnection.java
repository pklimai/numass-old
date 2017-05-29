package inr.numass.control;

import hep.dataforge.control.connections.DeviceConnection;
import hep.dataforge.control.devices.Device;
import hep.dataforge.control.devices.DeviceListener;
import hep.dataforge.fx.FXObject;
import hep.dataforge.values.Value;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by darksnake on 14-May-17.
 */
public abstract class DeviceViewConnection<D extends Device> extends DeviceConnection<D> implements DeviceListener, FXObject {
    private Map<String, ObjectBinding<Value>> bindings = new HashMap<>();

    /**
     * Get binding for a given device state
     *
     * @param state
     * @return
     */
    public ObjectBinding<Value> getStateBinding(String state) {
        return bindings.computeIfAbsent(state, stateName ->
                new ObjectBinding<Value>() {
                    @Override
                    protected Value computeValue() {
                        if(isOpen()) {
                            return getDevice().getState(stateName);
                        } else {
                            return Value.NULL;
                        }
                    }
                }
        );
    }

    /**
     * Bind existing boolean property to writable device state
     *
     * @param state
     * @param property
     */
    protected void bindBooleanToState(String state, BooleanProperty property) {
        getStateBinding(state).addListener((observable, oldValue, newValue) -> {
            if (isOpen() && oldValue != newValue) {
                property.setValue(newValue.booleanValue());
            }
        });
        property.addListener((observable, oldValue, newValue) -> {
            if (isOpen() && oldValue != newValue) {
                getDevice().setState(state, newValue);
            }
        });
    }

    @Override
    public void notifyDeviceStateChanged(Device device, String name, Value state) {
        if (bindings.containsKey(name)) {
            bindings.get(name).invalidate();
        }
    }

//    /**
//     * The small view for
//     * @return
//     */
//    public abstract Optional<Parent> getBoardView();
}
