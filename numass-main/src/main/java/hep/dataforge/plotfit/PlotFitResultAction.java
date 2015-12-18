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
package hep.dataforge.plotfit;

import hep.dataforge.actions.OneToOneAction;
import hep.dataforge.meta.Meta;
import hep.dataforge.context.Context;
import hep.dataforge.data.DataPoint;
import hep.dataforge.data.DataSet;
import hep.dataforge.data.XYDataAdapter;
import hep.dataforge.datafitter.FitState;
import hep.dataforge.datafitter.models.XYModel;
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.description.TypedActionDef;
import hep.dataforge.exceptions.ContentException;
import hep.dataforge.io.log.Logable;
import hep.dataforge.plots.PlotFrame;
import hep.dataforge.plots.PlotsPlugin;
import hep.dataforge.plots.XYPlotFrame;
import hep.dataforge.plots.data.PlottableData;
import hep.dataforge.plots.data.PlottableFunction;
import hep.dataforge.plots.jfreechart.JFreeChartFrame;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 *
 * @author darksnake
 */
@TypedActionDef(name = "plotFit", description = "Plot fit result", inputType = FitState.class, outputType = FitState.class)
@NodeDef(name = "adapter", info = "adapter for DataSet being fitted. By default is taken from model.")
@ValueDef(name = "plotTitle", def = "", info = "The title of the plot.")
public class PlotFitResultAction extends OneToOneAction<FitState, FitState> {

    public PlotFitResultAction(Context context, Meta annotation) {
        super(context, annotation);
    }

    @Override
    protected FitState execute(Logable log, Meta metaData, FitState input){

        DataSet data = input.getDataSet();
        if(!(input.getModel() instanceof XYModel)){
            log.logError("The fit model should be instance of XYModel for this action. Action failed!");
            return input;
        }
        XYModel model = (XYModel)input.getModel();
        
        XYDataAdapter adapter;
        if (metaData.hasNode("adapter")){
            adapter = new XYDataAdapter(metaData.getNode("adapter"));
        } else if(input.getModel() instanceof XYModel){
            adapter = model.getAdapter();
        } else throw new ContentException("No adapter defined for data interpretation");
        
        UnivariateFunction function = (double x) -> model.getSpectrum().value(x, input.getParameters());
        
        XYPlotFrame frame = (XYPlotFrame) PlotsPlugin.buildFrom(getContext()).buildPlotFrame(getName(), input.getName(), metaData);
                //JFreeChartFrame.drawFrame(reader.getString("plotTitle", "Fit result plot for "+input.getName()), null);
        
        double[] x = new double[data.size()];
        
//        double[] y = new double[data.size()];
        
        
        double xMin = Double.POSITIVE_INFINITY;
        
        double xMax = Double.NEGATIVE_INFINITY;        
  
        List<DataPoint> points = new ArrayList<>();
        
        for (int i = 0; i < data.size(); i++) {
            x[i] = adapter.getX(data.get(i)).doubleValue();
//            y[i] = adapter.getY(data.get(i)); 

            points.add(adapter.mapToDefault(data.get(i)));
            if(x[i]<xMin){
                xMin = x[i];
            }
            
            if(x[i]>xMax){
                xMax = x[i];
            }            
        }
        
        frame.add(new PlottableFunction("fit", null, function, points, "x"));
        
        frame.add(new PlottableData("data", null, points));
        
        
        return input;
    }
    
}