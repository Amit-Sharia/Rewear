package com.rewear.ui;

import com.rewear.Session;
import com.rewear.dao.ExchangeDAO;
import com.rewear.dao.ItemDAO;
import com.rewear.models.ItemBrowseRow;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.ImageIcon;
import javax.swing.JSplitPane;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Image;
import java.net.URL;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Lists available items from other users; supports requesting an exchange.
 */
public class BrowseItemsFrame extends JFrame {

    private final DashboardFrame parent;
    private final ItemDAO itemDAO = new ItemDAO();
    private final ExchangeDAO exchangeDAO = new ExchangeDAO();

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<ItemBrowseRow> rows = new ArrayList<>();
    private final JTextArea detailsArea = new JTextArea();
    private final JLabel imagePreview = new JLabel("No image", SwingConstants.CENTER);

    public BrowseItemsFrame(DashboardFrame parent) {
        super("Browse Items");
        this.parent = parent;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 420);
        setLocationRelativeTo(parent);

        String[] cols = {"ID", "Name", "Brand", "Owner", "Points", "Category", "Size", "Condition", "Description"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Available items (sample data from your database)", SwingConstants.CENTER);
        root.add(title, BorderLayout.NORTH);
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        imagePreview.setOpaque(true);
        imagePreview.setBackground(new Color(245, 245, 245));

        JPanel detailsPanel = new JPanel(new BorderLayout(6, 6));
        detailsPanel.add(imagePreview, BorderLayout.CENTER);
        detailsPanel.add(new JScrollPane(detailsArea), BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(table), detailsPanel);
        splitPane.setResizeWeight(0.7);
        root.add(splitPane, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Refresh");
        JButton request = new JButton("Request Exchange");
        JButton close = new JButton("Close");
        refresh.addActionListener(e -> reload());
        request.addActionListener(e -> requestExchange());
        close.addActionListener(e -> dispose());
        actions.add(refresh);
        actions.add(request);
        actions.add(close);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedDetails();
            }
        });
        reload();
    }

    private void reload() {
        var u = Session.getCurrentUser();
        if (u == null) {
            JOptionPane.showMessageDialog(this, "Not logged in.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            rows.clear();
            rows.addAll(itemDAO.listAvailableExcludingOwner(u.getUserId()));
            tableModel.setRowCount(0);
            for (ItemBrowseRow r : rows) {
                Vector<Object> v = new Vector<>();
                v.add(r.getItemId());
                v.add(r.getItemName());
                v.add(r.getBrand());
                v.add(r.getOwnerUsername());
                v.add(r.getPointsValue());
                v.add(r.getCategoryName());
                v.add(r.getSizeLabel());
                v.add(r.getConditionLabel());
                v.add(r.getDescription());
                tableModel.addRow(v);
            }
            if (!rows.isEmpty()) {
                table.setRowSelectionInterval(0, 0);
                showSelectedDetails();
            } else {
                detailsArea.setText("");
                imagePreview.setIcon(null);
                imagePreview.setText("No image");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not load items: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSelectedDetails() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= rows.size()) {
            return;
        }
        ItemBrowseRow item = rows.get(row);
        detailsArea.setText(item.getDescription() == null ? "" : item.getDescription());
        renderImage(item.getImageUrl());
    }

    private void renderImage(String imageRef) {
        imagePreview.setIcon(null);
        imagePreview.setText("No image");
        if (imageRef == null || imageRef.isBlank()) {
            return;
        }
        try {
            ImageIcon rawIcon;
            if (imageRef.startsWith("http://") || imageRef.startsWith("https://")) {
                rawIcon = new ImageIcon(new URL(imageRef));
            } else {
                rawIcon = new ImageIcon(new File(imageRef).getAbsolutePath());
            }
            if (rawIcon.getIconWidth() <= 0) {
                return;
            }
            Image scaled = rawIcon.getImage().getScaledInstance(220, 220, Image.SCALE_SMOOTH);
            imagePreview.setText("");
            imagePreview.setIcon(new ImageIcon(scaled));
        } catch (Exception ignored) {
            imagePreview.setText("Image unavailable");
        }
    }

    private void requestExchange() {
        var u = Session.getCurrentUser();
        if (u == null) {
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an item first.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ItemBrowseRow item = rows.get(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Request exchange for \"" + item.getItemName() + "\" costing " + item.getPointsValue() + " points?",
                "Confirm exchange", JOptionPane.OK_CANCEL_OPTION);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            exchangeDAO.requestExchange(u.getUserId(), item.getItemId());
            JOptionPane.showMessageDialog(this, "Request sent. Owner must accept before points are deducted.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            parent.refreshHeader();
            reload();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Exchange failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
