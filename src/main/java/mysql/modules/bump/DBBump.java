package mysql.modules.bump;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import mysql.DBMain;

public class DBBump {

    public static void setNextBump(Instant instant) throws SQLException {
        DBMain.getInstance().asyncUpdate(
                "UPDATE Bump SET next = ?;",
                preparedStatement -> preparedStatement.setString(1, DBMain.instantToDateTimeString(instant))
        );
    }

    public static Instant getNextBump() throws SQLException {
        Instant instant = null;

        String sql = "SELECT next FROM Bump;";
        Statement statement = DBMain.getInstance().statementExecuted(sql);
        ResultSet resultSet = statement.getResultSet();

        if (resultSet.next()) {
            instant = resultSet.getTimestamp(1).toInstant();
        }

        resultSet.close();
        statement.close();

        return instant;
    }

}
