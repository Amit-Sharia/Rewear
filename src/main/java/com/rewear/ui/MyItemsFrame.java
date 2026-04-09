package com.rewear.ui;

import com.rewear.Session;
import com.rewear.dao.ItemDAO;
import com.rewear.models.MyItemRow;

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
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Lists the current user's listed items.
 */
public class MyItemsFrame extends BaseFrame {

    private final DashboardFrame parent;
    private final ItemDAO itemDAO = new ItemDAO();

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<MyItemRow> rows = new ArrayList<>();
    private final JTextArea detailsArea = new JTextArea();
    private final JLabel imagePreview = new JLabel("No image", SwingConstants.CENTER);

    public MyItemsFrame(DashboardFrame parent) {
        super("My Items", 980, 460);
        this.parent = parent;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);

        String[] cols = {"ID", "Name", "Brand", "Points", "Category", "Size", "Condition", "Status", "Description"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Items you listed", SwingConstants.CENTER);
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
        JButton removeBtn = new JButton("Remove Item");
        JButton close = new JButton("Close");
        refresh.addActionListener(e -> reload());
        removeBtn.addActionListener(e -> removeSelectedItem());
        close.addActionListener(e -> dispose());
        actions.add(removeBtn);
        actions.add(refresh);
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
            rows.addAll(itemDAO.listMyItems(u.getUserId()));
            tableModel.setRowCount(0);
            for (MyItemRow r : rows) {
                Vector<Object> v = new Vector<>();
                v.add(r.getItemId());
                v.add(r.getItemName());
                v.add(r.getBrand());
                v.add(r.getPointsValue());
                v.add(r.getCategoryName());
                v.add(r.getSizeLabel());
                v.add(r.getConditionLabel());
                v.add(r.getStatus());
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
        parent.refreshHeader();
    }

    private void showSelectedDetails() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= rows.size()) {
            return;
        }
        MyItemRow item = rows.get(row);
        detailsArea.setText(item.getDescription() == null ? "" : item.getDescription());
        renderImage(item.getImageUrl());
    }

    private void removeSelectedItem() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= rows.size()) {
            JOptionPane.showMessageDialog(this, "Please select an item to remove.", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        MyItemRow item = rows.get(row);

        // Check if item is in any exchanges
        try {
            int exchangeCount = itemDAO.countExchangesForItem(item.getItemId());
            String message;
            if (exchangeCount > 0) {
                message = "Are you sure you want to delete \"" + item.getItemName() + "\"?\n\n" +
                        "This item is part of " + exchangeCount + " exchange(s).\n" +
                        "All related exchange records will also be removed.\n\n" +
                        "This action cannot be undone.";
            } else {
                message = "Are you sure you want to delete \"" + item.getItemName() + "\"?\n\n" +
                        "This action cannot be undone.";
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    message,
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            itemDAO.deleteItem(item.getItemId());
            JOptionPane.showMessageDialog(this, "Item deleted successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            reload();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                    "Could not delete item:\n" + ex.getMessage() + 
                    "\n\nIf this persists, contact support.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
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
}
