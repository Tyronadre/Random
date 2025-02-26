package gui;

import data.ControlPoint;
import data.NURBSModel;
import gui.events.EventSystem;

import javax.swing.table.AbstractTableModel;
import java.util.List;

// TableModel zur Darstellung und Bearbeitung der Kontrollpunkte.
public class ControlPointTableModel extends AbstractTableModel {
    private List<ControlPoint> controlPoints;
    private int currentDimension = 2;

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
        return currentDimension + 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ControlPoint cp = controlPoints.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> cp.x;
            case 1 -> cp.y;
            case 2 -> currentDimension == 2 ? cp.weight : cp.z;
            case 3 -> cp.weight;
            default -> null;
        };
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
                case 0:
                    cp.x = val;
                    break;
                case 1:
                    cp.y = val;
                    break;
                case 2:
                    if (currentDimension == 2)
                        cp.weight = val;
                    else
                        cp.z = val;
                    break;
                case 3:
                    cp.weight = val;
                    break;
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        } catch (NumberFormatException e) {
            // Ungültige Eingabe ignorieren.
            System.err.println("Invalid input: " + aValue);
        }
    }

    public void setCurrentDimension(int dim) {
        currentDimension = dim;
        fireTableStructureChanged();
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "X";
            case 1 -> "Y";
            case 2 -> currentDimension == 2 ? "Weight" : "Z";
            case 3 -> "Weight";
            default -> "";
        };
    }
}
