package hu.finominfo.scheduler.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class KeyValueStore {
    private static final String DB_URL = "jdbc:h2:./keyvaluestore";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private Connection connection;

    public KeyValueStore() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createDatabase() {
        try {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS KeyValueStore ("
                    + "name VARCHAR, "
                    + "year INT, "
                    + "month INT, "
                    + "type VARCHAR, "
                    + "value INT, "
                    + "PRIMARY KEY (name, year, month, type))");
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void writeData(String name, int year, int month, String type, int value) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "MERGE INTO KeyValueStore (name, year, month, type, value) VALUES (?, ?, ?, ?, ?)");
            preparedStatement.setString(1, name);
            preparedStatement.setInt(2, year);
            preparedStatement.setInt(3, month);
            preparedStatement.setString(4, type);
            preparedStatement.setInt(5, value);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Integer> readData(String name, int year, int month, String type) {
        Map<String, Integer> data = new HashMap<>();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT name, year, month, type, value FROM KeyValueStore WHERE name = ? AND year = ? AND month = ? AND type = ?");
            preparedStatement.setString(1, name);
            preparedStatement.setInt(2, year);
            preparedStatement.setInt(3, month);
            preparedStatement.setString(4, type);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int value = resultSet.getInt("value");
                data.put(type, value);
            }
            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public void printAll() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * from KeyValueStore");
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                System.out.print(resultSet.getString(1));
                System.out.print(" ");
                System.out.print(resultSet.getInt(2));
                System.out.print(" ");
                System.out.print(resultSet.getInt(3));
                System.out.print(" ");
                System.out.print(resultSet.getString(4));
                System.out.print(" ");
                System.out.println(resultSet.getInt(5));
            }
            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws SQLException {
        KeyValueStore keyValueStore = new KeyValueStore();
        keyValueStore.createDatabase();

        keyValueStore.writeData("Békési Tamás", 2023, 10, "WE", 17);
        keyValueStore.writeData("Békési Tamás", 2023, 10, "NH", 2);
        keyValueStore.writeData("Békési Tamás", 2023, 10, "ALL", 55);

        keyValueStore.writeData("Fekete József", 2023, 10, "WE", 18);
        keyValueStore.writeData("Fekete József", 2023, 10, "NH", 1);
        keyValueStore.writeData("Fekete József", 2023, 10, "ALL", 62);

        keyValueStore.writeData("Granc Róbert", 2023, 10, "WE", 14);
        keyValueStore.writeData("Granc Róbert", 2023, 10, "NH", 1);
        keyValueStore.writeData("Granc Róbert", 2023, 10, "ALL", 56);

        keyValueStore.writeData("Hossam Youssef", 2023, 10, "WE", 12);
        keyValueStore.writeData("Hossam Youssef", 2023, 10, "NH", 0);
        keyValueStore.writeData("Hossam Youssef", 2023, 10, "ALL", 36);

        keyValueStore.writeData("Islam Sayed", 2023, 10, "WE", 10);
        keyValueStore.writeData("Islam Sayed", 2023, 10, "NH", 2);
        keyValueStore.writeData("Islam Sayed", 2023, 10, "ALL", 36);

        keyValueStore.writeData("Maher Adly Maher", 2023, 10, "WE", 10);
        keyValueStore.writeData("Maher Adly Maher", 2023, 10, "NH", 1);
        keyValueStore.writeData("Maher Adly Maher", 2023, 10, "ALL", 39);

        keyValueStore.writeData("Nagy Zoltán", 2023, 10, "WE", 16);
        keyValueStore.writeData("Nagy Zoltán", 2023, 10, "NH", 0);
        keyValueStore.writeData("Nagy Zoltán", 2023, 10, "ALL", 54);

        keyValueStore.writeData("Pintér Dávid", 2023, 10, "WE", 14);
        keyValueStore.writeData("Pintér Dávid", 2023, 10, "NH", 2);
        keyValueStore.writeData("Pintér Dávid", 2023, 10, "ALL", 56);

        keyValueStore.writeData("Ritter Gergő", 2023, 10, "WE", 16);
        keyValueStore.writeData("Ritter Gergő", 2023, 10, "NH", 0);
        keyValueStore.writeData("Ritter Gergő", 2023, 10, "ALL", 55);

        keyValueStore.writeData("Tima Gergely", 2023, 10, "WE", 23);
        keyValueStore.writeData("Tima Gergely", 2023, 10, "NH", 0);
        keyValueStore.writeData("Tima Gergely", 2023, 10, "ALL", 61);

        keyValueStore.writeData("Tóth Sándor", 2023, 10, "WE", 8);
        keyValueStore.writeData("Tóth Sándor", 2023, 10, "NH", 2);
        keyValueStore.writeData("Tóth Sándor", 2023, 10, "ALL", 37);

        keyValueStore.connection.commit();

        keyValueStore.printAll();

        keyValueStore.connection.close();

        /*
        Map<String, Integer> johnData = keyValueStore.readData("John", 2023, 10, "A");
        for (Map.Entry<String, Integer> entry : johnData.entrySet()) {
            System.out.println("Type: " + entry.getKey() + ", Value: " + entry.getValue());
        }
         */
    }
}
