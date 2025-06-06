import javax.swing.*;
import javax.swing.border.Border;
import java.util.Set;
import java.util.stream.Collectors;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Main extends JFrame {
    public Main() {
        super("Simulation");                        // set window title
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        SimulationPanel panel = new SimulationPanel();
        add(panel);                
        setVisible(true);

        int delayMs = 8; // 60 fps
        new Timer(delayMs, e -> { // every 16ms this is called, redrawing the panel and calling the simulation
            panel.stepSimulation();
            panel.repaint();
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main());
    }

    static class SimulationPanel extends JPanel{
        private final List<Particle> particles = new ArrayList<>(); // list of allthe partcles
        private final double hz = 0.016;
        private final double radius = 16.0; // 16 pixel radius of interactivty
        private final double restDensity = 1.0;
        private final double gasConstant = 50.0; // for preassure calculati0ons
        private final double viscCoeff = 250.0;
        private final double gravity = 9.81;
         private final Map<CellKey, List<Particle>> grid = new HashMap<>(); 

        public SimulationPanel(){
            for(int i = 0; i < 2000; i++){ // change the total amount of particles
                double x = 100 + (i % 20) * 7;
                double y = 100 + (i / 20) * 5; // create them in a grid
                particles.add(new Particle(x, y, 1.0)); // mass is constant
            }
        }
        @Override
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            for(Particle p : particles){
                int r = 3;
                g.setColor(Color.BLUE);
                g.fillOval((int)(p.x - r), (int)(p.y - r), 2*r, 2*r);
            }    
        }
        
        public void stepSimulation(){ // make the hash, find densities, find preassure, find forces, integrate, handle the edges(reverse x y velocirtyt basically)
            spatialHash();
            calcDensity();
            calcPressures();
            calcForces();
            integrate();
            handleEdges();

        }

        public void spatialHash(){ // efficiency
            grid.clear();
            for (Particle p : particles){
                int ci = (int) Math.floor(p.x / radius);
                int cj = (int) Math.floor(p.y / radius);
                CellKey key = new CellKey(ci, cj);
                grid.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            }
        }

        public void calcPressures(){
            for (Particle p : particles){
                p.preassure = gasConstant * (p.density - restDensity);
            }
        }

        private void integrate() {
            for (Particle p : particles) {
                p.vx += p.ax * hz;
                p.vy += p.ay * hz;
                p.x  += p.vx * hz;
                p.y  += p.vy * hz;
            }
        }
        
        private void handleEdges() {
            int width  = getWidth();
            int height = getHeight();
            double restitution = 0.5;
            double pr = 3.0;  // particle draw‐radius

            for (Particle p : particles) {
                // --- horizontal walls ---
                if (p.x < pr) {
                    p.x  = pr;
                    p.vx = -p.vx * restitution;
                } else if (p.x > width - pr) {
                    p.x  = width - pr;
                    p.vx = -p.vx * restitution;
                }

                // --- vertical walls ---
                if (p.y < pr) {
                    p.y  = pr;
                    p.vy = -p.vy * restitution;
                } else if (p.y > height - pr) {
                    p.y  = height - pr;
                    p.vy = -p.vy * restitution;
                }
            }
        }


        public void calcDensity(){ // find density for every particlle at every tiomestepo
            for (Particle p :particles){
                p.density = 0; // calc from scratch
                int ci = (int)(p.x / radius), cj = (int)(p.y / radius);

                for(int i = -1; i <= 1; i++){
                    for(int j = -1; j <= 1; j++){ // loop over neighboring cells plus itself
                        int ni = ci + i, nj = cj + j; // neighbor i and neighbor j are the current cell plus the loops incrementer
                        // get the list of particles in the cell, if it is null, skip, other wise 
                        List<Particle> neighbor = grid.get(new CellKey(ni, nj)); // particles in this specifci neighbor outof 8 neighbors
                        if (neighbor != null){
                            for (Particle pn : neighbor){
                                double dx = p.x - pn.x, dy = p.y - pn.y; //distance
                                double r = Math.sqrt(dx*dx + dy*dy); // magnitutdwe
                                if(r < radius) {
                                    p.density += pn.mass * SPHKernels2D.poly6(r, radius); // density formula
                                }
                            }
                        }
                    }
                }
            }
        }

        public void calcForces(){
            for(Particle p : particles){ // use all forces to find acceleration, so reset it to zero at the start
                p.ax = 0; p.ay = 0; // each particle
                int ci = (int)(p.x / radius), cj = (int)(p.y / radius); // find the cell thje particle we are it is in
                
                 for(int i = -1; i <= 1; i++){
                    for(int j = -1; j <= 1; j++){ // all neighbors again, should be a function as this is repeated code, double forloop so this can be combined with aboce for efficiency
                        int ni = ci + i, nj = cj + j;
                        List<Particle> neighbor = grid.get(new CellKey(ni, nj));
                        if (neighbor != null){
                            for(Particle pn : neighbor){
                                if(pn == p) continue; // skip self
                                double dx = p.x - pn.x, dy = p.y - pn.y; // distance vectors
                                double r = Math.sqrt(dx*dx + dy*dy); // magnitude
                                if(r < radius && r > 0){
                                    double gradW = SPHKernels2D.spikyGrad(r, radius); // magnitutde
                                    double common = -pn.mass * (p.preassure + pn.preassure) / (2 * pn.density); // common factor
                                    
                                    p.ax += common * gradW * (dx / r);
                                    p.ay += common * gradW * (dy / r); // adding acc
                                
                                    double lapW = SPHKernels2D.viscLaplacian(r, radius);
                                    double dvx = pn.vx - p.vx, dvy = pn.vy - p.vy;
                                    double viscosityFactor = viscCoeff * pn.mass / pn.density;

                                    p.ax += viscosityFactor * lapW * dvx;
                                    p.ay += viscosityFactor * lapW * dvy; // adding acc
                                }
                            }
                        }
                    }
                }
                p.ax /= p.density;
                p.ay /= p.density;
                p.ay += gravity;
            }
        } 
        
        class CellKey {
            public final int i, j;
            public CellKey(int i, int j) { this.i = i; this.j = j; }
            @Override
            public boolean equals(Object o) {
                if (!(o instanceof CellKey)) return false;
                CellKey other = (CellKey) o;
                return this.i == other.i && this.j == other.j;
            }
            @Override
            public int hashCode() {
                return 31 * i + j;
            }
        }
    }
    

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
}