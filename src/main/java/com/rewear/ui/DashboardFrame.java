package com.rewear.ui;

import com.rewear.Session;
import com.rewear.dao.UserDAO;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;

/**
 * Post-login menu: Add Item, Browse, My Items, Reviews, Logout.
 */
public class DashboardFrame extends BaseFrame {

    private final JLabel welcomeLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel statsLabel = new JLabel("", SwingConstants.CENTER);
    private final UserDAO userDAO = new UserDAO();

    public DashboardFrame() {
        super("ReWear – Dashboard", 420, 360);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        welcomeLabel.setFont(welcomeLabel.getFont().deriveFont(welcomeLabel.getFont().getSize() + 2f));
        statsLabel.setFont(statsLabel.getFont().deriveFont(statsLabel.getFont().getSize() - 1f));
        JPanel header = new JPanel(new GridLayout(0, 1, 0, 4));
        header.add(welcomeLabel);
        header.add(statsLabel);
        root.add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 1, 8, 8));
        JButton addBtn = new JButton("Add Item");
        addBtn.addActionListener(e -> new AddItemFrame(this).setVisible(true));
        grid.add(addBtn);

        JButton browseBtn = new JButton("Browse Items");
        browseBtn.addActionListener(e -> new BrowseItemsFrame(this).setVisible(true));
        grid.add(browseBtn);

        JButton myBtn = new JButton("My Items");
        myBtn.addActionListener(e -> new MyItemsFrame(this).setVisible(true));
        grid.add(myBtn);

        JButton revBtn = new JButton("Reviews (after exchange)");
        revBtn.addActionListener(e -> new ReviewFrame(this).setVisible(true));
        grid.add(revBtn);

        JButton reqBtn = new JButton("Manage Requests");
        reqBtn.addActionListener(e -> new ManageRequestsFrame(this).setVisible(true));
        grid.add(reqBtn);

        JButton chatBtn = new JButton("Chats & Pickup");
        chatBtn.addActionListener(e -> new ChatListFrame(this).setVisible(true));
        grid.add(chatBtn);

        JButton pointsBtn = new JButton("Add 50 Demo Points");
        pointsBtn.addActionListener(e -> addDemoPoints());
        grid.add(pointsBtn);

        JButton outBtn = new JButton("Logout");
        outBtn.addActionListener(e -> logout());
        grid.add(outBtn);

        root.add(grid, BorderLayout.CENTER);

        setContentPane(root);
        refreshWelcome();
    }

    private void refreshWelcome() {
        var u = Session.getCurrentUser();
        if (u == null) {
            welcomeLabel.setText("Not logged in");
            statsLabel.setText("");
            return;
        }
        try {
            var fresh = userDAO.findById(u.getUserId());
            fresh.ifPresent(Session::setCurrentUser);
            u = Session.getCurrentUser();
        } catch (SQLException ignored) {
            // keep existing session user for label
        }
        if (u != null) {
            welcomeLabel.setText("Hello, " + u.getUsername() + " — Points: " + u.getPointsBalance());
            try {
                int availableItems = userDAO.getAvailableItemsCount(u.getUserId());
                double avgRating = userDAO.getOwnerAverageRating(u.getUserId());
                statsLabel.setText(String.format("Available Items: %d | Average Rating: %.2f", availableItems, avgRating));
            } catch (SQLException ignored) {
                statsLabel.setText("Available Items: n/a | Average Rating: n/a");
            }
        }
    }

    /** Call when returning from a child window so points stay up to date. */
    public void refreshHeader() {
        refreshWelcome();
        revalidate();
        repaint();
    }

    private void logout() {
        Session.clear();
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void addDemoPoints() {
        var u = Session.getCurrentUser();
        if (u == null) {
            return;
        }
        try {
            userDAO.addPoints(u.getUserId(), 50, "Demo top-up from dashboard");
            refreshHeader();
            JOptionPane.showMessageDialog(this, "50 points added.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
