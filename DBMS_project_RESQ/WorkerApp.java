import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class WorkerApp extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainContainer;

    // --- SESSION DATA ---
    private int currentWorkerId = -1;
    private int currentHubId = -1;
    private String workerName = "";
    private int activeRequestId = -1;

    // --- UI COMPONENTS ---
    private JLabel welcomeLabel;
    private DefaultTableModel invModel, pastJobsModel, contractModel;
    private JTextArea activeJobDisplay;
    private JTextField partIdInput, qtyInput, hoursInput;

    public WorkerApp() {
        setTitle("Res-Q : Technician Field Portal");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        mainContainer.add(createLoginPanel(), "LOGIN");
        mainContainer.add(createDashboardPanel(), "DASHBOARD");

        add(mainContainer);
        cardLayout.show(mainContainer, "LOGIN");
        setVisible(true);
    }

    // ==========================================
    // SCREEN 1: SECURE LOGIN
    // ==========================================
    private JPanel createLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(44, 62, 80)); // Dark professional theme
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;

        JLabel title = new JLabel("Worker Secure Login");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        p.add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy++;
        JLabel l1 = new JLabel("Worker ID:"); l1.setForeground(Color.WHITE); p.add(l1, gbc);
        gbc.gridx = 1; JTextField idField = new JTextField(12); p.add(idField, gbc);

        gbc.gridy++; gbc.gridx = 0;
        JLabel l2 = new JLabel("Phone Number:"); l2.setForeground(Color.WHITE); p.add(l2, gbc);
        gbc.gridx = 1; JTextField phoneField = new JTextField(12); p.add(phoneField, gbc);

        gbc.gridy++; gbc.gridx = 0;
        JLabel l3 = new JLabel("Hub ID:"); l3.setForeground(Color.WHITE); p.add(l3, gbc);
        gbc.gridx = 1; JTextField hubField = new JTextField(12); p.add(hubField, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        JButton btnLogin = new JButton("Authenticate");
        btnLogin.setBackground(new Color(39, 174, 96));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.addActionListener(e -> attemptLogin(idField.getText(), phoneField.getText(), hubField.getText()));
        p.add(btnLogin, gbc);

        return p;
    }

    // ==========================================
    // SCREEN 2: WORKER DASHBOARD
    // ==========================================
    private JPanel createDashboardPanel() {
        JPanel p = new JPanel(new BorderLayout());

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(new Color(231, 76, 60)); // Red theme for workers
        welcomeLabel = new JLabel("Field Portal");
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        header.add(welcomeLabel);

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            currentWorkerId = -1; currentHubId = -1; activeRequestId = -1;
            cardLayout.show(mainContainer, "LOGIN");
        });
        header.add(btnLogout);
        p.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 14));

        // TAB 1: ACTIVE ASSIGNMENT & BILLING
        JPanel activePanel = new JPanel(new BorderLayout());
        
        activeJobDisplay = new JTextArea();
        activeJobDisplay.setEditable(false);
        activeJobDisplay.setFont(new Font("Monospaced", Font.BOLD, 14));
        activeJobDisplay.setBackground(new Color(236, 240, 241));
        activeJobDisplay.setBorder(BorderFactory.createTitledBorder("Current Dispatch Details"));
        activePanel.add(new JScrollPane(activeJobDisplay), BorderLayout.CENTER);

        // Billing & Parts Console (Bottom of Active Tab)
        JPanel billingConsole = new JPanel(new GridLayout(2, 1, 5, 5));
        billingConsole.setBorder(BorderFactory.createTitledBorder("Job Completion & Billing Console"));
        
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Part ID Used:")); partIdInput = new JTextField(5); row1.add(partIdInput);
        row1.add(new JLabel("Quantity:")); qtyInput = new JTextField(5); row1.add(qtyInput);
        JButton btnUsePart = new JButton("Consume Part from Hub");
        btnUsePart.setBackground(new Color(243, 156, 18));
        btnUsePart.setForeground(Color.WHITE);
        btnUsePart.addActionListener(e -> consumePart());
        row1.add(btnUsePart);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("Hours Worked (₹400/hr):")); hoursInput = new JTextField(5); row2.add(hoursInput);
        JButton btnBill = new JButton("Generate e-Bill for Customer");
        btnBill.setBackground(new Color(41, 128, 185));
        btnBill.setForeground(Color.WHITE);
        btnBill.addActionListener(e -> generateBill());
        row2.add(btnBill);

        billingConsole.add(row1); 
        billingConsole.add(row2);
        
        JPanel southWrap = new JPanel(new BorderLayout());
        southWrap.add(billingConsole, BorderLayout.CENTER);
        JButton refreshActive = new JButton("🔄 Check for New Assignments");
        refreshActive.addActionListener(e -> fetchActiveJob());
        southWrap.add(refreshActive, BorderLayout.SOUTH);
        
        activePanel.add(southWrap, BorderLayout.SOUTH);
        tabs.addTab("🚨 Active Assignment", activePanel);

        // TAB 2: LOCAL HUB INVENTORY
        JPanel invPanel = new JPanel(new BorderLayout());
        invModel = new DefaultTableModel();
        JTable invTable = new JTable(invModel); invTable.setRowHeight(25);
        invPanel.add(new JScrollPane(invTable), BorderLayout.CENTER);
        JButton refreshInv = new JButton("🔄 Refresh My Hub Stock");
        refreshInv.addActionListener(e -> fetchTableData(
            "SELECT p.PartID, p.Name, p.Category, h.Quantity, p.Price " +
            "FROM HUB_INVENTORY h JOIN PART p ON h.PartID = p.PartID " +
            "WHERE h.HubID = " + currentHubId, invModel));
        invPanel.add(refreshInv, BorderLayout.SOUTH);
        tabs.addTab("📦 Hub Inventory", invPanel);

        // TAB 3: PAST JOBS
        JPanel pastPanel = new JPanel(new BorderLayout());
        pastJobsModel = new DefaultTableModel();
        JTable pastTable = new JTable(pastJobsModel); pastTable.setRowHeight(25);
        pastPanel.add(new JScrollPane(pastTable), BorderLayout.CENTER);
        JButton refreshPast = new JButton("🔄 Refresh History");
        refreshPast.addActionListener(e -> fetchTableData(
            "SELECT da.RequestID, er.IssueType, da.DispatchTime, da.Status " +
            "FROM DISPATCH_ASSIGNMENT da JOIN EMERGENCY_REQUEST er ON da.RequestID = er.RequestID " +
            "WHERE da.WorkerID = " + currentWorkerId + " AND da.Status = 'JobDone'", pastJobsModel));
        pastPanel.add(refreshPast, BorderLayout.SOUTH);
        tabs.addTab("📜 Past Jobs", pastPanel);

        // TAB 4: CONTRACT INFO
        JPanel contractPanel = new JPanel(new BorderLayout());
        contractModel = new DefaultTableModel();
        JTable cTable = new JTable(contractModel); cTable.setRowHeight(25);
        contractPanel.add(new JScrollPane(cTable), BorderLayout.CENTER);
        JButton refreshContract = new JButton("🔄 View Contract Details");
        refreshContract.addActionListener(e -> fetchTableData(
            "SELECT VehicleReg, VehicleModel, CleaningDutyClause FROM CONTRACT WHERE WorkerID = " + currentWorkerId, contractModel));
        contractPanel.add(refreshContract, BorderLayout.SOUTH);
        tabs.addTab("📑 My Contract", contractPanel);

        p.add(tabs, BorderLayout.CENTER);
        return p;
    }

    // ==========================================
    // LOGIC & DATABASE EXECUTION
    // ==========================================

    private void attemptLogin(String id, String phone, String hub) {
        if (id.isEmpty() || phone.isEmpty() || hub.isEmpty()) return;
        
        String sql = "SELECT Name FROM WORKER WHERE WorkerID=? AND Contact_Number=? AND HubID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(id));
            pstmt.setString(2, phone);
            pstmt.setInt(3, Integer.parseInt(hub));
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                currentWorkerId = Integer.parseInt(id);
                currentHubId = Integer.parseInt(hub);
                workerName = rs.getString("Name");
                
                welcomeLabel.setText(" Technician: " + workerName + " | Assigned Hub: " + currentHubId);
                fetchActiveJob(); // Load active assignment on login
                cardLayout.show(mainContainer, "DASHBOARD");
            } else {
                JOptionPane.showMessageDialog(this, "Authentication Failed. Please check your credentials.");
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage()); }
    }

