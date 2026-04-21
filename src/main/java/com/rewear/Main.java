package com.rewear;

import com.rewear.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.LookAndFeel;

/**
 * Entry point: launches the Swing login window on the EDT.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        installLookAndFeelSafely();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private static void installLookAndFeelSafely() {
        try {
            // Load FlatLaf only if it is present on the runtime classpath.
            Class<?> lafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            Object lafInstance = lafClass.getDeclaredConstructor().newInstance();
            if (lafInstance instanceof LookAndFeel lookAndFeel) {
                UIManager.setLookAndFeel(lookAndFeel);
            }
        } catch (Throwable ignored) {
            // Keep Swing default look and feel if FlatLaf is unavailable.
        }
    }
}
