package mysql.modules.version;

import java.util.ArrayList;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleCache;

public class DBVersion extends DBSingleCache<VersionBean> {

    private static final DBVersion ourInstance = new DBVersion();

    public static DBVersion getInstance() {
        return ourInstance;
    }

    private DBVersion() {
    }

    @Override
    protected VersionBean loadBean() {
        ArrayList<VersionBeanSlot> slots = new DBDataLoad<VersionBeanSlot>("Version", "version, date", "1 ORDER BY date")
                .getArrayList(
                        resultSet -> new VersionBeanSlot(
                                resultSet.getString(1),
                                resultSet.getTimestamp(2).toInstant()
                        )
                );

        VersionBean versionBean = new VersionBean(slots);
        versionBean.getSlots().addListAddListener(list -> list.forEach(this::insertVersion));

        return versionBean;
    }

    protected void insertVersion(VersionBeanSlot versionBeanSlot) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO Version (version, date) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setString(1, versionBeanSlot.getVersion());
            preparedStatement.setString(2, DBMain.instantToDateTimeString(versionBeanSlot.getDate()));
        });
    }

}
