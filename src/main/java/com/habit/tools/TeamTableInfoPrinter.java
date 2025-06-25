package com.habit.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TeamTableInfoPrinter {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:habit.db";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(teams)");
            System.out.println("teamsテーブルのカラム一覧:");
            while (rs.next()) {
                System.out.println(
                    "cid: " + rs.getInt("cid") +
                    ", name: " + rs.getString("name") +
                    ", type: " + rs.getString("type")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
