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

            // Verwende ein JSplitPane, damit das GUI.ControlPanel resizable ist.
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

