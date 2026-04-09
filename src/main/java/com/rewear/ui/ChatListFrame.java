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

public class ChatListFrame extends BaseFrame {
    private final ExchangeDAO exchangeDAO = new ExchangeDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<ExchangeRecord> rows = new ArrayList<>();

    public ChatListFrame(JFrame parent) {
        super("Chats & Pickup", 860, 420);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);

        String[] cols = {"Exchange ID", "Item", "Other Party", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(new JLabel("Completed exchanges available for chat", SwingConstants.CENTER), BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Refresh");
        JButton openChat = new JButton("Open Chat");
        JButton close = new JButton("Close");
        refresh.addActionListener(e -> reload());
        openChat.addActionListener(e -> openChat());
        close.addActionListener(e -> dispose());
        actions.add(refresh);
        actions.add(openChat);
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
            rows.addAll(exchangeDAO.listChatEligibleExchanges(u.getUserId()));
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

    private void openChat() {
        int idx = table.getSelectedRow();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Select an exchange first.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ExchangeRecord r = rows.get(idx);
        new ChatFrame(this, r.getExchangeId(), r.getItemName(), r.getOtherPartyUsername()).setVisible(true);
    }
}
