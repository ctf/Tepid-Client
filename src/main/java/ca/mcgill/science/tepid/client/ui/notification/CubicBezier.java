package ca.mcgill.science.tepid.client.ui.notification;

/**
 * Translated from JS: <a href="https://github.com/arian/cubic-bezier">Source</a>
 */
public class CubicBezier {

    private final double x1, x2, y1, y2, epsilon;

    private CubicBezier(double x1, double y1, double x2, double y2, double epsilon) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.epsilon = epsilon;
    }

    public double calc(double t) {
        double x2, d2, t2 = t;

        // First try a few iterations of Newton's method -- normally very fast.
        for (int i = 0; i < 8; i++) {
            x2 = curveX(t2) - t;
            if (Math.abs(x2) < epsilon) return curveY(t2);
            d2 = derivativeCurveX(t2);
            if (Math.abs(d2) < 1e-6) break;
            t2 = t2 - x2 / d2;
        }

        double t0 = 0, t1 = 1;
        t2 = t;

        if (t2 < t0) return curveY(t0);
        if (t2 > t1) return curveY(t1);

        // Fallback to the bisection method for reliability.
        while (t0 < t1) {
            x2 = curveX(t2);
            if (Math.abs(x2 - t) < epsilon) return curveY(t2);
            if (t > x2) t0 = t2;
            else t1 = t2;
            t2 = (t1 - t0) * .5 + t0;
        }

        // Failure
        return curveY(t2);
    }

    private double curveX(double t) {
        double v = 1 - t;
        return 3 * v * v * t * x1 + 3 * v * t * t * x2 + t * t * t;
    }

    private double curveY(double t) {
        double v = 1 - t;
        return 3 * v * v * t * y1 + 3 * v * t * t * y2 + t * t * t;
    }

    private double derivativeCurveX(double t) {
        double v = 1 - t;
        return 3 * (2 * (t - 1) * t + v * v) * x1 + 3 * (-t * t * t + 2 * v * t) * x2;
    }

    public static CubicBezier create(double x1, double y1, double x2, double y2, double epsilon) {
        return new CubicBezier(x1, y1, x2, y2, epsilon);
    }

}
