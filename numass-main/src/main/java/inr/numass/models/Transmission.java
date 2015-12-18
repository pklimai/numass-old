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


import hep.dataforge.functions.ParametricFunction;
import hep.dataforge.maths.NamedDoubleSet;
import hep.dataforge.names.NamedSet;
import org.apache.commons.math3.analysis.BivariateFunction;

/**
 *
 * @author Darksnake
 */
public interface Transmission extends NamedSet{
    
    double getValue(NamedDoubleSet set, double input, double output);
    double getDeriv(String name, NamedDoubleSet set, double input, double output);
    boolean providesDeriv(String name);
    
    ParametricFunction getConvolutedSpectrum(RangedNamedSetSpectrum bare);
    
    
    default BivariateFunction getBivariateFunction(final NamedDoubleSet params){
        return (double input, double output) -> getValue(params, input, output);
    }
    
    default BivariateFunction getBivariateDerivFunction(final String name, final NamedDoubleSet params){
        return (double input, double output) -> getDeriv(name, params, input, output);
    }    
}