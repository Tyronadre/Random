package tst;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

// Main application frame
public class Nurbs3DApp extends JFrame {
    public Nurbs3DApp() {
        setTitle("3D NURBS Modeler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        Nurbs3DPanel panel = new Nurbs3DPanel();
        add(panel);
        setLocationRelativeTo(null);
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Nurbs3DApp().setVisible(true));
    }
}

// The drawing panel: renders the 3D NURBS curve and its control polygon,
// projecting the 3D scene with a simple camera.
class Nurbs3DPanel extends JPanel {
    // 3D NURBS model (control points, degree, knot vector)
    private NurbsModel3D model;
    // A simple camera for 3D-to-2D projection
    private Camera camera;

    // Variables for mouse-based camera rotation
    private int lastMouseX, lastMouseY;
    private boolean rotating = false;

    public Nurbs3DPanel() {
        // Create a simple NURBS model – for example, a quadratic curve in 3D.
        model = new NurbsModel3D();
        // Create some sample control points (you can later add a GUI to modify these)
        model.controlPoints.add(new ControlPoint3D(1, 0, 0, 1));
        model.controlPoints.add(new ControlPoint3D(1, 1, 1, 1));
        model.controlPoints.add(new ControlPoint3D(0, 1, 0, 1));
        // Set degree and generate a uniform (clamped) knot vector.
        model.degree = 2;
        model.knots = model.generateUniformKnotVector(model.controlPoints.size(), model.degree);

        // Initialize the camera. We start at (0,0,5), looking at the origin.
        camera = new Camera(new Vector3(0, 0, 5), new Vector3(0, 0, 0), new Vector3(0, 1, 0),
                60, 1.0, 0.1, 100);

        // Mouse controls for camera orbit and zoom.
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                rotating = true;
            }
            public void mouseReleased(MouseEvent e) {
                rotating = false;
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (rotating) {
                    int dx = e.getX() - lastMouseX;
                    int dy = e.getY() - lastMouseY;
                    camera.orbit(dx, dy);
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    repaint();
                }
            }
        });
        addMouseWheelListener(e -> {
            camera.zoom(e.getWheelRotation());
            repaint();
        });
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Update camera aspect ratio
        camera.aspect = getWidth() / (double) getHeight();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the control polygon (red)
        g2.setColor(Color.RED);
        List<ControlPoint3D> cps = model.controlPoints;
        for (int i = 0; i < cps.size() - 1; i++) {
            Vector3 p1 = new Vector3(cps.get(i).x, cps.get(i).y, cps.get(i).z);
            Vector3 p2 = new Vector3(cps.get(i+1).x, cps.get(i+1).y, cps.get(i+1).z);
            Point pp1 = camera.project(p1, getWidth(), getHeight());
            Point pp2 = camera.project(p2, getWidth(), getHeight());
            g2.drawLine(pp1.x, pp1.y, pp2.x, pp2.y);
        }

        // Draw the NURBS curve (blue)
        g2.setColor(Color.BLUE);
        int numSamples = 100;
        Point prev = null;
        // The valid parameter range is from knots[degree] to knots[knots.length - degree - 1]
        double uStart = model.knots[model.degree];
        double uEnd = model.knots[model.knots.length - model.degree - 1];
        for (int i = 0; i <= numSamples; i++) {
            double u = uStart + (uEnd - uStart) * i / numSamples;
            ControlPoint3D pt = model.evaluateCurvePoint(u);
            Vector3 p = new Vector3(pt.x, pt.y, pt.z);
            Point proj = camera.project(p, getWidth(), getHeight());
            if (prev != null) {
                g2.drawLine(prev.x, prev.y, proj.x, proj.y);
            }
            prev = proj;
        }
    }
}

// 3D NURBS model: holds a list of control points, the degree, and knot vector,
// and includes evaluation of the curve via a de Boor–like algorithm.
class NurbsModel3D {
    public List<ControlPoint3D> controlPoints;
    public int degree;
    public double[] knots;

    public NurbsModel3D() {
        controlPoints = new ArrayList<>();
    }

