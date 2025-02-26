package gui;

import data.ControlPoint;
import data.NURBSModel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Steuerungspanel: Enthält die Tabelle der Kontrollpunkte, einen Spinner für den Grad,
// ein Textfeld zur Anzeige/Bearbeitung des Knotenvectors und Felder zum Knoteneinfügen sowie
// eine Anzeige des Kontrollpolygons.
public class ControlPanel extends JPanel {
    private final NURBSModel model;
    private final ControlPointTableModel tableModel;
    private final JSpinner degreeSpinner;
    private JTextField knotField;
    // Für Knoteneinfügung:
    private final JTextField insertKnotField;
    // Anzeige des Kontrollpolygons:
    private final JTextArea polygonDisplay;

    public ControlPanel(NURBSModel model, NURBSPanel drawingPanel) {
        this.model = model;
        setLayout(new BorderLayout());

        // Tabelle für Kontrollpunkte:
        tableModel = new ControlPointTableModel(model.getControlPoints());
        JTable table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(400, 150));

        // Panel für Knoteneinfügen:
        JPanel insertPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        insertPanel.add(new JLabel("Insert Knot:"));
        insertKnotField = new JTextField(8);
        insertPanel.add(insertKnotField);
        JButton insertKnotButton = getInsertKnotButton(model, drawingPanel);
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
        JButton applyKnotsButton = getApplyKnotsButton(model, drawingPanel);
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

    private JButton getApplyKnotsButton(NURBSModel model, NURBSPanel drawingPanel) {
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
        return applyKnotsButton;
    }

    private JButton getInsertKnotButton(NURBSModel model, NURBSPanel drawingPanel) {
        JButton insertKnotButton = new JButton("Insert Knot");
        insertKnotButton.addActionListener(e -> {
            try {
                double u = Double.parseDouble(insertKnotField.getText().trim());
                // Überprüfe, ob u in der gültigen Domäne liegt:
                double[] U = model.getKnots();
                int p = model.getDegree();
                int m = U.length - 1;
                if (u < U[p] || u > U[m - p]) {
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
        return insertKnotButton;
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
