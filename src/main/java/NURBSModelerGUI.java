import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class NURBSModelerGUI {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NURBSModel model = new NURBSModel();
            NURBSPanel drawingPanel = new NURBSPanel(model);
            ControlPanel controlPanel = new ControlPanel(model, drawingPanel);

            // Verwende ein JSplitPane, damit das ControlPanel resizable ist.
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, drawingPanel, controlPanel);
            splitPane.setDividerLocation(550);

            JFrame frame = new JFrame("NURBS Modeler");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(splitPane);
            frame.setSize(900, 600);
            frame.setVisible(true);
        });
    }
}

// Modellklasse: speichert Kontrollpunkte, Grad und Knotenvector und enthält Methoden zur Aktualisierung.
class NURBSModel {
    private List<ControlPoint> controlPoints;
    private int degree;
    private double[] knots;

    public NURBSModel() {
        // Standard: Viertelkreis mit quadratischer NURBS (Grad = 2) und 3 Kontrollpunkten
        degree = 2;
        controlPoints = new ArrayList<>();
        controlPoints.add(new ControlPoint(1.0, 0.0, 1.0));
        controlPoints.add(new ControlPoint(Math.cos(Math.toRadians(45)), Math.sin(Math.toRadians(45)), Math.cos(Math.toRadians(45))));
        controlPoints.add(new ControlPoint(0.0, 1.0, 1.0));
        knots = generateUniformKnotVector(controlPoints.size(), degree);
    }

    public List<ControlPoint> getControlPoints() { return controlPoints; }
    public void setControlPoints(List<ControlPoint> cps) { this.controlPoints = cps; }

    public int getDegree() { return degree; }
    public void setDegree(int d) { this.degree = d; }

    public double[] getKnots() { return knots; }
    public void setKnots(double[] knots) { this.knots = knots; }

    // Generiert einen uniformen, geklammerten Knotenvector
    public double[] generateUniformKnotVector(int numControlPoints, int degree) {
        int n = numControlPoints - 1;
        int m = n + degree + 1;
        double[] kv = new double[m + 1];
        for (int i = 0; i <= m; i++) {
            if (i <= degree)
                kv[i] = 0.0;
            else if (i >= m - degree)
                kv[i] = 1.0;
            else
                kv[i] = (double)(i - degree) / (m - 2 * degree);
        }
        return kv;
    }

    // Führt Knoteneinfügung an Parameterwert u durch.
    // Dabei werden neue Kontrollpunkte in homogenen Koordinaten berechnet, sodass die Kurve unverändert bleibt.
    public void insertKnot(double u) throws IllegalArgumentException {
        double[] U = knots;
        int p = degree;
        int n = controlPoints.size() - 1;
        int m = n + p + 1;

        // Prüfe, ob u in der gültigen Domäne liegt.
        if (u < U[p] || u > U[m - p])
            throw new IllegalArgumentException("Knot value out of range.");

        // Finde k, sodass U[k] <= u < U[k+1]
        int k = p;
        while (k < m && !(U[k] <= u && u < U[k+1])) {
            k++;
        }
        // Falls u == U[m-p], setze k = m - p - 1
        if (k >= m) k = m - p - 1;

        // Erstelle neue Knotensequenz: Länge m+2
        double[] newKnots = new double[U.length + 1];
        for (int i = 0; i <= k; i++) {
            newKnots[i] = U[i];
        }
        newKnots[k+1] = u;
        for (int i = k+1; i < U.length; i++) {
            newKnots[i+1] = U[i];
        }

        // Knoteneinfügealgorithmus: arbeite in homogenen Koordinaten
        List<ControlPoint> newCP = new ArrayList<>();
        // Für i = 0 bis k-p: unverändert übernehmen.
        for (int i = 0; i < k - p + 1; i++) {
            newCP.add(controlPoints.get(i));
        }
        // Berechne neue Kontrollpunkte für i = k-p+1 bis k:
        for (int i = k - p + 1; i <= k; i++) {
            double alpha = (u - U[i]) / (U[i + p] - U[i]);
            // Umrechnung in homogene Koordinaten: H(P) = (w*x, w*y, w)
            ControlPoint Pi_1 = controlPoints.get(i-1);
            ControlPoint Pi = controlPoints.get(i);
            double X1 = Pi_1.weight * Pi_1.x;
            double Y1 = Pi_1.weight * Pi_1.y;
            double W1 = Pi_1.weight;
            double X2 = Pi.weight * Pi.x;
            double Y2 = Pi.weight * Pi.y;
            double W2 = Pi.weight;
            double X = (1 - alpha) * X1 + alpha * X2;
            double Y = (1 - alpha) * Y1 + alpha * Y2;
            double W = (1 - alpha) * W1 + alpha * W2;
            newCP.add(new ControlPoint(X / W, Y / W, W));
        }
        // Für i = k+1 bis n+1: übernehmen (shifted um 1)
        for (int i = k+1; i <= n+1; i++) {
            newCP.add(controlPoints.get(i-1));
        }

        // Aktualisiere Modell
        controlPoints = newCP;
        knots = newKnots;
    }
}

