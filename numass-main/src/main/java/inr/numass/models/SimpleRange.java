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

import hep.dataforge.values.NamedValueSet;

/**
 *
 * @author Darksnake
 */
public class SimpleRange implements SpectrumRange{
    Double min;
    Double max;

    public SimpleRange(Double min, Double max) {
        if(min>=max){
            throw new IllegalArgumentException();
        }
        this.min = min;
        this.max = max;
    }

    
    
    @Override
    public Double max(NamedValueSet set) {
        return max;
    }

    @Override
    public Double min(NamedValueSet set) {
        return min;
    }
    
    
    
}
