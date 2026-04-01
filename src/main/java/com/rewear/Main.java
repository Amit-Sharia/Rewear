package com.rewear;

import com.rewear.ui.LoginFrame;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point: launches the Swing login window on the EDT.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {
            // default L&F
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
