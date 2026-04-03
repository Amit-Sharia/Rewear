package com.rewear.ui;

import com.rewear.Session;
import com.rewear.dao.UserDAO;
import com.rewear.exceptions.ValidationException;
import com.rewear.models.User;
import com.rewear.validators.InputValidator;

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
 * Login screen: validates credentials against {@code USERS}.
 */
public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        super("ReWear – Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 260);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Community Clothing Exchange", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(title.getFont().getSize() + 4f));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Username:"));
        form.add(usernameField);
        form.add(new JLabel("Password:"));
        form.add(passwordField);
        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> openRegister());
        actions.add(registerBtn);
        actions.add(loginBtn);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        // Basic empty field validation
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter username and password.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Validate username format
            InputValidator.validateUsername(username);
            
            var opt = userDAO.login(username, password);
            if (opt.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            User u = opt.get();
            Session.setCurrentUser(u);
            dispose();
            new DashboardFrame().setVisible(true);
        } catch (ValidationException vex) {
            JOptionPane.showMessageDialog(this, vex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openRegister() {
        dispose();
        new RegisterFrame().setVisible(true);
    }
}