    // Generates a uniform, clamped knot vector.
    public double[] generateUniformKnotVector(int numPoints, int degree) {
        int n = numPoints - 1;
        int m = n + degree + 1;
        double[] kv = new double[m+1];
        for (int i = 0; i <= m; i++) {
            if (i <= degree) {
                kv[i] = 0.0;
            } else if (i >= m - degree) {
                kv[i] = 1.0;
            } else {
                kv[i] = (double) (i - degree) / (m - 2 * degree);
            }
        }
        return kv;
    }

    // Evaluate the NURBS curve at parameter u.
    // This implementation uses de Boor's algorithm in homogeneous coordinates.
    public ControlPoint3D evaluateCurvePoint(double u) {
        int n = controlPoints.size() - 1;
        int p = degree;
        // Find index k so that knots[k] <= u < knots[k+1]
        int k = p;
        while (k < knots.length - 1 && !(knots[k] <= u && u < knots[k+1])) {
            k++;
        }
        if (u == knots[knots.length - p - 1]) {
            k = knots.length - p - 2;
        }
        // Copy control points into array d in homogeneous coordinates.
        ControlPoint3D[] d = new ControlPoint3D[n+1];
        for (int i = 0; i <= n; i++) {
            d[i] = controlPoints.get(i).copy();
        }
        // de Boor recursion
        for (int r = 1; r <= p; r++) {
            for (int i = k; i >= k - p + r; i--) {
                double alpha = (u - knots[i]) / (knots[i + p - r + 1] - knots[i]);
                // Interpolate in homogeneous coordinates
                double x = (1 - alpha) * d[i-1].x * d[i-1].weight + alpha * d[i].x * d[i].weight;
                double y = (1 - alpha) * d[i-1].y * d[i-1].weight + alpha * d[i].y * d[i].weight;
                double z = (1 - alpha) * d[i-1].z * d[i-1].weight + alpha * d[i].z * d[i].weight;
                double w = (1 - alpha) * d[i-1].weight + alpha * d[i].weight;
                d[i] = new ControlPoint3D(x / w, y / w, z / w, w);
            }
        }
        return d[k];
    }
}

// Represents a 3D control point with weight.
class ControlPoint3D {
    public double x, y, z, weight;
    public ControlPoint3D(double x, double y, double z, double weight) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.weight = weight;
    }
    public ControlPoint3D copy() {
        return new ControlPoint3D(x, y, z, weight);
    }
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f, w=%.2f)", x, y, z, weight);
    }
}

// Simple 3D vector class.
class Vector3 {
    public double x, y, z;
    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public Vector3 add(Vector3 v) {
        return new Vector3(x + v.x, y + v.y, z + v.z);
    }
    public Vector3 subtract(Vector3 v) {
        return new Vector3(x - v.x, y - v.y, z - v.z);
    }
    public Vector3 scale(double s) {
        return new Vector3(x * s, y * s, z * s);
    }
    public double dot(Vector3 v) {
        return x * v.x + y * v.y + z * v.z;
    }
    public Vector3 cross(Vector3 v) {
        return new Vector3(y * v.z - z * v.y,
                z * v.x - x * v.z,
                x * v.y - y * v.x);
    }
    public Vector3 normalize() {
        double mag = Math.sqrt(x * x + y * y + z * z);
        if (mag == 0) return new Vector3(0, 0, 0);
        return new Vector3(x / mag, y / mag, z / mag);
    }
}

// A simple 4x4 matrix class for transformations.
class Matrix4 {
    public double[] m; // 16 elements in column-major order.
    public Matrix4() {
        m = new double[16];
    }
    public static Matrix4 identity() {
        Matrix4 mat = new Matrix4();
        for (int i = 0; i < 16; i++) mat.m[i] = 0;
        mat.m[0] = mat.m[5] = mat.m[10] = mat.m[15] = 1;
        return mat;
    }
    public Matrix4 multiply(Matrix4 other) {
        Matrix4 result = new Matrix4();
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                result.m[col * 4 + row] = 0;
                for (int k = 0; k < 4; k++) {
                    result.m[col * 4 + row] += this.m[k * 4 + row] * other.m[col * 4 + k];
                }
            }
        }
        return result;
    }
    public Vector3 transform(Vector3 v) {
        double x = m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12];
        double y = m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13];
        double z = m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14];
        double w = m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15];
        if (w != 0) {
            x /= w; y /= w; z /= w;
        }
        return new Vector3(x, y, z);
    }
}

