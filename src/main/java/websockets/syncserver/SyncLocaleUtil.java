package websockets.syncserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import commands.Command;
import constants.AssetIds;
import constants.Locales;
import core.ListGen;
import core.TextManager;
import net.dv8tion.jda.api.Permission;
import org.json.JSONObject;

public class SyncLocaleUtil {

    private SyncLocaleUtil() {
    }

    public static JSONObject getLanguagePack(String category, String key) {
        JSONObject jsonObject = new JSONObject();

        for (String localeString : Locales.LIST) {
            Locale locale = new Locale(localeString);
            jsonObject.put(locale.getDisplayName(), TextManager.getString(locale, category, key).replace("%PREFIX", "L."));
        }

        return jsonObject;
    }

    public static JSONObject getCommandPermissions(Command command) {
        JSONObject jsonObject = new JSONObject();

        for (String localeString : Locales.LIST) {
            Locale locale = new Locale(localeString);
            ArrayList<Permission> permissionList = new ArrayList<>(Arrays.asList(command.getCommandProperties().userGuildPermissions()));
            permissionList.addAll(Arrays.asList(command.getCommandProperties().userChannelPermissions()));

            String permissionsList = new ListGen<Permission>().getList(
                    permissionList, "", ListGen.SLOT_TYPE_NONE,
                    permission -> TextManager.getString(locale, TextManager.PERMISSIONS, permission.name())
            );
            jsonObject.put(locale.getDisplayName(), permissionsList);
        }

        return jsonObject;
    }

    public static JSONObject getCommandSpecs(String commandCategory, String key, String commandTrigger) {
        JSONObject jsonObject = new JSONObject();

        for (String localeString : Locales.LIST) {
            Locale locale = new Locale(localeString);
            String str = solveVariablesOfCommandText(TextManager.getString(locale, commandCategory, key));
            if (!str.isEmpty()) {
                str = ("\n" + str).replace("\n", "\nL." + commandTrigger + " ").substring(1);
            }

            jsonObject.put(locale.getDisplayName(), str);
        }

        return jsonObject;
    }

    private static String solveVariablesOfCommandText(String string) {
        return string
                .replaceAll("(?i)%MessageContent", "hi")
                .replaceAll("(?i)%#Channel", "#welcome")
                .replaceAll("(?i)%MessageID", "557961653975515168")
                .replaceAll("(?i)%ChannelID", "557953262305804310")
                .replaceAll("(?i)%ServerID", String.valueOf(AssetIds.SUPPORT_SERVER_ID))
                .replaceAll("(?i)%@User", "@Aninoss#7220")
                .replaceAll("(?i)%@Bot", "@Lawliet#5480")
                .replaceAll("(?i)%Prefix", "L.");
    }

}
