import data.NURBSModel;
import gui.ControlPanel;
import gui.NURBSPanel;

import javax.swing.*;

public class NURBSModelerGUI {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NURBSModel model = new NURBSModel();
            NURBSPanel drawingPanel = new NURBSPanel(model);
            ControlPanel controlPanel = new ControlPanel(model, drawingPanel);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPanel, drawingPanel);
            splitPane.setDividerLocation(300);
            splitPane.setEnabled(false);

            JFrame frame = new JFrame("NURBS Modeler");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(splitPane);
            frame.setSize(1000, 800);
            frame.setVisible(true);
        });
    }
}

