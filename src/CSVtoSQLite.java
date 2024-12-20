import java.sql.*;
import java.io.*;

public class CSVtoSQLite {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlite::memory:";
        String[] csvFiles = {
                "Pathway.csv",
                "Pathwat.csv",
                "Pathway.csv"
        };

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            for (int i = 0; i < csvFiles.length; i++) {
                String tableName = "table" + (i + 1);
                createTableFromCSV(conn, csvFiles[i], tableName);
            }

            String query = """
                SELECT normalized_name, COUNT(*) AS table_count
                FROM (
                    SELECT DISTINCT LOWER(TRIM("Name (original name)")) AS normalized_name FROM table3
                    UNION ALL
                    SELECT DISTINCT LOWER(TRIM("Name (original name)")) AS normalized_name FROM table2
                    UNION ALL
                    SELECT DISTINCT LOWER(TRIM("Name (original name)")) AS normalized_name FROM table1
                ) combined
                GROUP BY normalized_name
                HAVING table_count = 3;
            """;

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            System.out.println("3 Toplantıya da Katılan Kişiler:");
            int count = 0;
            while (rs.next()) {
                String name = rs.getString("normalized_name");
                int tableCount = rs.getInt("table_count");

                // Yazdırma işlemiyle doğrulama
                System.out.println(name + " | Table Count: " + tableCount);

                // Sadece table_count == 3 ise say
                if (tableCount == 3) count++;
            }
            System.out.println("Doğru Toplam Kişi Sayısı: " + count);

            System.out.println("Toplam Kişi Sayısı: " + count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createTableFromCSV(Connection conn, String filePath, String tableName) throws Exception {
        String createTableSQL = String.format("""
            CREATE TABLE %s (
                "Name (original name)" TEXT,
                "Email" TEXT,
                "Join time" TEXT,
                "Leave time" TEXT,
                "Duration (minutes)" INTEGER,
                "Guest" TEXT,
                "In waiting room" TEXT
            );
        """, tableName);

        Statement stmt = conn.createStatement();
        stmt.execute(createTableSQL);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            String insertSQL = String.format("INSERT INTO %s VALUES (?, ?, ?, ?, ?, ?, ?)", tableName);
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);

            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                for (int i = 0; i < data.length; i++) {
                    pstmt.setString(i + 1, normalize(data[i]));
                }
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private static String normalize(String input) {
        if (input == null) return null;
        return input.trim().toLowerCase()
                .replace("ç", "c")
                .replace("ğ", "g")
                .replace("ı", "i")
                .replace("İ", "i")
                .replace("ö", "o")
                .replace("ş", "s")
                .replace("ü", "u")
                .replaceAll("\\s+", " ");
    }
}
