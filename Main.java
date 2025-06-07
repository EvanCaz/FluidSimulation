import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.SwingUtilities;

public class Main extends JFrame {
    private final List<Particle> particles = new ArrayList<>();
    private final SimulationPanel panel;
    private ForkJoinPool pool; // will be initialized later
    private final ScheduledExecutorService simulationExecutor = Executors.newSingleThreadScheduledExecutor();

    public Main() {
        super("Simulation"); // set window title 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 1000); // set window size in pixels 

        // Initialize particles in a grid formation 
        for (int i = 0; i < 3000; i++) {                         
            double x = 100 + (i % 20) * 7;
            double y = 100 + (i / 20) * 5;
            particles.add(new Particle(x, y, 10.0)); // mass is constant 
        }
        
        panel = new SimulationPanel(particles);
        add(panel);
        setVisible(true);

        // configure forkjoin pool based on available processors
        int cores = Runtime.getRuntime().availableProcessors();
        pool = new ForkJoinPool(Math.max(2, cores * 3 / 4)); // leave some cores for ui/system
        
        // move simulation to background thread for responsive ui
        int delayMs = 4;
        simulationExecutor.scheduleAtFixedRate(() -> {
            stepSimulation(); // update physics 
            SwingUtilities.invokeLater(panel::repaint); // request redraw on edt
        }, 0, delayMs, TimeUnit.MILLISECONDS);
    }

    private void stepSimulation() {
        panel.attemptSpawn();
        panel.spatialHash(); // build grid for neighbor search 

        // parallel density computation 
        pool.invoke(new DensityTask(particles, 0, particles.size(), panel));

        panel.calcPressures(); // compute pressure from density 

        // Parallel force computation
        pool.invoke(new ForceTask(particles, 0, particles.size(), panel));

        panel.integrate(); // integrate velocity and position 
        panel.handleEdges(); // handle collisions with window edges 
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new); // start GUI on event thread 
    }

    
}



// recursive for computing density in parallel
class DensityTask extends RecursiveAction {
    private static final int THRESHOLD = 150; // max particles per task
    private final List<Particle> particles;
    private final int start, end;
    private final SimulationPanel panel;

    public DensityTask(List<Particle> particles, int start, int end, SimulationPanel panel) {
        this.particles = particles;
        this.start = start;
        this.end = end;
        this.panel = panel;
    }

    @Override
    protected void compute() {
        int length = end - start;
        if (length <= THRESHOLD) {
            for (int i = start; i < end; i++) {
                panel.computeDensityFor(particles.get(i)); // density for one particle logic)
            }
        } else {
            int mid = start + length / 2;
            // improved work-stealing balance
            DensityTask left = new DensityTask(particles, start, mid, panel);
            DensityTask right = new DensityTask(particles, mid, end, panel);
            left.fork();
            right.compute();
            left.join();
        }
    }
}

// RecursiveAction for computing forces in parallel
class ForceTask extends RecursiveAction {
    private static final int THRESHOLD = 100; // max particles per task (reduced for balance)
    private final List<Particle> particles;
    private final int start, end;
    private final SimulationPanel panel;

    public ForceTask(List<Particle> particles, int start, int end, SimulationPanel panel) {
        this.particles = particles;
        this.start = start;
        this.end = end;
        this.panel = panel;
    }

    @Override
    protected void compute() {
        int length = end - start;
        if (length <= THRESHOLD) {
            for (int i = start; i < end; i++) {
                panel.computeForcesFor(particles.get(i)); // force for one particle logic)
            }
        } else {
            int mid = start + length / 2;
            // improved work-stealing balance
            ForceTask left = new ForceTask(particles, start, mid, panel);
            ForceTask right = new ForceTask(particles, mid, end, panel);
            left.fork();
            right.compute();
            left.join();
        }
    }
}

 // Panel handling both rendering and simulation data structures
