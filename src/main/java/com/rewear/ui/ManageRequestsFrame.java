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
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class ManageRequestsFrame extends BaseFrame {
    private final DashboardFrame parent;
    private final ExchangeDAO exchangeDAO = new ExchangeDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<ExchangeRecord> rows = new ArrayList<>();

    public ManageRequestsFrame(DashboardFrame parent) {
        super("Incoming Requests", 860, 420);
        this.parent = parent;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);

        String[] cols = {"Exchange ID", "Item", "Requester", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(new JLabel("Pending or accepted requests", SwingConstants.CENTER), BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Refresh");
        JButton reject = new JButton("Reject");
        JButton accept = new JButton("Accept");
        JButton complete = new JButton("Complete");
        JButton close = new JButton("Close");
        refresh.addActionListener(e -> reload());
        reject.addActionListener(e -> rejectRequest());
        accept.addActionListener(e -> acceptRequest());
        complete.addActionListener(e -> completeRequest());
        close.addActionListener(e -> dispose());
        actions.add(refresh);
        actions.add(reject);
        actions.add(accept);
        actions.add(complete);
        actions.add(close);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
        reload();
    }

    private void reload() {
        var u = Session.getCurrentUser();
        if (u == null) {
            return;
        }
        try {
            rows.clear();
            rows.addAll(exchangeDAO.listOwnerPendingRequests(u.getUserId()));
            tableModel.setRowCount(0);
            for (ExchangeRecord r : rows) {
                Vector<Object> v = new Vector<>();
                v.add(r.getExchangeId());
                v.add(r.getItemName());
                v.add(r.getOtherPartyUsername());
                v.add(r.getStatus());
                tableModel.addRow(v);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ExchangeRecord selected() {
        int idx = table.getSelectedRow();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Select a request first.", "Validation", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return rows.get(idx);
    }

    private void rejectRequest() {
        var u = Session.getCurrentUser();
        if (u == null) {
            return;
        }
        ExchangeRecord r = selected();
        if (r == null) {
            return;
        }
        try {
            exchangeDAO.rejectRequest(r.getExchangeId(), u.getUserId());
            JOptionPane.showMessageDialog(this, "Request rejected.", "Done", JOptionPane.INFORMATION_MESSAGE);
            reload();
            parent.refreshHeader();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void acceptRequest() {
        var u = Session.getCurrentUser();
        if (u == null) {
            return;
        }
        ExchangeRecord r = selected();
        if (r == null) {
            return;
        }
        try {
            exchangeDAO.acceptRequest(r.getExchangeId(), u.getUserId());
            JOptionPane.showMessageDialog(this, "Request accepted. Complete it after handoff/delivery.",
                    "Done", JOptionPane.INFORMATION_MESSAGE);
            parent.refreshHeader();
            reload();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void completeRequest() {
        var u = Session.getCurrentUser();
        if (u == null) {
            return;
        }
        ExchangeRecord r = selected();
        if (r == null) {
            return;
        }
        if (!"ACCEPTED".equalsIgnoreCase(r.getStatus())) {
            JOptionPane.showMessageDialog(this, "Only ACCEPTED requests can be completed.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Complete exchange #" + r.getExchangeId() + " now?\nThis will transfer points and mark items exchanged.",
                "Confirm completion", JOptionPane.OK_CANCEL_OPTION);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            exchangeDAO.completeAcceptedRequest(r.getExchangeId(), u.getUserId());
            JOptionPane.showMessageDialog(this, "Exchange completed successfully.", "Done",
                    JOptionPane.INFORMATION_MESSAGE);
            parent.refreshHeader();
            reload();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
