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
package inr.numass.models;

import hep.dataforge.functions.AbstractParametricFunction;
import hep.dataforge.functions.ParametricFunction;
import hep.dataforge.maths.NamedDoubleSet;
import inr.numass.NumassContext;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 *
 * @author Darksnake
 */
class TransmissionConvolution extends AbstractParametricFunction {

    BivariateFunction trans;
    ParametricFunction spectrum;

    SpectrumRange range;

    TransmissionConvolution(ParametricFunction spectrum, BivariateFunction transmission, double endpoint) {
        this(spectrum, transmission, new SimpleRange(0d, endpoint));
    }

    TransmissionConvolution(ParametricFunction spectrum, BivariateFunction transmission, SpectrumRange range) {
        super(spectrum);
        this.trans = transmission;
        this.spectrum = spectrum;
        this.range = range;
    }

    @Override
    public double derivValue(final String parName, final double U, NamedDoubleSet set) {
        double min = range.min(set);
        double max = range.max(set);

        if (U >= max) {
            return 0;
        }
        UnivariateFunction integrand = (double E) -> {
            if (E <= U) {
                return 0;
            }
            return trans.value(E, U) * spectrum.derivValue(parName, E, set);
        };
        return NumassContext.defaultIntegrator.integrate(integrand, Math.max(U, min), max + 1d);
    }

    @Override
    public boolean providesDeriv(String name) {
        return spectrum.providesDeriv(name);
    }

    @Override
    public double value(final double U, NamedDoubleSet set) {
        double min = range.min(set);
        double max = range.max(set);

        if (U >= max) {
            return 0;
        }
        UnivariateFunction integrand = (double E) -> {
            if (E <= U) {
                return 0;
            }
            return trans.value(E, U) * spectrum.value(E, set);
        };
        return NumassContext.defaultIntegrator.integrate(integrand, Math.max(U, min), max + 1d);
    }
}