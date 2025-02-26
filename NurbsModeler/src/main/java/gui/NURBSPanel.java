package gui;

import data.ControlPoint;
import data.NURBSModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

// Panel, das die NURBS-Kurve zeichnet, mit Mausinteraktionen (Panning und Drag & Drop).
public class NURBSPanel extends JPanel {
    private NURBSModel model;
    // Panning-Parameter
    private int panOffsetX = 0, panOffsetY = 0;
    // Für Mausinteraktionen
    private int lastMouseX, lastMouseY;
    private int selectedPointIndex = -1;

    public NURBSPanel(NURBSModel model) {
        this.model = model;
        setBackground(Color.WHITE);
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point click = e.getPoint();
                java.util.List<ControlPoint> cps = model.getControlPoints();
                selectedPointIndex = -1;
                // Prüfe, ob ein Kontrollpunkt in der Nähe (innerhalb 8 Pixel) angeklickt wurde.
                for (int i = 0; i < cps.size(); i++) {
                    Point p = transform(cps.get(i));
                    if (click.distance(p) < 8) {
                        selectedPointIndex = i;
                        break;
                    }
                }
                // Wenn kein Punkt getroffen, starte Panning.
                if (selectedPointIndex == -1) {
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedPointIndex != -1) {
                    // Verschiebe den ausgewählten Kontrollpunkt.
                    Point mousePoint = e.getPoint();
                    double scale = 200.0;
                    int baseOffsetX = 200;
                    int baseOffsetY = 400;
                    double newX = (mousePoint.x - baseOffsetX - panOffsetX) / scale;
                    double newY = (baseOffsetY + panOffsetY - mousePoint.y) / scale;
                    model.getControlPoints().get(selectedPointIndex).x = newX;
                    model.getControlPoints().get(selectedPointIndex).y = newY;
                } else {
                    // Panning: Aktualisiere die Verschiebung.
                    int dx = e.getX() - lastMouseX;
                    int dy = e.getY() - lastMouseY;
                    panOffsetX += dx;
                    panOffsetY += dy;
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                selectedPointIndex = -1;
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        java.util.List<ControlPoint> cps = model.getControlPoints();

        // Zeichne das Kontrollpolygon (Grau)
        g2.setColor(Color.GRAY);
        for (int i = 0; i < cps.size() - 1; i++) {
            Point p1 = transform(cps.get(i));
            Point p2 = transform(cps.get(i + 1));
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // Zeichne die Kontrollpunkte (Rot)
        g2.setColor(Color.RED);
        for (ControlPoint cp : cps) {
            Point p = transform(cp);
            g2.fillOval(p.x - 4, p.y - 4, 8, 8);
        }

        // Berechne und zeichne die NURBS-Kurve (Blau)
        g2.setColor(Color.BLUE);
        java.util.List<Point> curvePoints = evaluateCurvePoints(200);
        for (int i = 0; i < curvePoints.size() - 1; i++) {
            Point p1 = curvePoints.get(i);
            Point p2 = curvePoints.get(i + 1);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    // Transformation von Modellkoordinaten in Bildschirmkoordinaten (unter Berücksichtigung von Panning).
    private Point transform(ControlPoint cp) {
        int scale = 200;
        int baseOffsetX = 200;
        int baseOffsetY = 400; // Y-Achse invertiert
        int x = baseOffsetX + panOffsetX + (int) (cp.x * scale);
        int y = baseOffsetY + panOffsetY - (int) (cp.y * scale);
        return new Point(x, y);
    }

    // Berechnet eine Liste von Punkten der NURBS-Kurve.
    private java.util.List<Point> evaluateCurvePoints(int numPoints) {
        java.util.List<Point> pts = new ArrayList<>();
        double[] U = model.getKnots();
        int p = model.getDegree();
        java.util.List<ControlPoint> cps = model.getControlPoints();
        // Falls der Knotenvector nicht zur aktuellen Konfiguration passt, neu generieren.
        if (U.length != cps.size() + p + 1) {
            U = model.generateUniformKnotVector(cps.size(), p);
            model.setKnots(U);
        }
        double uStart = U[p];
        double uEnd = U[U.length - p - 1];
        for (int i = 0; i <= numPoints; i++) {
            double u = uStart + (uEnd - uStart) * i / numPoints;
            ControlPoint cp = evaluateCurvePoint(u, U, p, cps);
            pts.add(transform(cp));
        }
        return pts;
    }

    // Berechnet einen Punkt der NURBS-Kurve bei Parameter u.
    private ControlPoint evaluateCurvePoint(double u, double[] U, int p, List<ControlPoint> cps) {
        double numeratorX = 0.0, numeratorY = 0.0, denominator = 0.0;
        for (int i = 0; i < cps.size(); i++) {
            double N = bsplineBasis(i, p, u, U);
            double wN = cps.get(i).weight * N;
            numeratorX += cps.get(i).x * wN;
            numeratorY += cps.get(i).y * wN;
            denominator += wN;
        }
        return new ControlPoint(numeratorX / denominator, numeratorY / denominator, 1.0);
    }

    // Rekursive Berechnung der B-Spline-Basisfunktion N_{i,p}(u).
    private double bsplineBasis(int i, int p, double u, double[] U) {
        if (p == 0) {
            if ((U[i] <= u && u < U[i + 1]) ||
                    (u == U[U.length - 1] && u >= U[i] && u <= U[i + 1]))
                return 1.0;
            return 0.0;
        } else {
            double left = 0.0, right = 0.0;
            double denom1 = U[i + p] - U[i];
            double denom2 = U[i + p + 1] - U[i + 1];
            if (denom1 != 0)
                left = (u - U[i]) / denom1 * bsplineBasis(i, p - 1, u, U);
            if (denom2 != 0)
                right = (U[i + p + 1] - u) / denom2 * bsplineBasis(i + 1, p - 1, u, U);
            return left + right;
        }
    }
}
