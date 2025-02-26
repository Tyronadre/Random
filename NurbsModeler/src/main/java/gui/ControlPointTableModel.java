package gui;

import data.ControlPoint;

import javax.swing.table.AbstractTableModel;
import java.util.List;

// TableModel zur Darstellung und Bearbeitung der Kontrollpunkte.
public class ControlPointTableModel extends AbstractTableModel {
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
            case 0:
                return cp.x;
            case 1:
                return cp.y;
            case 2:
                return cp.weight;
            default:
                return null;
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
                case 0:
                    cp.x = val;
                    break;
                case 1:
                    cp.y = val;
                    break;
                case 2:
                    cp.weight = val;
                    break;
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
