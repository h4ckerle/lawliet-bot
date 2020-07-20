package DiscordEvents.ServerMemberLeave;

import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberLeaveAbstract;
import Modules.MemberCountDisplay;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;

import java.util.Locale;

@DiscordEventAnnotation(allowBannedUser = true)
public class ServerMemberLeaveMCDisplays extends ServerMemberLeaveAbstract {

    @Override
    public boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        MemberCountDisplay.manage(locale, server);
        return true;
    }

}