package events.discordevents.guildmemberjoin;

import java.util.Locale;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.MemberCountDisplay;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent(allowBannedUser = true, allowBots = true)
public class GuildMemberJoinMCDisplays extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        Locale locale = DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).getLocale();
        MemberCountDisplay.getInstance().manage(locale, event.getGuild());
        return true;
    }

}
