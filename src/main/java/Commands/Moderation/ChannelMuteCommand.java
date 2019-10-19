package Commands.Moderation;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import General.*;
import General.Mention.Mention;
import General.Mention.MentionFinder;
import General.Mute.MuteData;
import General.Mute.MuteManager;
import MySQL.DBServer;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "chmute",
        userPermissions = Permission.MANAGE_PERMISSIONS_IN_CHANNEL,
        botPermissions = Permission.MANAGE_PERMISSIONS_IN_CHANNEL,
        thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat/128/stop-icon.png",
        emoji = "\uD83D\uDED1",
        executable = false,
        aliases = {"channelmute", "mute"}
)
public class ChannelMuteCommand extends Command implements onRecievedListener  {

    private boolean mute;

    public ChannelMuteCommand() {
        this.mute = true;
    }

    public ChannelMuteCommand(boolean mute) {
        this.mute = mute;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        Message message = event.getMessage();
        Server server = message.getServer().get();

        ServerTextChannel channel = message.getServerTextChannel().get();
        List<ServerTextChannel> channelList = MentionFinder.getTextChannels(message, followedString).getList();
        if (channelList.size() > 0)
            channel = channelList.get(0);

        EmbedBuilder errorEmbed = PermissionCheck.userAndBothavePermissions(getLocale(), server, channel, message.getUserAuthor().get(), getUserPermissions(), getBotPermissions());
        if (errorEmbed != null) {
            message.getChannel().sendMessage(errorEmbed).get();
            return false;
        }

        List<User> userList = MentionFinder.getUsers(message, followedString).getList();
        if (userList.size() == 0) {
            message.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL,"no_mentions"))).get();
            return false;
        }

        ArrayList<User> successfulUsers = new ArrayList<>();
        for(User user: userList) {
            if (!Tools.userHasAdminPermissions(server, user)) successfulUsers.add(user);
        }

        if (successfulUsers.size() == 0) {
            message.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL,"admin_block"))).get();
            return false;
        }

        MuteData muteData = new MuteData(server, channel, successfulUsers);
        boolean doneSomething = MuteManager.getInstance().executeMute(muteData, mute);

        Mention mention = Tools.getMentionedStringOfUsers(getLocale(), channel.getServer(), userList);
        EmbedBuilder actionEmbed = EmbedFactory.getCommandEmbedStandard(this, getString("action", mention.isMultiple(), mention.getString(), message.getUserAuthor().get().getMentionTag(), channel.getMentionTag()));
        for(User user: userList) {
            try {
                if (!user.isYourself() && !user.isBot()) user.sendMessage(actionEmbed).get();
            } catch (InterruptedException | ExecutionException e) {
                //Ignore
            }
        }

        ModerationStatus moderationStatus = DBServer.getModerationFromServer(event.getServer().get());

        if (moderationStatus.getChannel() != null &&
                PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getTrigger(), moderationStatus.getChannel(), Permission.WRITE_IN_TEXT_CHANNEL | Permission.EMBED_LINKS_IN_TEXT_CHANNELS) &&
                doneSomething
        ) {
            moderationStatus.getChannel().sendMessage(actionEmbed).get();
        }

        if (!mute || !successfulUsers.contains(event.getApi().getYourself()) || channel.getId() != event.getServerTextChannel().get().getId()) {
            EmbedBuilder eb;

            if (doneSomething)
                eb = EmbedFactory.getCommandEmbedSuccess(this, getString("success_description", mention.isMultiple(), mention.getString(), channel.getMentionTag()));
            else
                eb = EmbedFactory.getCommandEmbedError(this, getString("nothingdone", mention.isMultiple(), mention.getString(), channel.getMentionTag()));

            event.getChannel().sendMessage(eb).get();
        }

        return true;
    }

}