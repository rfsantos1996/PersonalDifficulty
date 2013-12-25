package com.jabyftw.pd;

import java.sql.*;
import java.util.logging.Level;

public class MySQL {

    private final PDifficulty pl;
    private final String user, pass, url;
    public Connection conn = null;

    public MySQL(PDifficulty pl, String username, String password, String url) {
        this.pl = pl;
        this.user = username;
        this.pass = password;
        this.url = url;
    }

    public Connection getConn() {
        if (conn != null) {
            return conn;
        }
        try {
            conn = DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            pl.getLogger().log(Level.WARNING, "Couldn''t connect to MySQL: {0}", e.getMessage());
        }
        return conn;
    }

    public void closeConn() {
        if (conn != null) {
            try {
                conn.close();
                conn = null;
            } catch (SQLException ex) {
                pl.getLogger().log(Level.WARNING, "Couldn''t connect to MySQL: {0}", ex.getMessage());
            }
        }
    }

    public void createTable() {
        pl.getServer().getScheduler().scheduleAsyncDelayedTask(pl, new Runnable() {

            @Override
            public void run() {
                try {
                    getConn().createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS `pdifficulty` (\n"
                            + "  `name` VARCHAR(24) NOT NULL,\n"
                            + "  `difficulty` INT NOT NULL,\n"
                            + "  PRIMARY KEY (`difficulty`),\n"
                            + "  UNIQUE INDEX `name_UNIQUE` (`name` ASC));");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        pl.getServer().getScheduler().scheduleAsyncDelayedTask(pl, new Runnable() {

            @Override
            public void run() {
                try {
                    getConn().createStatement().executeUpdate("DELETE FROM `pdifficulty` WHERE `difficulty`=" + pl.defaultDif + ";");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void insertPlayer(String name, int dif) {
        final String name2 = name;
        final int dif2 = dif;
        pl.getServer().getScheduler().scheduleAsyncDelayedTask(pl, new Runnable() {

            @Override
            public void run() {
                try {
                    getConn().createStatement().execute("INSERT INTO `pdifficulty` (`name`, `difficulty`) VALUES ('" + name2 + "', " + dif2 + ");");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void updateDifficulty(String name, int dif) {
        final String name2 = name;
        final int dif2 = dif;
        pl.getServer().getScheduler().scheduleAsyncDelayedTask(pl, new Runnable() {

            @Override
            public void run() {
                try {
                    getConn().createStatement().execute("UPDATE `pdifficulty` SET `difficulty`=" + dif2 + " WHERE `name`='" + name2 + "';");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public int getDifficulty(String name) {
        try {
            ResultSet rs = getConn().createStatement().executeQuery("SELECT `difficulty` FROM `pdifficulty` WHERE `name`='" + name + "';");
            while (rs.next()) {
                return rs.getInt("difficulty");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pl.defaultDif;
    }
}
