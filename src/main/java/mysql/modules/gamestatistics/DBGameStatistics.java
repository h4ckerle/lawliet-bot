package mysql.modules.gamestatistics;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import core.Program;
import mysql.DBMain;
import mysql.DBObserverMapCache;

public class DBGameStatistics extends DBObserverMapCache<String, GameStatisticsBean> {

    private static final DBGameStatistics ourInstance = new DBGameStatistics();

    public static DBGameStatistics getInstance() {
        return ourInstance;
    }

    private DBGameStatistics() {
    }

    @Override
    protected GameStatisticsBean load(String command) throws Exception {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT won, value FROM GameStatistics WHERE game = ?;");
        preparedStatement.setString(1, command);
        preparedStatement.execute();

        double[] values = { 0.0, 0.0 };
        ResultSet resultSet = preparedStatement.getResultSet();
        while (resultSet.next()) {
            values[resultSet.getInt(1)] = resultSet.getDouble(2);
        }

        resultSet.close();
        preparedStatement.close();

        return new GameStatisticsBean(command, values);
    }

    @Override
    protected void save(GameStatisticsBean gameStatisticsBean) {
        if (Program.isPublicVersion()) {
            DBMain.getInstance().asyncUpdate("REPLACE INTO GameStatistics (game, won, value) VALUES (?, ?, ?), (?, ?, ?);", preparedStatement -> {
                preparedStatement.setString(1, gameStatisticsBean.getCommand());
                preparedStatement.setBoolean(2, false);
                preparedStatement.setDouble(3, gameStatisticsBean.getValue(false));
                preparedStatement.setString(4, gameStatisticsBean.getCommand());
                preparedStatement.setBoolean(5, true);
                preparedStatement.setDouble(6, gameStatisticsBean.getValue(true));
            });
        }
    }

}