// Repräsentation eines Kontrollpunktes (x, y, weight)
class ControlPoint {
    public double x, y, weight;
    public ControlPoint(double x, double y, double weight) {
        this.x = x;
        this.y = y;
        this.weight = weight;
    }
    @Override
    public String toString() {
        return String.format("(%.3f, %.3f, %.3f)", x, y, weight);
    }
}

// Panel, das die NURBS-Kurve zeichnet, mit Mausinteraktionen (Panning und Drag & Drop).
class NURBSPanel extends JPanel {
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
                List<ControlPoint> cps = model.getControlPoints();
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
        List<ControlPoint> cps = model.getControlPoints();

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
        List<Point> curvePoints = evaluateCurvePoints(200);
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
        int x = baseOffsetX + panOffsetX + (int)(cp.x * scale);
        int y = baseOffsetY + panOffsetY - (int)(cp.y * scale);
        return new Point(x, y);
    }

    // Berechnet eine Liste von Punkten der NURBS-Kurve.
    private List<Point> evaluateCurvePoints(int numPoints) {
        List<Point> pts = new ArrayList<>();
        double[] U = model.getKnots();
        int p = model.getDegree();
        List<ControlPoint> cps = model.getControlPoints();
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
            if ((U[i] <= u && u < U[i+1]) ||
                    (u == U[U.length - 1] && u >= U[i] && u <= U[i+1]))
                return 1.0;
            return 0.0;
        } else {
            double left = 0.0, right = 0.0;
            double denom1 = U[i+p] - U[i];
            double denom2 = U[i+p+1] - U[i+1];
            if (denom1 != 0)
                left = (u - U[i]) / denom1 * bsplineBasis(i, p-1, u, U);
            if (denom2 != 0)
                right = (U[i+p+1] - u) / denom2 * bsplineBasis(i+1, p-1, u, U);
            return left + right;
        }
    }
}

// Steuerungspanel: Enthält die Tabelle der Kontrollpunkte, einen Spinner für den Grad,
// ein Textfeld zur Anzeige/Bearbeitung des Knotenvectors und Felder zum Knoteneinfügen sowie
// eine Anzeige des Kontrollpolygons.
class ControlPanel extends JPanel {
    private NURBSModel model;
    private NURBSPanel drawingPanel;
    private JTable table;
    private ControlPointTableModel tableModel;
    private JSpinner degreeSpinner;
    private JTextField knotField;
    // Für Knoteneinfügung:
    private JTextField insertKnotField;
    private JButton insertKnotButton;
    // Anzeige des Kontrollpolygons:
    private JTextArea polygonDisplay;

    public ControlPanel(NURBSModel model, NURBSPanel drawingPanel) {
        this.model = model;
        this.drawingPanel = drawingPanel;
        setLayout(new BorderLayout());

        // Tabelle für Kontrollpunkte:
        tableModel = new ControlPointTableModel(model.getControlPoints());
        table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(400, 150));

