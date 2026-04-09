import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class ResQDashboard extends JFrame {

    // --- UI COMPONENTS ---
    private JTabbedPane tabbedPane;
    private JLabel statusLabel;

    // Models
    private DefaultTableModel manualDispatchModel;
    private DefaultTableModel workerModel, inventoryModel;
    private DefaultTableModel requestModel, dispatchModel, userModel, contractModel, paymentModel;

    // Inputs for Add Worker
    private JTextField wName, wContact, wSalary, wHubId;
    private JComboBox<String> wVerif, wAvail, wSpec;

    // Inputs for Manual Dispatch
    private JTextField mdReqId, mdWorkerId;

    // Filters
    private JComboBox<String> hubFilterWorker, hubFilterInv;
    private JComboBox<String> reqStatusFilter, catFilter;

    public ResQDashboard() {
        setTitle("Res-Q Enterprise ERP - Admin Command Center");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        statusLabel = new JLabel(" Admin System Online | Connected to ResQ_DB");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel.setFont(new Font("Monospaced", Font.BOLD, 12));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("  RES-Q MASTER ADMIN CONSOLE");
        title.setOpaque(true);
        title.setBackground(new Color(41, 128, 185));
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        header.add(title, BorderLayout.NORTH);

        // --- TABS ---
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));

        tabbedPane.addTab("🚨 Manual Dispatch ('Both' Category)", createManualDispatchTab());
        tabbedPane.addTab("👥 Worker Management", createWorkerTab());
        tabbedPane.addTab("📦 Hub Inventory", createInventoryTab());
        tabbedPane.addTab("📊 Master Logs (Requests & Dispatch)", createMasterLogsTab());
        tabbedPane.addTab("🗺️ Users, Contracts & Finance", createMiscDataTab());

        add(header, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        setVisible(true);
    }

    // ==========================================
    // TAB 1: MANUAL DISPATCH ('BOTH' CATEGORY)
    // ==========================================
    private JPanel createManualDispatchTab() {
        JPanel p = new JPanel(new BorderLayout());
        
        // Table showing only 'Both' and 'Pending'
        manualDispatchModel = new DefaultTableModel();
        JTable table = new JTable(manualDispatchModel);
        table.setRowHeight(25);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        // Control Panel
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        controls.setBorder(BorderFactory.createTitledBorder("Assign Standby Worker to Complex Issue"));
        
        controls.add(new JLabel("Request ID:"));
        mdReqId = new JTextField(5);
        controls.add(mdReqId);
        
        controls.add(new JLabel("Standby Worker ID:"));
        mdWorkerId = new JTextField(5);
        controls.add(mdWorkerId);
        
        JButton btnAssign = new JButton("Assign & Dispatch");
        btnAssign.setBackground(new Color(231, 76, 60));
        btnAssign.setForeground(Color.WHITE);
        btnAssign.addActionListener(e -> handleManualDispatch());
        controls.add(btnAssign);

        JButton btnRefresh = new JButton("🔄 Refresh List");
        btnRefresh.addActionListener(e -> refreshManualDispatch());
        controls.add(btnRefresh);

        p.add(controls, BorderLayout.SOUTH);
        refreshManualDispatch();
        return p;
    }

    // ==========================================
    // TAB 2: WORKER MANAGEMENT
    // ==========================================
    private JPanel createWorkerTab() {
        JPanel p = new JPanel(new BorderLayout());

        // TOP: Add Worker Form
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        addPanel.setBorder(BorderFactory.createTitledBorder("Hire / Register New Worker"));
        
        addPanel.add(new JLabel("Name:")); wName = new JTextField(8); addPanel.add(wName);
        addPanel.add(new JLabel("Phone:")); wContact = new JTextField(8); addPanel.add(wContact);
        addPanel.add(new JLabel("Salary:")); wSalary = new JTextField(6); addPanel.add(wSalary);
        addPanel.add(new JLabel("HubID:")); wHubId = new JTextField(3); addPanel.add(wHubId);
        
        wVerif = new JComboBox<>(new String[]{"Verified", "Pending", "Rejected"});
        addPanel.add(new JLabel("Status:")); addPanel.add(wVerif);
        
        wAvail = new JComboBox<>(new String[]{"Off-Shift", "Standby", "Busy"});
        addPanel.add(new JLabel("Avail:")); addPanel.add(wAvail);
        
        wSpec = new JComboBox<>(new String[]{"Electrical", "Plumbing"});
        addPanel.add(new JLabel("Spec:")); addPanel.add(wSpec);

        JButton btnAdd = new JButton("Add Worker");
        btnAdd.addActionListener(e -> handleAddWorker());
        addPanel.add(btnAdd);
        p.add(addPanel, BorderLayout.NORTH);

        // CENTER: Worker Table
        workerModel = new DefaultTableModel();
        p.add(new JScrollPane(new JTable(workerModel)), BorderLayout.CENTER);

        // BOTTOM: Filter by Hub
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter by Hub ID:"));
        hubFilterWorker = new JComboBox<>(new String[]{"All", "1", "2", "3"});
        hubFilterWorker.addActionListener(e -> refreshWorkers());
        filterPanel.add(hubFilterWorker);
        
        JButton btnRef = new JButton("🔄 Refresh");
        btnRef.addActionListener(e -> refreshWorkers());
        filterPanel.add(btnRef);
        p.add(filterPanel, BorderLayout.SOUTH);

        refreshWorkers();
        return p;
    }

    // ==========================================
    // TAB 3: HUB INVENTORY
    // ==========================================
    private JPanel createInventoryTab() {
        JPanel p = new JPanel(new BorderLayout());
        inventoryModel = new DefaultTableModel();
        p.add(new JScrollPane(new JTable(inventoryModel)), BorderLayout.CENTER);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Select Hub to View Inventory:"));
        hubFilterInv = new JComboBox<>(new String[]{"All", "1", "2", "3"});
        hubFilterInv.addActionListener(e -> refreshInventory());
        filterPanel.add(hubFilterInv);

        p.add(filterPanel, BorderLayout.NORTH);
        refreshInventory();
        return p;
    }

    // ==========================================
    // TAB 4: MASTER LOGS (Filtered Requests/Dispatch)
    // ==========================================
    private JPanel createMasterLogsTab() {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 10));

        // Top: Emergency Requests
        JPanel reqPanel = new JPanel(new BorderLayout());
        reqPanel.setBorder(BorderFactory.createTitledBorder("Emergency Requests"));
        requestModel = new DefaultTableModel();
        reqPanel.add(new JScrollPane(new JTable(requestModel)), BorderLayout.CENTER);
        
        JPanel rFilter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rFilter.add(new JLabel("Filter Status:"));
        reqStatusFilter = new JComboBox<>(new String[]{"All", "Pending", "EnRoute", "Completed", "Cancelled"});
        reqStatusFilter.addActionListener(e -> refreshRequests());
        rFilter.add(reqStatusFilter);
        reqPanel.add(rFilter, BorderLayout.NORTH);

        // Bottom: Dispatch & Payments
        JPanel dispPanel = new JPanel(new BorderLayout());
        dispPanel.setBorder(BorderFactory.createTitledBorder("Dispatch Assignments & Linked Payments"));
        dispatchModel = new DefaultTableModel();
        dispPanel.add(new JScrollPane(new JTable(dispatchModel)), BorderLayout.CENTER);

        JPanel dFilter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dFilter.add(new JLabel("Filter by Category:"));
        catFilter = new JComboBox<>(new String[]{"All", "Plumbing", "Electrical", "Both"});
        catFilter.addActionListener(e -> refreshDispatch());
        dFilter.add(catFilter);
        dispPanel.add(dFilter, BorderLayout.NORTH);

        p.add(reqPanel);
        p.add(dispPanel);

        refreshRequests();
        refreshDispatch();
        return p;
    }

    // ==========================================
    // TAB 5: USERS, CONTRACTS & FINANCE
    // ==========================================
    private JPanel createMiscDataTab() {
        JPanel p = new JPanel(new GridLayout(3, 1, 0, 10));

        userModel = new DefaultTableModel();
        JPanel up = new JPanel(new BorderLayout()); up.setBorder(BorderFactory.createTitledBorder("All Users"));
        up.add(new JScrollPane(new JTable(userModel)), BorderLayout.CENTER);
        
        contractModel = new DefaultTableModel();
        JPanel cp = new JPanel(new BorderLayout()); cp.setBorder(BorderFactory.createTitledBorder("Worker Contracts"));
        cp.add(new JScrollPane(new JTable(contractModel)), BorderLayout.CENTER);
        
        paymentModel = new DefaultTableModel();
        JPanel pp = new JPanel(new BorderLayout()); pp.setBorder(BorderFactory.createTitledBorder("Master Finance Log"));
        pp.add(new JScrollPane(new JTable(paymentModel)), BorderLayout.CENTER);

        p.add(up); p.add(cp); p.add(pp);

        fetchTableData("SELECT * FROM USER", userModel);
        fetchTableData("SELECT * FROM CONTRACT", contractModel);
        fetchTableData("SELECT * FROM PAYMENT", paymentModel);

        return p;
    }

    // ==========================================
    // LOGIC & DATABASE EXECUTION
    // ==========================================

    private void handleManualDispatch() {
        if (mdReqId.getText().isEmpty() || mdWorkerId.getText().isEmpty()) return;
        
        String sqlDisp = "INSERT INTO DISPATCH_ASSIGNMENT (RequestID, WorkerID, Status) VALUES (?, ?, 'EnRoute')";
        String sqlReq = "UPDATE EMERGENCY_REQUEST SET Status = 'EnRoute' WHERE RequestID = ?";
        
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement p1 = conn.prepareStatement(sqlDisp);
                 PreparedStatement p2 = conn.prepareStatement(sqlReq)) {
                
                int rId = Integer.parseInt(mdReqId.getText());
                p1.setInt(1, rId);
                p1.setInt(2, Integer.parseInt(mdWorkerId.getText()));
                p2.setInt(1, rId);
                
                p1.executeUpdate();
                p2.executeUpdate();
                conn.commit();
                
                statusLabel.setText("✅ Complex Job Dispatched Manually!");
                mdReqId.setText(""); mdWorkerId.setText("");
                refreshManualDispatch();
                refreshRequests();
                refreshDispatch();
            } catch (Exception ex) { conn.rollback(); throw ex; }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Dispatch Error: " + e.getMessage()); }
    }

    private void handleAddWorker() {
        String sql = "INSERT INTO WORKER (Name, Contact_Number, Salary, VerificationStatus, AvailabilityStatus, HubID, Pro_Speciality) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, wName.getText());
            pstmt.setString(2, wContact.getText());
            pstmt.setDouble(3, Double.parseDouble(wSalary.getText()));
            pstmt.setString(4, (String) wVerif.getSelectedItem());
            pstmt.setString(5, (String) wAvail.getSelectedItem());
            pstmt.setInt(6, Integer.parseInt(wHubId.getText()));
            pstmt.setString(7, (String) wSpec.getSelectedItem());
            
            pstmt.executeUpdate();
            statusLabel.setText("✅ New Worker Registered to DB.");
            
            // Clear inputs
            wName.setText(""); wContact.setText(""); wSalary.setText(""); wHubId.setText("");
            refreshWorkers();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Registration Error: " + e.getMessage()); }
    }

    // --- REFRESH METHODS (With Filters) ---

    private void refreshManualDispatch() {
        String sql = "SELECT er.RequestID, er.IssueType, er.Timestamp, u.Name AS UserName, mh.HubID AS Target_Hub " +
                     "FROM EMERGENCY_REQUEST er " +
                     "JOIN USER u ON er.UserID = u.UserID " +
                     "JOIN MICRO_HUB mh ON u.Polygon_ID = mh.Polygon_ID " +
                     "WHERE er.Category = 'Both' AND er.Status = 'Pending'";
        fetchTableData(sql, manualDispatchModel);
    }

    private void refreshWorkers() {
        String hub = (String) hubFilterWorker.getSelectedItem();
        String sql = "SELECT WorkerID, Name, Contact_Number, Salary, VerificationStatus, AvailabilityStatus, Pro_Speciality, HubID FROM WORKER";
        if (!hub.equals("All")) sql += " WHERE HubID = " + hub;
        fetchTableData(sql, workerModel);
    }

    private void refreshInventory() {
        String hub = (String) hubFilterInv.getSelectedItem();
        String sql = "SELECT h.HubID, p.ZoneName, i.PartID, pr.Name, pr.Category, i.Quantity " +
                     "FROM HUB_INVENTORY i " +
                     "JOIN MICRO_HUB h ON i.HubID = h.HubID " +
                     "JOIN POLYGON p ON h.Polygon_ID = p.PolygonID " +
                     "JOIN PART pr ON i.PartID = pr.PartID";
        if (!hub.equals("All")) sql += " WHERE h.HubID = " + hub;
        fetchTableData(sql, inventoryModel);
    }

    private void refreshRequests() {
        String stat = (String) reqStatusFilter.getSelectedItem();
        String sql = "SELECT * FROM EMERGENCY_REQUEST";
        if (!stat.equals("All")) sql += " WHERE Status = '" + stat + "'";
        sql += " ORDER BY Timestamp DESC";
        fetchTableData(sql, requestModel);
    }

    private void refreshDispatch() {
        String cat = (String) catFilter.getSelectedItem();
        String sql = "SELECT da.AssignmentID, da.RequestID, er.Category, da.WorkerID, w.Name AS WorkerName, da.Status, p.Amount, p.TransactionStatus " +
                     "FROM DISPATCH_ASSIGNMENT da " +
                     "JOIN EMERGENCY_REQUEST er ON da.RequestID = er.RequestID " +
                     "JOIN WORKER w ON da.WorkerID = w.WorkerID " +
                     "LEFT JOIN PAYMENT p ON da.RequestID = p.RequestID";
        if (!cat.equals("All")) sql += " WHERE er.Category = '" + cat + "'";
        fetchTableData(sql, dispatchModel);
    }

    // --- CORE FETCH METHOD ---
    private void fetchTableData(String sql, DefaultTableModel model) {
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
                    Object val = rs.getObject(i);
                    row.add(val == null ? "N/A" : val);
                }
                model.addRow(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ResQDashboard::new);
    }
}