class SimulationPanel extends JPanel {
    private final List<Particle> particles; // all particles 
    private final double hz = 0.016; // timestep in seconds 
    private final double radius = 10.0; // interaction radius in pixels 
    private final double restDensity = .28; // rest density for SPH 
    private final double gasConstant = 7000.0; // pressure constant 
    private final double viscCoeff = 1000.0; // viscosity coefficient 
    private final double gravity = 9.81; // gravity acceleration 
    private final Map<CellKey, List<Particle>> grid = new HashMap<>(); // spatial hash grid 
    private final int drawRadius = 6; // radius to draw each particle
    
    private long   fpsLastTime   = System.currentTimeMillis();
    private int    fpsFrameCount = 0;
    private double fps           = 0.0;
    
    private volatile boolean leftDown = false, rightDown = false;
    private volatile Point mousePos = new Point();
    private long lastSpawn = 0;
    private final int SPAWN_INTERVAL_MS = 100;  // 100 particles a sec
    
    public SimulationPanel(List<Particle> particles) {
        this.particles = particles;
        setDoubleBuffered(true); // enable double-buffering for smoother rendering

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mousePos.setLocation(e.getX(), e.getY());
                if (SwingUtilities.isLeftMouseButton(e))  leftDown = true;
                else if (SwingUtilities.isRightMouseButton(e)) rightDown = true;
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e))  leftDown = false;
                else if (SwingUtilities.isRightMouseButton(e)) rightDown = false;
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePos.setLocation(e.getX(), e.getY());
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                mousePos.setLocation(e.getX(), e.getY());
            }
        });
    }

    public void attemptSpawn() { // if left is being clicked, simple cerate new point objects 
        if (!leftDown) return;
        long now = System.currentTimeMillis();
        if (now - lastSpawn < SPAWN_INTERVAL_MS) return;
        lastSpawn = now;
        for (int i = 0; i < 20; i++) {
            double angle = 2 * Math.PI * i / 10; // make a circle
            double x = mousePos.x + 20 * Math.cos(angle);
            double y = mousePos.y + 20 * Math.sin(angle);
            particles.add(new Particle(x, y, 10.0));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        fpsFrameCount++;
        long now = System.currentTimeMillis();
        long delta = now - fpsLastTime;
        if (delta >= 1000) {
            fps = fpsFrameCount * 1000.0 / delta;
            fpsFrameCount = 0;
            fpsLastTime = now;
    }
        
        String status = String.format("Actors: %d    FPS: %.1f", particles.size(), fps);
        FontMetrics fm = g.getFontMetrics();
        int   textWidth = fm.stringWidth(status);
        int   x = (getWidth() - textWidth) / 2;
        int   y = fm.getAscent() + 5;  // little padding from top
        g.setColor(Color.BLACK);
        g.drawString(status, x, y);

        for (Particle p : particles) {
            g.setColor(Color.BLUE);
    // draw each particle as a circle of diameter drawRadius*2
            g.fillOval((int)(p.x - drawRadius), (int)(p.y - drawRadius), 2 * drawRadius, 2 * drawRadius);
        }
    }

    

    // Build spatial hash: assign particles to cells 
    public void spatialHash() {
        // reuse existing lists to reduce allocations
        for (List<Particle> cell : grid.values()) {
            cell.clear();
        }
        
        for (Particle p : particles) {
            int ci = (int)Math.floor(p.x / radius);
            int cj = (int)Math.floor(p.y / radius);
            CellKey key = new CellKey(ci, cj);
            // reuse or create cell lists
            List<Particle> cell = grid.computeIfAbsent(key, k -> new ArrayList<>(50));
            cell.add(p);
        }
    }

    // Computes density for a single particle (extracted from calcDensity)
    public void computeDensityFor(Particle p) {
        p.density = 0;
        int ci = (int)(p.x / radius), cj = (int)(p.y / radius);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                List<Particle> neighbors = grid.get(new CellKey(ci + i, cj + j));
                if (neighbors != null) {
                    for (Particle pn : neighbors) {
                        double dx = p.x - pn.x, dy = p.y - pn.y;
                        double r = Math.sqrt(dx*dx + dy*dy);
                        if (r < radius) {
                            p.density += pn.mass * SPHKernels2D.poly6(r, radius);
                        }
                    }
                }
            }
        }
    }

    // Compute pressure from density for all particles 
    public void calcPressures() {
        for (Particle p : particles) {
            p.preassure = Math.max(0, gasConstant * (p.density - restDensity));

        }
    }

    // Computes forces for a single particle (extracted from calcForces)
    public void computeForcesFor(Particle p) {
        p.ax = 0; p.ay = 0;
        int ci = (int)(p.x / radius), cj = (int)(p.y / radius);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                List<Particle> neighbors = grid.get(new CellKey(ci + i, cj + j));
                if (neighbors != null) {
                    for (Particle pn : neighbors) {
                        if (pn == p) continue; // skip self 
                        double dx = p.x - pn.x, dy = p.y - pn.y;
                        double r = Math.sqrt(dx*dx + dy*dy);
                        if (r > 0 && r < radius) {
                            double gradW = SPHKernels2D.spikyGrad(r, radius);
                            double factor = -pn.mass * (p.preassure/(p.density*p.density) + pn.preassure/(pn.density*pn.density));
                            p.ax += factor * gradW * (dx / r);
                            p.ay += factor * gradW * (dy / r);
                            double lapW = SPHKernels2D.viscLaplacian(r, radius);
                            double dvx = pn.vx - p.vx, dvy = pn.vy - p.vy;
                            double visc = viscCoeff * pn.mass / pn.density;
                            p.ax += visc * lapW * dvx;
                            p.ay += visc * lapW * dvy;
                        }
                    }
                }
            }
        }
        p.ax = p.ax / p.density;
        p.ay = p.ay / p.density;
        if (rightDown) {
            double dxm = mousePos.x - p.x;
            double dym = mousePos.y - p.y;
            double rm = Math.hypot(dxm, dym);
            double attractRadius = 200.0;
            if (rm < attractRadius && rm > 1e-3) {
                // simple spring‐like pull: strength falls off with distance
                double pullStrength = 10.0 * (1.0 - rm/attractRadius);
                p.ax += pullStrength * (dxm / rm) / p.density;
                p.ay += pullStrength * (dym / rm) / p.density;
            }
        }
        p.ay += gravity; // add gravity 
    }

    // integrate positions and velocities 
    public void integrate() {
        // precompute time step factor for efficiency
        final double stepFactor = hz;
        // parallel integration step
        particles.parallelStream().forEach(p -> {
            p.vx += p.ax * stepFactor;
            p.vy += p.ay * stepFactor;
            p.x  += p.vx * stepFactor;
            p.y  += p.vy * stepFactor;
        });
    }

    // Handle collisions with window edges 
    public void handleEdges() {
        int width = getWidth(), height = getHeight();
        double restitution = 0.7;
        double pr = drawRadius; // use draw radius so drawing and physics match

        for (Particle p : particles) {
            if (p.x < pr) { p.x = pr; p.vx = -p.vx * restitution; }
            else if (p.x > width - pr) { p.x = width - pr; p.vx = -p.vx * restitution; }
            if (p.y < pr) { p.y = pr; p.vy = -p.vy * restitution; }
            else if (p.y > height - pr) { 
                double penetration = p.y - (height - pr);
                p.y -= penetration;
                p.vy = -p.vy * restitution;
            }
        }
    }

    // Key for spatial hash grid 
    static class CellKey {
        final int i, j;
        CellKey(int i, int j) { this.i = i; this.j = j; }
        @Override public boolean equals(Object o) { return (o instanceof CellKey) && ((CellKey)o).i == i && ((CellKey)o).j == j; }
        @Override public int hashCode() { return 31 * i + j; }
    }
}