        // Panel für Knoteneinfügen:
        JPanel insertPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        insertPanel.add(new JLabel("Insert Knot:"));
        insertKnotField = new JTextField(8);
        insertPanel.add(insertKnotField);
        insertKnotButton = new JButton("Insert Knot");
        insertKnotButton.addActionListener(e -> {
            try {
                double u = Double.parseDouble(insertKnotField.getText().trim());
                // Überprüfe, ob u in der gültigen Domäne liegt:
                double[] U = model.getKnots();
                int p = model.getDegree();
                int m = U.length - 1;
                if (u < U[p] || u > U[m-p]) {
                    JOptionPane.showMessageDialog(this, "Knot value out of range.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Führe Knoteneinfügung durch:
                model.insertKnot(u);
                // Aktualisiere Knotenvector-Feld:
                knotField.setText(knotVectorToString(model.getKnots()));
                // Aktualisiere Tabelle und Polygonanzeige:
                tableModel.setControlPoints(model.getControlPoints());
                tableModel.fireTableDataChanged();
                updatePolygonDisplay();
                drawingPanel.repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid knot value format.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        insertPanel.add(insertKnotButton);

        // Panel für Grad und Knotenvector:
        JPanel paramPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        paramPanel.add(new JLabel("Degree:"));
        degreeSpinner = new JSpinner(new SpinnerNumberModel(model.getDegree(), 1, 10, 1));
        degreeSpinner.addChangeListener(e -> {
            int newDegree = (Integer) degreeSpinner.getValue();
            model.setDegree(newDegree);
            // Aktualisiere Knotenvector gemäß dem neuen Grad:
            double[] newKnots = model.generateUniformKnotVector(model.getControlPoints().size(), newDegree);
            model.setKnots(newKnots);
            knotField.setText(knotVectorToString(newKnots));
            drawingPanel.repaint();
        });
        paramPanel.add(degreeSpinner);
        paramPanel.add(new JLabel("Knot Vector (comma separated):"));
        knotField = new JTextField(30);
        knotField.setText(knotVectorToString(model.getKnots()));
        paramPanel.add(knotField);
        JButton applyKnotsButton = new JButton("Apply Knots");
        applyKnotsButton.addActionListener(e -> {
            String text = knotField.getText();
            String[] parts = text.split(",");
            List<Double> knotList = new ArrayList<>();
            try {
                for (String s : parts) {
                    knotList.add(Double.parseDouble(s.trim()));
                }
                double[] knots = new double[knotList.size()];
                for (int i = 0; i < knots.length; i++) {
                    knots[i] = knotList.get(i);
                }
                if (knots.length != model.getControlPoints().size() + model.getDegree() + 1) {
                    JOptionPane.showMessageDialog(this, "Invalid knot vector length.", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    model.setKnots(knots);
                    drawingPanel.repaint();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid knot vector format.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        paramPanel.add(applyKnotsButton);

        // Anzeige des Kontrollpolygons (als Liste):
        polygonDisplay = new JTextArea(5, 30);
        polygonDisplay.setEditable(false);
        updatePolygonDisplay();
        JScrollPane polygonScroll = new JScrollPane(polygonDisplay);
        polygonScroll.setBorder(BorderFactory.createTitledBorder("Control Polygon"));

        // Zusammenstellen des ControlPanels:
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(tableScroll, BorderLayout.CENTER);
        topPanel.add(insertPanel, BorderLayout.SOUTH);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(paramPanel, BorderLayout.NORTH);
        bottomPanel.add(polygonScroll, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.CENTER);
    }

    private void updatePolygonDisplay() {
        StringBuilder sb = new StringBuilder();
        List<ControlPoint> cps = model.getControlPoints();
        for (int i = 0; i < cps.size(); i++) {
            sb.append("P").append(i).append(" = ").append(cps.get(i).toString()).append("\n");
        }
        polygonDisplay.setText(sb.toString());
    }

    private String knotVectorToString(double[] knots) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < knots.length; i++) {
            sb.append(knots[i]);
            if (i < knots.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}

// TableModel zur Darstellung und Bearbeitung der Kontrollpunkte.
class ControlPointTableModel extends AbstractTableModel {
    private List<ControlPoint> controlPoints;
    private String[] columnNames = {"X", "Y", "Weight"};

    public ControlPointTableModel(List<ControlPoint> controlPoints) {
        this.controlPoints = controlPoints;
    }

    public void setControlPoints(List<ControlPoint> cps) {
        this.controlPoints = cps;
    }

    @Override
    public int getRowCount() {
        return controlPoints.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ControlPoint cp = controlPoints.get(rowIndex);
        switch (columnIndex) {
            case 0: return cp.x;
            case 1: return cp.y;
            case 2: return cp.weight;
            default: return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            double val = Double.parseDouble(aValue.toString());
            ControlPoint cp = controlPoints.get(rowIndex);
            switch (columnIndex) {
                case 0: cp.x = val; break;
                case 1: cp.y = val; break;
                case 2: cp.weight = val; break;
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        } catch (NumberFormatException e) {
            // Ungültige Eingabe ignorieren.
        }
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
}
