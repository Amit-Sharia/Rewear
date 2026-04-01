package com.rewear.ui;

import com.rewear.Session;
import com.rewear.dao.ChatDAO;
import com.rewear.models.ChatMessage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatFrame extends JFrame {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final int exchangeId;
    private final ChatDAO chatDAO = new ChatDAO();
    private final JTextArea transcript = new JTextArea();
    private final JTextField input = new JTextField();
    private final Timer refreshTimer;

    public ChatFrame(JFrame parent, int exchangeId, String itemName, String otherParty) {
        super("Chat - Exchange #" + exchangeId);
        this.exchangeId = exchangeId;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(700, 480);
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(new JLabel("Item: " + itemName + " | Chat with: " + otherParty, SwingConstants.LEFT), BorderLayout.NORTH);

        transcript.setEditable(false);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);
        root.add(new JScrollPane(transcript), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.add(input, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton send = new JButton("Send");
        JButton refresh = new JButton("Refresh");
        send.addActionListener(e -> sendMessage());
        refresh.addActionListener(e -> reload());
        actions.add(refresh);
        actions.add(send);
        bottom.add(actions, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
        refreshTimer = new Timer(3500, e -> reload());
        refreshTimer.start();
        reload();
    }

    @Override
    public void dispose() {
        refreshTimer.stop();
        super.dispose();
    }

    private void reload() {
        var u = Session.getCurrentUser();
        if (u == null) {
            return;
        }
        try {
            List<ChatMessage> messages = chatDAO.listMessages(exchangeId, u.getUserId());
            StringBuilder sb = new StringBuilder();
            for (ChatMessage m : messages) {
                String when = m.getSentAt() != null ? FMT.format(m.getSentAt()) : "";
                sb.append("[").append(when).append("] ")
                        .append(m.getSenderUsername()).append(": ")
                        .append(m.getMessageText()).append("\n");
            }
            transcript.setText(sb.toString());
            transcript.setCaretPosition(transcript.getDocument().getLength());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        var u = Session.getCurrentUser();
        if (u == null) {
            return;
        }
        String text = input.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        try {
            chatDAO.sendMessage(exchangeId, u.getUserId(), text);
            input.setText("");
            reload();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
