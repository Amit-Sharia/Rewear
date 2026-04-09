package com.rewear.ui;

import com.rewear.Session;
import com.rewear.dao.ItemDAO;
import com.rewear.models.Lookup;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * Form to insert a clothing item into {@code ITEMS} (and optional {@code ITEM_IMAGES}).
 */
public class AddItemFrame extends BaseFrame {

    private final DashboardFrame parent;
    private final ItemDAO itemDAO = new ItemDAO();

    private final JTextField nameField = new JTextField();
    private final JTextField brandField = new JTextField();
    private final JTextField pointsField = new JTextField();
    private final JTextField imageUrlField = new JTextField();
    private final JTextArea descArea = new JTextArea(4, 24);

    private final JComboBox<Lookup> categoryCombo = new JComboBox<>();
    private final JComboBox<Lookup> sizeCombo = new JComboBox<>();
    private final JComboBox<Lookup> conditionCombo = new JComboBox<>();

    public AddItemFrame(DashboardFrame parent) {
        super("Add Item", 480, 460);
        this.parent = parent;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("List a clothing item", SwingConstants.CENTER);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Name:"));
        form.add(nameField);
        form.add(new JLabel("Brand:"));
        form.add(brandField);
        form.add(new JLabel("Points value:"));
        form.add(pointsField);
        form.add(new JLabel("Category:"));
        form.add(categoryCombo);
        form.add(new JLabel("Size:"));
        form.add(sizeCombo);
        form.add(new JLabel("Condition:"));
        form.add(conditionCombo);
        form.add(new JLabel("Image URL (optional):"));
        JPanel imageRow = new JPanel(new BorderLayout(6, 0));
        JButton browseImage = new JButton("Choose...");
        browseImage.addActionListener(e -> chooseImageFile());
        imageRow.add(imageUrlField, BorderLayout.CENTER);
        imageRow.add(browseImage, BorderLayout.EAST);
        form.add(imageRow);

        root.add(form, BorderLayout.CENTER);

        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.add(new JLabel("Description:"), BorderLayout.NORTH);
        south.add(new JScrollPane(descArea), BorderLayout.CENTER);

        JPanel actions = new JPanel();
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Close");
        save.addActionListener(e -> saveItem());
        cancel.addActionListener(e -> dispose());
        actions.add(cancel);
        actions.add(save);
        south.add(actions, BorderLayout.SOUTH);

        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        loadLookups();
    }

    private void chooseImageFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select item photo");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected != null) {
                imageUrlField.setText(selected.getAbsolutePath());
            }
        }
    }

    private void loadLookups() {
        try {
            fillCombo(categoryCombo, itemDAO.listCategories());
            fillCombo(sizeCombo, itemDAO.listSizes());
            fillCombo(conditionCombo, itemDAO.listConditions());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not load categories/sizes: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void fillCombo(JComboBox<Lookup> combo, List<Lookup> list) {
        combo.removeAllItems();
        for (Lookup l : list) {
            combo.addItem(l);
        }
    }

    private void saveItem() {
        var u = Session.getCurrentUser();
        if (u == null) {
            JOptionPane.showMessageDialog(this, "Not logged in.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String name = nameField.getText().trim();
        String brand = brandField.getText().trim();
        String pts = pointsField.getText().trim();
        if (name.isEmpty() || brand.isEmpty() || pts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name, brand, and points are required.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int points;
        try {
            points = Integer.parseInt(pts);
            if (points < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Points must be a non-negative number.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Lookup cat = (Lookup) categoryCombo.getSelectedItem();
        Lookup sz = (Lookup) sizeCombo.getSelectedItem();
        Lookup cond = (Lookup) conditionCombo.getSelectedItem();
        if (cat == null || sz == null || cond == null) {
            JOptionPane.showMessageDialog(this, "Select category, size, and condition.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String desc = descArea.getText().trim();
        String img = imageUrlField.getText().trim();
        try {
            itemDAO.insertItem(u.getUserId(), cat.getId(), sz.getId(), cond.getId(), name, brand, desc, points,
                    img.isEmpty() ? null : img);
            JOptionPane.showMessageDialog(this, "Item saved.", "Success", JOptionPane.INFORMATION_MESSAGE);
            parent.refreshHeader();
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