// A simple camera class with a look-at view and perspective projection.
class Camera {
    public Vector3 position, target, up;
    public double fov, aspect, near, far;
    // For orbiting control.
    private double azimuth = 0;      // horizontal angle
    private double elevation = 0;    // vertical angle
    private double distance;         // distance from target

    public Camera(Vector3 position, Vector3 target, Vector3 up,
                  double fov, double aspect, double near, double far) {
        this.position = position;
        this.target = target;
        this.up = up;
        this.fov = fov;
        this.aspect = aspect;
        this.near = near;
        this.far = far;
        this.distance = position.subtract(target).normalize().scale(1).dot(position.subtract(target));
        this.distance = Math.sqrt((position.subtract(target)).dot(position.subtract(target)));
    }

    // Orbit the camera around the target given mouse deltas.
    public void orbit(double dx, double dy) {
        azimuth += dx * 0.01;
        elevation += dy * 0.01;
        elevation = Math.max(-Math.PI/2 + 0.1, Math.min(Math.PI/2 - 0.1, elevation));
        double x = target.x + distance * Math.cos(elevation) * Math.sin(azimuth);
        double y = target.y + distance * Math.sin(elevation);
        double z = target.z + distance * Math.cos(elevation) * Math.cos(azimuth);
        position = new Vector3(x, y, z);
    }

    // Zoom in/out by modifying the distance.
    public void zoom(int wheelRotation) {
        distance *= (1 + wheelRotation * 0.1);
        double x = target.x + distance * Math.cos(elevation) * Math.sin(azimuth);
        double y = target.y + distance * Math.sin(elevation);
        double z = target.z + distance * Math.cos(elevation) * Math.cos(azimuth);
        position = new Vector3(x, y, z);
    }

    // Computes the view matrix using a lookAt transformation.
    public Matrix4 getViewMatrix() {
        Vector3 zaxis = position.subtract(target).normalize();
        Vector3 xaxis = up.cross(zaxis).normalize();
        Vector3 yaxis = zaxis.cross(xaxis);
        Matrix4 view = Matrix4.identity();
        view.m[0] = xaxis.x; view.m[4] = xaxis.y; view.m[8]  = xaxis.z;
        view.m[1] = yaxis.x; view.m[5] = yaxis.y; view.m[9]  = yaxis.z;
        view.m[2] = zaxis.x; view.m[6] = zaxis.y; view.m[10] = zaxis.z;
        view.m[12] = -xaxis.dot(position);
        view.m[13] = -yaxis.dot(position);
        view.m[14] = -zaxis.dot(position);
        return view;
    }

    // Computes the perspective projection matrix.
    public Matrix4 getProjectionMatrix() {
        Matrix4 proj = new Matrix4();
        double f = 1.0 / Math.tan(Math.toRadians(fov) / 2);
        proj.m[0] = f / aspect;
        proj.m[5] = f;
        proj.m[10] = (far + near) / (near - far);
        proj.m[11] = -1;
        proj.m[14] = (2 * far * near) / (near - far);
        proj.m[15] = 0;
        return proj;
    }

    // Projects a 3D point to 2D screen coordinates.
    public Point project(Vector3 point, int width, int height) {
        Matrix4 view = getViewMatrix();
        Matrix4 proj = getProjectionMatrix();
        Matrix4 vp = proj.multiply(view);
        Vector3 p = vp.transform(point);
        // Convert normalized device coordinates (-1 to 1) to screen coordinates.
        int sx = (int) ((p.x + 1) / 2 * width);
        int sy = (int) ((1 - (p.y + 1) / 2) * height);
        return new Point(sx, sy);
    }
}
