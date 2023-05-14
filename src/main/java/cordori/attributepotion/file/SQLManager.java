package cordori.attributepotion.file;

import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SQLManager {
    public static boolean MySQL = false;
    public static SQLManager sql;
    @Getter private final BasicDataSource dataSource = new BasicDataSource();

    @SneakyThrows
    public SQLManager(String url, String username, String password, String driver) {
        Class.forName(driver);
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);

        if(MySQL) {
            dataSource.setUsername(username);
            dataSource.setPassword(password);
        }
    }

    @SneakyThrows
    public Connection getConnection() {
        return dataSource.getConnection();
    }

    @SneakyThrows
    public void disconnect() {
        getConnection().close();
    }

    @SneakyThrows
    public void createTable() {
        @Cleanup Connection conn = getConnection();
        @Cleanup Statement statement = conn.createStatement();
        String query = "CREATE TABLE IF NOT EXISTS playerdata ("
                + "uuid VARCHAR(255), "
                + "potionKey VARCHAR(255), "
                + "attrList VARCHAR(255), "
                + "potionGroup VARCHAR(255), "
                + "useTime BIGINT)";
        statement.executeUpdate(query);
    }


    @SneakyThrows
    public void insert(String uuid, String potionKey, List<String> attrList, Long useTime, String group) {
        String query = "INSERT INTO playerdata (uuid, potionKey, attrList, potionGroup, useTime) VALUES (?, ?, ?, ?, ?)";
        @Cleanup Connection conn = getConnection();
        @Cleanup PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, uuid);
        stmt.setString(2, potionKey);
        stmt.setString(3, String.join(",", attrList));
        stmt.setString(4, group);
        stmt.setLong(5, useTime);
        stmt.executeUpdate();
    }

    @SneakyThrows
    public HashMap<String, List<Object>> getPotionData(String uuid) {

        HashMap<String, List<Object>> potionData = new HashMap<>();
        String query = "SELECT potionKey, attrList, potionGroup, useTime FROM playerdata WHERE uuid = ?";

        @Cleanup Connection conn = getConnection();
        @Cleanup PreparedStatement selectStmt = conn.prepareStatement(query);

        selectStmt.setString(1, uuid);

        @Cleanup ResultSet rs = selectStmt.executeQuery();

        while (rs.next()) {
            List<Object> objectList = new ArrayList<>();
            String potionKey = rs.getString("potionKey");
            String str = rs.getString("attrList");
            String potionGroup = rs.getString("potionGroup");
            long useTime = rs.getLong("useTime");

            objectList.add(str);
            objectList.add(potionGroup);
            objectList.add(useTime);
            potionData.put(potionKey, objectList);
        }

        return potionData;
    }


    @SneakyThrows
    public void delete(String uuid, String potionKey) {

        String query = "DELETE FROM playerdata WHERE uuid = ? AND potionKey = ?";

        @Cleanup Connection conn = getConnection();
        @Cleanup PreparedStatement stmt = conn.prepareStatement(query);

        stmt.setString(1, uuid);
        stmt.setString(2, potionKey);
        stmt.executeUpdate();
    }

}

