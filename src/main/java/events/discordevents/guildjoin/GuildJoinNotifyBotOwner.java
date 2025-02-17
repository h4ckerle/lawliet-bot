package events.discordevents.guildjoin;

import constants.AssetIds;
import core.MainLogger;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildJoinAbstract;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;

@DiscordEvent
public class GuildJoinNotifyBotOwner extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(GuildJoinEvent event) {
        if (event.getGuild().getMemberCount() >= 5000) {
            JDAUtil.sendPrivateMessage(
                    AssetIds.OWNER_USER_ID,
                    "**+++** " + StringUtil.escapeMarkdown(event.getGuild().getName()) + " (" + event.getGuild().getMemberCount() + ")"
            ).queue();
        }

        MainLogger.get().info("+++ {} ({})", event.getGuild().getName(), event.getGuild().getMemberCount());
        return true;
    }

}
