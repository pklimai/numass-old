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
package inr.numass.scripts;

import hep.dataforge.context.GlobalContext;
import hep.dataforge.data.DataSet;
import hep.dataforge.tables.ListTable;
import hep.dataforge.datafitter.FitManager;
import hep.dataforge.datafitter.FitState;
import hep.dataforge.datafitter.ParamSet;
import hep.dataforge.datafitter.models.XYModel;
import hep.dataforge.likelihood.BayesianManager
import static hep.dataforge.maths.RandomUtils.setSeed;
import inr.numass.data.SpectrumGenerator;
import inr.numass.models.BetaSpectrum
import inr.numass.models.ModularSpectrum
import inr.numass.models.NBkgSpectrum;
import static inr.numass.utils.DataModelUtils.getUniformSpectrumConfiguration;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;


PrintWriter out = GlobalContext.out();
FitManager fm = new FitManager();

setSeed(543982);

//        TritiumSpectrum beta = new TritiumSpectrum(2e-4, 13995d, 18580d);
File fssfile = new File("c:\\Users\\Darksnake\\Dropbox\\PlayGround\\FS.txt");
ModularSpectrum beta = new ModularSpectrum(new BetaSpectrum(),8.3e-5, 14400d, 19010d);
beta.setCaching(false);
NBkgSpectrum spectrum = new NBkgSpectrum(beta);
XYModel model = new XYModel("tritium", spectrum);

ParamSet allPars = new ParamSet();

allPars.setParValue("N", 6e5);
//значение 6е-6 соответствует полной интенстивности 6е7 распадов в секунду
//Проблема была в переполнении счетчика событий в генераторе. Заменил на long. Возможно стоит поставить туда число с плавающей точкой
allPars.setParError("N", 25);
allPars.setParDomain("N", 0d, Double.POSITIVE_INFINITY);
allPars.setParValue("bkg", 5);
allPars.setParError("bkg", 1e-3);
allPars.setParValue("E0", 18575d);
allPars.setParError("E0", 0.1);
allPars.setParValue("mnu2", 0d);
allPars.setParError("mnu2", 1d);
allPars.setParValue("msterile2", 1000 * 1000);
allPars.setParValue("U2", 0);
allPars.setParError("U2", 1e-4);
allPars.setParDomain("U2", 0d, 1d);
allPars.setParValue("X", 0.0);
allPars.setParDomain("X", 0d, Double.POSITIVE_INFINITY);
allPars.setParValue("trap", 1d);
allPars.setParError("trap", 0.01d);
allPars.setParDomain("trap", 0d, Double.POSITIVE_INFINITY);

//        PlotManager pm = new PlotManager();
//        String plotTitle = "Tritium spectrum";
//        pm.plotFunction(FunctionUtils.getSpectrumFunction(spectrum, allPars), 14000, 18600, 500,plotTitle, null);
//        PrintNamed.printSpectrum(Out.out, beta.trapping, allPars, 14000d, 18600d, 500);
//        double e = 18570d;
//        trans.alpha = 1e-4;
//        trans.plotTransmission(System.out, allPars, e, e-1000d, e+100d, 200);
SpectrumGenerator generator = new SpectrumGenerator(model, allPars);

//        ColumnedDataFile file = new ColumnedDataFile("d:\\PlayGround\\RUN36.cfg");
//        ListTable config = file.getDataSet("time","X");
double Elow = 14000d;
double Eup = 18600d;
int numpoints = (int) ((Eup - Elow) / 50);
double time = 1e6 / numpoints; // 3600 / numpoints;
DataSet config = getUniformSpectrumConfiguration(Elow, Eup, time, numpoints);
//        config.addAll(DataModelUtils.getUniformSpectrumConfiguration(Eup, Elow, time, numpoints));// в обратную сторону

ListTable data = generator.generateData(config);
//        plotTitle = "Generated tritium spectrum data";
//        pm.plotXYScatter(data, "X", "Y",plotTitle, null);
//        bareBeta.setFSS("D:\\PlayGround\\FSS.dat");
//        data = tritiumUtils.applyDrift(data, 2.8e-6);

FitState state = fm.buildState(data, model, allPars);

//       fm.checkDerivs();
//        res.print(Out.out);        
//        fm.checkFitDerivatives();
FitState res = fm.runDefaultTask(state, "U2", "N", "trap");

res.print(out);

//        res = fm.runFrom(res);
//        res = fm.generateErrorsFrom(res);
beta.setCaching(true);
beta.setSuppressWarnings(true);

BayesianManager bm = new BayesianManager();
//        bm.setPriorProb(new OneSidedUniformPrior("trap", 0, true));
//        bm.setPriorProb(new GaussianPrior("trap", 1d, 0.002));
//        bm.printMarginalLikelihood(Out.out,"U2", res);

FitState conf = bm.getConfidenceInterval("U2", res, ["U2", "N", "trap"]);
//        plotTitle = String.format("Marginal likelihood for parameter \'%s\'", "U2");
//        pm.plotFunction(bm.getMarginalLikelihood("U2", res), 0, 2e-3, 40,plotTitle, null);

conf.print(out);
//        PrintNamed.printLogProbRandom(Out.out, res, 5000,0.5d, "E0","N");

spectrum.counter.print(out);

