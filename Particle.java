public class Particle{
    volatile public double x, y; // position 
    volatile public double vx, vy; // velocity
    public double ax, ay; // acceleartion
    public double density;
    public double preassure;
    public final double mass; // the same for all particles

    public Particle(double x, double y, double mass){
        this.mass = mass;
        this.x = x; this.y = y;
        this.vx = 0; this.vy = 0;
        this.ax = 0; this.ay  = 0;
        this.density = 0; this.preassure = 0; // calculated from neighbors in smothing radius in each time step 
    }
}