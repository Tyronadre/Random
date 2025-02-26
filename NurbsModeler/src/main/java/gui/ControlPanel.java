package gui;

import data.NURBSModel;
import gui.events.ControlPointMovedEvent;
import gui.events.EventSystem;
import gui.events.KnotVectorChangedEvent;
import gui.events.NURBSResolutionChangedEvent;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ControlPanel extends JPanel {
    private final JTextField insertKnotField;
    private final EventSystem eventSystem = EventSystem.getInstance();

    public ControlPanel(NURBSModel model, NURBSPanel drawingPanel) {
        setLayout(new GridBagLayout());

        // Tabelle für Kontrollpunkte:
        JScrollPane controlPointsTable = getControlPointsTable(model, drawingPanel);

        // Panel to insert Knots:
        JPanel knotControl = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton insertKnotButton = getInsertKnotButton(model);
        insertKnotField = new JTextField(8);
        insertKnotField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    insertKnotButton.doClick();
                }
            }
        });
        knotControl.add(insertKnotButton);
        knotControl.add(insertKnotField);

        // Panel for other stuff:
        JPanel paramPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        paramPanel.add(new JLabel("Degree:"));
        var degreeSpinner = new JSpinner(new SpinnerNumberModel(model.getDegree(), 1, 10, 1));
        degreeSpinner.addChangeListener(e -> {
            int newDegree = (Integer) degreeSpinner.getValue();
            model.setDegree(newDegree);
            double[] newKnots = model.generateUniformKnotVector(model.getControlPoints().size(), newDegree);
            model.setKnots(newKnots);
            eventSystem.dispatch(new KnotVectorChangedEvent(this, newKnots));
        });
        paramPanel.add(degreeSpinner);
        var resolutionSpinner = new JSpinner(new SpinnerNumberModel(200, 100, 1000, 100));
        resolutionSpinner.addChangeListener(e -> eventSystem.dispatch(new NURBSResolutionChangedEvent(this, (Integer) resolutionSpinner.getValue())));
        paramPanel.add(new JLabel("Resolution:"));
        paramPanel.add(resolutionSpinner);

        var knotPanel = new KnotPanel(model);

        // Zusammenstellen des ControlPanels:
        var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        add(paramPanel, gbc);
        gbc.gridy = 1;
        add(knotControl, gbc);
        gbc.gridy = 2;

        add(knotPanel, gbc);
        gbc.gridy = 3;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        add(controlPointsTable, gbc);

    }

    private JScrollPane getControlPointsTable(NURBSModel model, NURBSPanel drawingPanel) {
        var controlPointsTableModel = new ControlPointTableModel(model.getControlPoints());
        controlPointsTableModel.addTableModelListener(e -> drawingPanel.repaint());
        JTable table = new JTable(controlPointsTableModel);
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                int row = table.rowAtPoint(table.getMousePosition());
                if (row == -1) {
                    return;
                }
                table.setRowSelectionInterval(row, row);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        JMenuItem deleteItem = new JMenuItem("Delete");
        popupMenu.add(deleteItem);
        deleteItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                // Testen ob dieser Punkt noch entfernt werden kann, um die Mindestanzahl an Kontrollpunkten zu gewährleisten
                if (controlPointsTableModel.getRowCount() <= model.getDegree() + 1) {
                    JOptionPane.showMessageDialog(this, "Cannot remove control point. Minimum number of control points reached.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                model.removeControlPoint(row);
                drawingPanel.repaint();
                this.repaint();
            }
        });
        table.setComponentPopupMenu(popupMenu);

        var controlPointsTable = new JScrollPane(table);

        eventSystem.subscribe(KnotVectorChangedEvent.class, e -> {
            controlPointsTableModel.setControlPoints(model.getControlPoints());
            controlPointsTableModel.fireTableDataChanged();
        });
        eventSystem.subscribe(ControlPointMovedEvent.class, e -> controlPointsTable.repaint());

        return controlPointsTable;
    }

    private JButton getInsertKnotButton(NURBSModel model) {
        JButton insertKnotButton = new JButton("Insert Knot:");
        insertKnotButton.addActionListener(e -> {
            try {
                double u = Double.parseDouble(insertKnotField.getText().trim().replaceAll(",", "."));
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
                // Aktualisiere Tabelle und Polygonanzeige:
                eventSystem.dispatch(new KnotVectorChangedEvent(this, model.getKnots()));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid knot value format.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (IndexOutOfBoundsException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return insertKnotButton;
    }
}
