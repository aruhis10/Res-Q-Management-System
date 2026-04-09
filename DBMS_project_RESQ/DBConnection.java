import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // URL for your local MySQL database
    private static final String URL = "jdbc:mysql://localhost:3306/resq_db";
    
    // Change these to your actual MySQL username and password
    private static final String USER = "root"; 
    private static final String PASSWORD = "vansh2823"; 

    public static Connection getConnection() throws SQLException {
        try {
            // Loads the JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC Driver not found. Did you add the JAR file to VS Code?");
        }
    }
}