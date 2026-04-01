package com.rewear.ui;

import com.rewear.dao.UserDAO;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;

/**
 * Registration form inserts into {@code USERS}.
 */
public class RegisterFrame extends JFrame {

    private final JTextField usernameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final UserDAO userDAO = new UserDAO();

    public RegisterFrame() {
        super("ReWear – Register");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 300);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Create an account", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(title.getFont().getSize() + 2f));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Username:"));
        form.add(usernameField);
        form.add(new JLabel("Email:"));
        form.add(emailField);
        form.add(new JLabel("Password:"));
        form.add(passwordField);
        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backBtn = new JButton("Back to Login");
        JButton saveBtn = new JButton("Register");
        backBtn.addActionListener(e -> backToLogin());
        saveBtn.addActionListener(e -> doRegister());
        actions.add(backBtn);
        actions.add(saveBtn);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void doRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (userDAO.usernameExists(username)) {
                JOptionPane.showMessageDialog(this, "Username already taken.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            userDAO.register(username, email, password);
            JOptionPane.showMessageDialog(this, "Account created. You can log in now.", "Success", JOptionPane.INFORMATION_MESSAGE);
            backToLogin();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void backToLogin() {
        dispose();
        new LoginFrame().setVisible(true);
    }
}
