package gui;

import data.NURBSModel;
import gui.events.EventSystem;
import gui.events.KnotVectorChangedEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;

class KnotPanel extends JPanel {
    EventSystem eventSystem = EventSystem.getInstance();
    double[] knots = new double[0];
    JTextField[] knotFields;
    DecimalFormat df = new DecimalFormat("#.####");
    JButton applyButton = new JButton("Apply");

    public KnotPanel(NURBSModel model) {
        setLayout(new WrapLayout(FlowLayout.LEFT));
        eventSystem.subscribe(KnotVectorChangedEvent.class, e -> updateFields(e.getNewKnotVector()));

        updateFields(model.getKnots());

        applyButton.addActionListener(e -> {
            try {
                double[] newKnots = new double[knotFields.length];
                for (int i = 0; i < knotFields.length; i++) {
                    newKnots[i] = Double.parseDouble(knotFields[i].getText().trim().replaceAll(",", "."));
                }
                model.setKnots(newKnots);
                eventSystem.dispatch(new KnotVectorChangedEvent(this, newKnots));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid knot value format.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        setBackground(Color.LIGHT_GRAY);
    }

    private void updateFields(double[] newKnotVector) {
        if (newKnotVector.length != knots.length) {
            this.removeAll();
            knotFields = new JTextField[newKnotVector.length];
            for (int i = 0; i < newKnotVector.length; i++) {
                knotFields[i] = new JTextField(5);
                knotFields[i].setText(df.format(newKnotVector[i]));
                knotFields[i].addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                            applyButton.doClick();
                        }
                    }
                });
                add(knotFields[i]);
            }
        } else {
            for (int i = 0; i < newKnotVector.length; i++) {
                knotFields[i].setText(String.valueOf(newKnotVector[i]));
            }
        }
        add(applyButton);
        this.revalidate();
    }
}