private void fetchActiveJob() {
        // Updated SQL to grab er.SpecificLocation
        String sql = "SELECT da.RequestID, er.IssueType, er.Category, er.SpecificLocation, u.Name, u.Phone, u.Latitude, u.Longitude " +
                     "FROM DISPATCH_ASSIGNMENT da " +
                     "JOIN EMERGENCY_REQUEST er ON da.RequestID = er.RequestID " +
                     "JOIN USER u ON er.UserID = u.UserID " +
                     "WHERE da.WorkerID = ? AND da.Status = 'EnRoute' LIMIT 1";
                     
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentWorkerId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                activeRequestId = rs.getInt("RequestID");
                activeJobDisplay.setText(
                    "\n  ======================================================\n" +
                    "   🚨 CURRENT EMERGENCY DISPATCH\n" +
                    "  ======================================================\n\n" +
                    "   Request ID : " + activeRequestId + "\n" +
                    "   Category   : " + rs.getString("Category") + "\n" +
                    "   Issue      : " + rs.getString("IssueType") + "\n" +
                    "   Exact Loc  : " + rs.getString("SpecificLocation") + "\n\n" +  // <--- NEW INFO ADDED HERE
                    "  ------------------------------------------------------\n" +
                    "   👤 CUSTOMER DETAILS\n" +
                    "  ------------------------------------------------------\n" +
                    "   Name       : " + rs.getString("Name") + "\n" +
                    "   Phone      : " + rs.getString("Phone") + "\n" +
                    "   GPS Target : " + rs.getString("Latitude") + ", " + rs.getString("Longitude") + "\n\n" +
                    "  ======================================================"
                );
            } else {
                activeRequestId = -1;
                activeJobDisplay.setText("\n\n\n        ✅ No active assignments. You are currently on Standby.");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void consumePart() {
        if (activeRequestId == -1) { 
            JOptionPane.showMessageDialog(this, "You have no active job to use parts for!"); 
            return; 
        }
        if (partIdInput.getText().isEmpty() || qtyInput.getText().isEmpty()) return;

        String sql = "UPDATE HUB_INVENTORY SET Quantity = Quantity - ? WHERE PartID = ? AND HubID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(qtyInput.getText()));
            pstmt.setInt(2, Integer.parseInt(partIdInput.getText()));
            pstmt.setInt(3, currentHubId); // Ensures they only take from THEIR hub
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Inventory Updated! Parts deducted from Hub " + currentHubId);
            } else {
                JOptionPane.showMessageDialog(this, "Failed. Check Part ID or verify sufficient stock in your Hub.");
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }

    private void generateBill() {
        if (activeRequestId == -1) { 
            JOptionPane.showMessageDialog(this, "You have no active job to bill!"); 
            return; 
        }
        if (hoursInput.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Hours Worked to calculate labor costs.");
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            double totalCost = 0;
            
            // 1. Calculate Part Cost (if a part was logged)
            if (!partIdInput.getText().isEmpty() && !qtyInput.getText().isEmpty()) {
                String costSql = "SELECT Price FROM PART WHERE PartID = ?";
                try (PreparedStatement p1 = conn.prepareStatement(costSql)) {
                    p1.setInt(1, Integer.parseInt(partIdInput.getText()));
                    ResultSet rs = p1.executeQuery();
                    if (rs.next()) {
                        totalCost += (rs.getDouble("Price") * Integer.parseInt(qtyInput.getText()));
                    }
                }
            }
            
            // 2. Add Labor Cost (Hours * 400)
            int hours = Integer.parseInt(hoursInput.getText());
            totalCost += (hours * 400);

            // 3. Create 'Pending' Payment for the User
            String paySql = "INSERT INTO PAYMENT (RequestID, Amount, TransactionStatus) VALUES (?, ?, 'Pending')";
            try (PreparedStatement p2 = conn.prepareStatement(paySql)) {
                p2.setInt(1, activeRequestId);
                p2.setDouble(2, totalCost);
                p2.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "e-Bill generated successfully!\nTotal Amount: ₹" + totalCost + "\nWaiting for Customer to make payment in their portal.");
            
            // Clear inputs
            partIdInput.setText(""); qtyInput.setText(""); hoursInput.setText("");
            
        } catch (Exception e) { 
            JOptionPane.showMessageDialog(this, "Billing Error: " + e.getMessage()); 
        }
    }

    private void fetchTableData(String sql, DefaultTableModel model) {
        if (currentWorkerId == -1) return;
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
             
            ResultSetMetaData meta = rs.getMetaData();
            model.setRowCount(0); model.setColumnCount(0);
            
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                model.addColumn(meta.getColumnLabel(i));
            }
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.add(rs.getObject(i));
                }
                model.addRow(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WorkerApp::new);
    }
}