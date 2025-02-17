package events.discordevents.guildmessagereactionremove;

import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.OnStaticReactionRemoveListener;
import constants.Emojis;
import core.CustomObservableMap;
import core.MainLogger;
import core.ShardManager;
import core.cache.MessageCache;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageReactionRemoveAbstract;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildBean;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

@DiscordEvent
public class GuildMessageReactionRemoveCommandsStatic extends GuildMessageReactionRemoveAbstract {

    @Override
    public boolean onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        if (!BotPermissionUtil.canReadHistory(event.getChannel())) {
            return true;
        }

        Message message;
        try {
            message = MessageCache.getInstance().retrieveMessage(event.getChannel(), event.getMessageIdLong()).get();
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
            return true;
        }

        boolean valid = DBStaticReactionMessages.getInstance().retrieve().containsKey(event.getMessageIdLong());
        if ((valid || message.getAuthor().getIdLong() == ShardManager.getInstance().getSelfId()) &&
                message.getEmbeds().size() > 0
        ) {
            GuildBean guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
            MessageEmbed embed = message.getEmbeds().get(0);
            if (embed.getTitle() != null && embed.getAuthor() == null) {
                String title = embed.getTitle();
                for (Class<? extends OnStaticReactionRemoveListener> clazz : CommandContainer.getInstance().getStaticReactionRemoveCommands()) {
                    Command command = CommandManager.createCommandByClass((Class<? extends Command>) clazz, guildBean.getLocale(), guildBean.getPrefix());
                    if (title.toLowerCase().startsWith(((OnStaticReactionRemoveListener) command).titleStartIndicator().toLowerCase()) && (valid || title.endsWith(Emojis.EMPTY_EMOJI))) {
                        try {
                            CustomObservableMap<Long, StaticReactionMessageData> map = DBStaticReactionMessages.getInstance().retrieve();
                            if (!map.containsKey(event.getMessageIdLong())) {
                                map.put(event.getMessageIdLong(), new StaticReactionMessageData(message, command.getTrigger()));
                            }

                            ((OnStaticReactionRemoveListener) command).onStaticReactionRemove(message, event);
                        } catch (Throwable throwable) {
                            MainLogger.get().error("Static reaction add exception", throwable);
                        }
                    }
                }
            }
        }

        return true;
    }

}
