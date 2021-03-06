package events.discordevents.guildmessagereceived;

import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.OnMessageInputListener;
import commands.listeners.OnNavigationListenerOld;
import commands.runnables.gimmickscategory.QuoteCommand;
import commands.runnables.informationcategory.HelpCommand;
import core.MainLogger;
import core.utils.ExceptionUtil;
import core.ShardManager;
import core.utils.MentionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import mysql.modules.autoquote.DBAutoQuote;
import mysql.modules.server.DBServer;
import mysql.modules.server.GuildBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedCommand extends GuildMessageReceivedAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        GuildBean guildBean = DBServer.getInstance().retrieve(event.getServer().get().getId());
        String prefix = guildBean.getPrefix();
        String content = event.getMessage().getContent();

        if (content.toLowerCase().startsWith("i.") && prefix.equalsIgnoreCase("L."))
            content = prefix + content.substring(2);

        String[] prefixes = {
                prefix,
                ShardManager.getInstance().getSelf().getMentionTag(),
                "<@!" + ShardManager.getInstance().getSelf().getIdAsString() + ">"
        };

        int prefixFound = -1;
        for (int i = 0; i < prefixes.length; i++) {
            if (prefixes[i] != null && content.toLowerCase().startsWith(prefixes[i].toLowerCase())) {
                prefixFound = i;
                break;
            }
        }

        if (prefixFound > -1) {
            if (prefixFound > 0 && manageForwardedMessages(event)) return true;

            String newContent = content.substring(prefixes[prefixFound].length()).trim();
            if (newContent.contains("  ")) newContent = newContent.replace("  ", " ");
            String commandTrigger = newContent.split(" ")[0].toLowerCase();
            if (newContent.contains("<") && newContent.split("<")[0].length() < commandTrigger.length())
                commandTrigger = newContent.split("<")[0].toLowerCase();

            String followedString;
            try {
                followedString = newContent.substring(commandTrigger.length()).trim();
            } catch (StringIndexOutOfBoundsException e) {
                followedString = "";
            }

            if (commandTrigger.length() > 0) {
                Locale locale = guildBean.getLocale();
                Class<? extends Command> clazz;
                clazz = CommandContainer.getInstance().getCommandMap().get(commandTrigger);
                if (clazz != null) {
                    Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
                    if (!command.isExecutableWithoutArgs() && followedString.isEmpty()) {
                        followedString = command.getTrigger();
                        command = CommandManager.createCommandByClass(HelpCommand.class, locale, prefix);
                        command.getAttachments().put("noargs", true);
                    }

                    try {
                        CommandManager.manage(event, command, followedString, getStartTime());
                    } catch (Throwable e) {
                        ExceptionUtil.handleCommandException(e, command, event.getServerTextChannel().get());
                    }
                }
            }
        } else {
            if (manageForwardedMessages(event)) return true;
            checkAutoQuote(event);
        }

        return true;
    }

    private void checkAutoQuote(MessageCreateEvent event) throws ExecutionException {
        if (event.getChannel().canYouWrite() && event.getChannel().canYouEmbedLinks()) {
            GuildBean guildBean = DBServer.getInstance().retrieve(event.getServer().get().getId());
            Locale locale = guildBean.getLocale();
            ArrayList<Message> messages = MentionUtil.getMessageWithLinks(event.getMessage(), event.getMessage().getContent()).getList();
            if (messages.size() > 0 && DBAutoQuote.getInstance().retrieve(event.getServer().get().getId()).isActive()) {
                try {
                    for (int i = 0; i < Math.min(3, messages.size()); i++) {
                        Message message = messages.get(i);
                        QuoteCommand quoteCommand = new QuoteCommand(locale, guildBean.getPrefix());
                        quoteCommand.postEmbed(event.getServerTextChannel().get(), message, true);
                    }
                } catch (Throwable throwable) {
                    MainLogger.get().error("Exception in Auto Quote", throwable);
                }
            }
        }
    }

    private boolean manageForwardedMessages(MessageCreateEvent event) {
        ArrayList<Command> list = CommandContainer.getInstance().getMessageForwardInstances();
        for (int i = list.size() - 1; i >= 0; i--) {
            Command command = list.get(i);
            if (command != null &&
                    (event.getChannel().getId() == command.getForwardChannelID() || command.getForwardChannelID() == -1) &&
                    (event.getMessage().getUserAuthor().get().getId() == command.getForwardUserID() || command.getForwardUserID() == -1)
            ) {
                try {
                    if (command instanceof OnMessageInputListener) {
                        boolean end = command.onForwardedRecievedSuper(event);
                        if (end) return true;
                    }
                    if (command instanceof OnNavigationListenerOld) {
                        boolean end = command.onNavigationMessageSuper(event, event.getMessage().getContent(), false);
                        if (end) return true;
                    }
                } catch (Throwable e) {
                    ExceptionUtil.handleCommandException(e, command, event.getServerTextChannel().get());
                }
            }
        }

        return false;
    }

}