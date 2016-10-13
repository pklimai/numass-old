/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile;

import hep.dataforge.context.Context;
import hep.dataforge.context.GlobalContext;
import hep.dataforge.description.NodeDef;
import hep.dataforge.description.ValueDef;
import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.maths.integration.UnivariateIntegrator;
import hep.dataforge.meta.Meta;
import hep.dataforge.stat.parametric.AbstractParametricBiFunction;
import hep.dataforge.stat.parametric.AbstractParametricFunction;
import hep.dataforge.stat.parametric.ParametricBiFunction;
import hep.dataforge.values.NamedValueSet;
import inr.numass.NumassIntegrator;
import inr.numass.models.FSS;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Compact all-in-one model for sterile neutrino spectrum
 *
 * @author Alexander Nozik
 */
@NodeDef(name = "resolution")
@NodeDef(name = "transmission")
@ValueDef(name = "fssFile", info = "The name for external FSS file. By default internal FSS file is used")
@ValueDef(name = "useFSS", type = "BOOLEAN")
public class SterileNeutrinoSpectrum extends AbstractParametricFunction {

    private static final String[] list = {"X", "trap", "E0", "mnu2", "msterile2", "U2"};
    /**
     * variables:Eo offset,Ein; parameters: "mnu2", "msterile2", "U2"
     */
    private final ParametricBiFunction source = new NumassBeta();
    /**
     * variables:Ein,Eout; parameters: "A"
     */
    private final ParametricBiFunction transmission;
    /**
     * variables:Eout,U; parameters: "X", "trap"
     */
    private final ParametricBiFunction resolution;
    /**
     * auxiliary function for trans-res convolution
     */
    private final ParametricBiFunction transRes;
    private FSS fss;
    //    private boolean useMC;
    private boolean fast;

    public SterileNeutrinoSpectrum(Context context, Meta configuration) {
        super(list);
        if (configuration.getBoolean("useFSS", true)) {
            InputStream fssStream;
            if (configuration.hasValue("fssFile")) {
                try {
                    fssStream = new FileInputStream(context.io().getFile(configuration.getString("fssFile")));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("Could not locate FSS file");
                }
            } else {
                fssStream = getClass().getResourceAsStream("/data/FS.txt");
            }
            fss = new FSS(fssStream);
        }

        transmission = new NumassTransmission(context, configuration.getNodeOrEmpty("transmission"));
        resolution = new NumassResolution(configuration.getMeta("resolution", Meta.empty()));
        this.fast = configuration.getBoolean("fast", true);
        transRes = new TransRes();
    }

    public SterileNeutrinoSpectrum(Meta configuration) {
        this(GlobalContext.instance(), configuration);
    }

    public SterileNeutrinoSpectrum() {
        this(GlobalContext.instance(), Meta.empty());
    }

    @Override
    public double derivValue(String parName, double u, NamedValueSet set) {
        switch (parName) {
            case "U2":
            case "msterile2":
            case "mnu2":
            case "E0":
                return integrate(u, source.derivative(parName), transRes, set);
            case "X":
            case "trap":
                return integrate(u, source, transRes.derivative(parName), set);
            default:
                throw new NotDefinedException();
        }
    }

    @Override
    public double value(double u, NamedValueSet set) {
        return integrate(u, source, transRes, set);
    }

    @Override
    public boolean providesDeriv(String name) {
        return source.providesDeriv(name) && transmission.providesDeriv(name) && resolution.providesDeriv(name);
    }


    /**
     * Direct Gauss-Legandre integration
     *
     * @param u
     * @param sourceFunction
     * @param transResFunction
     * @param set
     * @return
     */
    private double integrate(
            double u,
            ParametricBiFunction sourceFunction,
            ParametricBiFunction transResFunction,
            NamedValueSet set) {

        double eMax = set.getDouble("E0") + 5d;

        if (u > eMax) {
            return 0;
        }

        UnivariateFunction fsSource;
        if (fss != null) {
            fsSource = (eIn) -> {
                double res = 0;
                for (int i = 0; i < fss.size(); i++) {
                    res += fss.getP(i) * sourceFunction.value(fss.getE(i), eIn, set);
                }
                return res;
            };
        } else {
            fsSource = (eIn) -> sourceFunction.value(0, eIn, set);
        }

        UnivariateIntegrator integrator;
        if (fast && eMax - u < 500) {
            integrator = NumassIntegrator.getFastInterator();
        } else {
            integrator = NumassIntegrator.getDefaultIntegrator();
        }

        return integrator.integrate(eIn -> fsSource.value(eIn) * transResFunction.value(eIn, u, set), u, eMax);
    }

    private class TransRes extends AbstractParametricBiFunction {

        public TransRes() {
            super(new String[]{"X", "trap"});
        }

        @Override
        public boolean providesDeriv(String name) {
            return true;
        }

        @Override
        public double derivValue(String parName, double eIn, double u, NamedValueSet set) {
            switch (parName) {
                case "X":
                    //TODO implement p0 derivative
                    throw new NotDefinedException();
                case "trap":
                    return lossRes(transmission.derivative(parName), eIn, u, set);
                default:
                    return super.derivValue(parName, eIn, u, set);
            }
        }

        @Override
        public double value(double eIn, double u, NamedValueSet set) {

            double p0 = NumassTransmission.p0(eIn, set);
            return p0 * resolution.value(eIn, u, set) + lossRes(transmission, eIn, u, set);
        }

        private double lossRes(ParametricBiFunction transFunc, double eIn, double u, NamedValueSet set) {
            UnivariateFunction integrand = (eOut) -> transFunc.value(eIn, eOut, set) * resolution.value(eOut, u, set);

            double border = u + 30;
            double firstPart = NumassIntegrator.getFastInterator().integrate(integrand, u, Math.min(eIn, border));
            double secondPart;
            if (eIn > border) {
                if (fast) {
                    secondPart = NumassIntegrator.getFastInterator().integrate(integrand, border, eIn);
                } else {
                    secondPart = NumassIntegrator.getDefaultIntegrator().integrate(integrand, border, eIn);
                }
            } else {
                secondPart = 0;
            }
            return firstPart + secondPart;
        }

    }

}