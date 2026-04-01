package com.rewear.ui;

import com.rewear.Session;
import com.rewear.dao.ExchangeDAO;
import com.rewear.models.ExchangeRecord;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * After an exchange, submit a review into {@code REVIEWS}.
 */
public class ReviewFrame extends JFrame {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DashboardFrame parent;
    private final ExchangeDAO exchangeDAO = new ExchangeDAO();

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<ExchangeRecord> rows = new ArrayList<>();

    public ReviewFrame(DashboardFrame parent) {
        super("Reviews");
        this.parent = parent;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(920, 440);
        setLocationRelativeTo(parent);

        String[] cols = {"Exchange ID", "Item", "Other party", "When"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Completed exchanges awaiting your review", SwingConstants.CENTER);
        root.add(title, BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Refresh");
        JButton submit = new JButton("Submit Review…");
        JButton close = new JButton("Close");
        refresh.addActionListener(e -> reload());
        submit.addActionListener(e -> submitReview());
        close.addActionListener(e -> dispose());
        actions.add(refresh);
        actions.add(submit);
        actions.add(close);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
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
            rows.addAll(exchangeDAO.listExchangesAwaitingReview(u.getUserId()));
            tableModel.setRowCount(0);
            for (ExchangeRecord r : rows) {
                Vector<Object> v = new Vector<>();
                v.add(r.getExchangeId());
                v.add(r.getItemName());
                v.add(r.getOtherPartyUsername());
                v.add(r.getCreatedAt() != null ? FMT.format(r.getCreatedAt()) : "");
                tableModel.addRow(v);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not load exchanges: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitReview() {
        var u = Session.getCurrentUser();
        if (u == null) {
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an exchange first.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ExchangeRecord rec = rows.get(row);

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Rating (1–5):"));
        JTextField ratingField = new JTextField("5", 4);
        panel.add(ratingField);
        panel.add(new JLabel("Comment:"));
        JTextArea comment = new JTextArea(4, 32);
        comment.setLineWrap(true);
        panel.add(new JScrollPane(comment));

        int ok = JOptionPane.showConfirmDialog(this, panel, "Review exchange #" + rec.getExchangeId(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        int rating;
        try {
            rating = Integer.parseInt(ratingField.getText().trim());
            if (rating < 1 || rating > 5) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Rating must be between 1 and 5.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String text = comment.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a short comment.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            exchangeDAO.submitReview(rec.getExchangeId(), u.getUserId(), rating, text);
            JOptionPane.showMessageDialog(this, "Thank you — review saved.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            reload();
            parent.refreshHeader();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
