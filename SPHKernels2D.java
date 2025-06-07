 public class SPHKernels2D {
    private static final double PI = Math.PI;

    public static double poly6(double r, double h) {
        if (r < 0 || r > h) return 0;
        double coeff = 4.0 / (PI * Math.pow(h, 8));
        return coeff * Math.pow(h*h - r*r, 3);
    }

    public static double spikyGrad(double r, double h) {
        if (r <= 0 || r > h) return 0;
        double coeff = -45.0 / (PI * Math.pow(h, 6)); //exponent and constant error fixed here
        return coeff * (h - r) * (h - r); 
    }

    public static double viscLaplacian(double r, double h) {
        if (r < 0 || r > h) return 0;
        double coeff = 45.0 / (PI * Math.pow(h, 6)); //exponent and constant error fixed here
        return coeff * (h - r);
    }
}