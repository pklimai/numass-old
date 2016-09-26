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
package inr.numass.data;

import hep.dataforge.exceptions.DataFormatException;
import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.tables.DataPoint;
import hep.dataforge.tables.MapPoint;
import hep.dataforge.tables.PointAdapter;
import hep.dataforge.tables.XYAdapter;
import hep.dataforge.values.Value;

/**
 *
 * @author Darksnake
 */
public class SpectrumDataAdapter extends XYAdapter {

    private static final String POINT_LENGTH_NAME = "time";

    public SpectrumDataAdapter() {
    }

    public SpectrumDataAdapter(Meta meta) {
        super(meta);
    }

    public SpectrumDataAdapter(String xName, String yName, String yErrName, String measurementTime) {
        super(new MetaBuilder(PointAdapter.DATA_ADAPTER_KEY)
                .setValue(X_VALUE_KEY, xName)
                .setValue(Y_VALUE_KEY, yName)
                .setValue(Y_ERROR_KEY, yErrName)
                .setValue(POINT_LENGTH_NAME, measurementTime)
                .build()
        );
    }

    public SpectrumDataAdapter(String xName, String yName, String measurementTime) {
        super(new MetaBuilder(PointAdapter.DATA_ADAPTER_KEY)
                .setValue(X_VALUE_KEY, xName)
                .setValue(Y_VALUE_KEY, yName)
                .setValue(POINT_LENGTH_NAME, measurementTime)
                .build()
        );
    }

    public double getTime(DataPoint point) {
        return this.getFrom(point, POINT_LENGTH_NAME, 1d).doubleValue();
    }

    public DataPoint buildSpectrumDataPoint(double x, long count, double t) {
        return new MapPoint(new String[]{nameFor(X_VALUE_KEY), nameFor(Y_VALUE_KEY),
            nameFor(POINT_LENGTH_NAME)},
                x, count, t);
    }

    public DataPoint buildSpectrumDataPoint(double x, long count, double countErr, double t) {
        return new MapPoint(new String[]{nameFor(X_VALUE_KEY), nameFor(Y_VALUE_KEY),
            nameFor(Y_ERROR_KEY), nameFor(POINT_LENGTH_NAME)},
                x, count, countErr, t);
    }

    @Override
    public boolean providesYError(DataPoint point) {
        return true;
    }

    @Override
    public Value getYerr(DataPoint point) throws NameNotFoundException {
        if (super.providesYError(point)) {
            return Value.of(super.getYerr(point).doubleValue() / getTime(point));
        } else {
            double y = super.getY(point).doubleValue();
            if (y <= 0) {
                throw new DataFormatException();
            } else {
                return Value.of(Math.sqrt(y) / getTime(point));
            }
        }
    }

    public long getCount(DataPoint point) {
        return super.getY(point).numberValue().longValue();
    }

    @Override
    public Value getY(DataPoint point) {
        return Value.of(super.getY(point).doubleValue() / getTime(point));
    }

}
