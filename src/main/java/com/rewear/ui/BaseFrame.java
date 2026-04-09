package com.rewear.ui;

import javax.swing.*;

/**
 * Base frame with shared UI helpers and standard initialization.
 */
public abstract class BaseFrame extends JFrame {

    protected BaseFrame(String title, int width, int height) {
        super(title);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(width, height);
        setLocationRelativeTo(null);
    }

    protected void showInfo(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    protected void showWarning(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    protected void showError(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
