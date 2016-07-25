/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile;

import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.fitting.parametric.AbstractParametricBiFunction;
import hep.dataforge.values.NamedValueSet;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;

/**
 * A bi-function for beta-spectrum calculation taking energy and final state as input.
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class NumassBeta extends AbstractParametricBiFunction {

    private static final double K = 1E-23;
    private static final String[] list = {"E0", "mnu2", "msterile2", "U2"};

    public NumassBeta() {
        super(list);
    }

    private double derivRoot(int n, double E0, double mnu2, double E) throws NotDefinedException {
        double D = E0 - E;//E0-E
        double res;
        if (D == 0) {
            return 0;
        }

        if (mnu2 >= 0) {
            if (E >= (E0 - sqrt(mnu2))) {
                return 0;
            }
            double bare = sqrt(D * D - mnu2);
            switch (n) {
                case 0:
                    res = factor(E) * (2 * D * D - mnu2) / bare;
                    break;
                case 1:
                    res = -factor(E) * 0.5 * D / bare;
                    break;
                default:
                    return 0;
            }
        } else {
            double mu = sqrt(-0.66 * mnu2);
            if (E >= (E0 + mu)) {
                return 0;
            }
            double root = sqrt(Math.max(D * D - mnu2, 0));
            double exp = exp(-1 - D / mu);
            switch (n) {
                case 0:
                    res = factor(E) * (D * (D + mu * exp) / root + root * (1 - exp));
                    break;
                case 1:
                    res = factor(E) * (-(D + mu * exp) / root * 0.5 - root * exp * (1 + D / mu) / 3 / mu);
                    break;
                default:
                    return 0;
            }
        }

        return res;
    }

    /**
     * Derivative of spectrum with sterile neutrinos
     * @param name
     * @param E
     * @param E0
     * @param pars
     * @return
     * @throws NotDefinedException 
     */
    private double derivRootsterile(String name, double E, double E0, NamedValueSet pars) throws NotDefinedException {
        double mnu2 = getParameter("mnu2", pars);
        double mst2 = getParameter("msterile2", pars);
        double u2 = getParameter("U2", pars);

        switch (name) {
            case "E0":
                if (u2 == 0) {
                    return derivRoot(0, E0, mnu2, E);
                }
                return u2 * derivRoot(0, E0, mst2, E) + (1 - u2) * derivRoot(0, E0, mnu2, E);
            case "mnu2":
                return (1 - u2) * derivRoot(1, E0, mnu2, E);
            case "msterile2":
                if (u2 == 0) {
                    return 0;
                }
                return u2 * derivRoot(1, E0, mst2, E);
            case "U2":
                return root(E0, mst2, E) - root(E0, mnu2, E);
            default:
                return 0;
        }

    }

    private double factor(double E) {
        return K * pfactor(E);
    }

    private double pfactor(double E) {
        double me = 0.511006E6;
        double Etot = E + me;
        double pe = sqrt(E * (E + 2d * me));
        double ve = pe / Etot;
        double yfactor = 2d * 2d * 1d / 137.039 * Math.PI;
        double y = yfactor / ve;
        double Fn = y / abs(1d - exp(-y));
        double Fermi = Fn * (1.002037 - 0.001427 * ve);
        double res = Fermi * pe * Etot;
        return res;
    }

    @Override
    public boolean providesDeriv(String name) {
        return true;
    }

    /**
     * Bare beta spectrum with Mintz negative mass correction
     * @param E0
     * @param mnu2
     * @param E
     * @return 
     */
    private double root(double E0, double mnu2, double E) {
        /*чистый бета-спектр*/
        double delta = E0 - E;//E0-E
        double res;
        double bare = factor(E) * delta * sqrt(Math.max(delta * delta - mnu2, 0));
        if (delta == 0) {
            return 0;
        }
        if (mnu2 >= 0) {
            res = Math.max(bare, 0);
        } else {
            if (delta + 0.812 * sqrt(-mnu2) <= 0) {
                return 0;              //sqrt(0.66)
            }
            double aux = sqrt(-mnu2 * 0.66) / delta;
            res = Math.max(bare * (1 + aux * exp(-1 - 1 / aux)), 0);
        }
        return res;
    }

    /**
     * beta-spectrum with sterile neutrinos
     * @param E
     * @param E0
     * @param pars
     * @return 
     */
    private double rootsterile(double E, double E0, NamedValueSet pars) {
        double mnu2 = getParameter("mnu2", pars);
        double mst2 = getParameter("msterile2", pars);
        double u2 = getParameter("U2", pars);

        if (u2 == 0) {
            return root(E0, mnu2, E);
        }
        return u2 * root(E0, mst2, E) + (1 - u2) * root(E0, mnu2, E);
        // P(rootsterile)+ (1-P)root
    }

    @Override
    protected double getDefaultParameter(String name) {
        switch (name) {
            case "mnu2":
                return 0;
            case "U2":
                return 0;
            case "msterile2":
                return 0;
            default:
                return super.getDefaultParameter(name);
        }
    }

    @Override
    public double derivValue(String parName, double fs, double eIn, NamedValueSet pars) {
        double E0 = getParameter("E0", pars);
        return derivRootsterile(parName,eIn, E0 - fs, pars);
    }

    @Override
    public double value(double fs, double eIn, NamedValueSet pars) {
        double E0 = getParameter("E0", pars);
        return rootsterile(eIn, E0 - fs, pars);
    }

}
