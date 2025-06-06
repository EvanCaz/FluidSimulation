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
        setSize(800, 600);
        SimulationPanel panel = new SimulationPanel();
        add(panel);                
        setVisible(true);

        int delayMs = 16; // 60 fps
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
        private final double restDensity = 1000.0;
        private final double gasConstant = 2000.0; // for preassure calculati0ons
        private final double viscCoeff = 250.0;
        private final double gravity = 9.81;
         private final Map<CellKey, List<Particle>> grid = new HashMap<>(); 

        public SimulationPanel(){
            for(int i = 0; i < 200; i++){ // change the total amount of particles
                double x = 100 + (i % 20) * 5;
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

        }

        public void spatialHash(){
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

        public void calcForces(){
            for(Particle p : particles){ // use all forces to find acceleration, so reset it to zero at the start
                p.ax = 0; p.ay = 0; // each particle

            }
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
