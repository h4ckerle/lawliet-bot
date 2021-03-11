package modules.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.utilitycategory.GiveawayCommand;
import constants.Emojis;
import core.*;
import core.schedule.MainScheduler;
import core.utils.JDAEmojiUtil;
import core.utils.StringUtil;
import mysql.modules.giveaway.DBGiveaway;
import mysql.modules.giveaway.GiveawayBean;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class GiveawayScheduler {

    private static final GiveawayScheduler ourInstance = new GiveawayScheduler();

    public static GiveawayScheduler getInstance() {
        return ourInstance;
    }

    private GiveawayScheduler() {
    }

    private boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        try {
            DBGiveaway.getInstance().retrieve().values().stream()
                    .filter(GiveawayBean::isActive)
                    .forEach(this::loadGiveawayBean);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start giveaway", e);
        }
    }

    public void loadGiveawayBean(GiveawayBean giveawayBean) {
        MainScheduler.getInstance().schedule(giveawayBean.getEnd(), "giveaway", () -> onGiveawayDue(giveawayBean));
    }

    private void onGiveawayDue(GiveawayBean giveawayBean) {
        if (giveawayBean.isActive()) {
            ShardManager.getInstance().getLocalGuildById(giveawayBean.getGuildId())
                    .map(guild -> guild.getTextChannelById(giveawayBean.getTextChannelId()))
                    .ifPresent(channel -> {
                        try {
                            processGiveawayUsers(channel, DBGuild.getInstance().retrieve(channel.getGuild().getIdLong()), giveawayBean);
                        } catch (Throwable e) {
                            MainLogger.get().error("Error in giveaway", e);
                        }
                    });
        }
    }

    private void processGiveawayUsers(TextChannel channel, GuildBean guildBean, GiveawayBean giveawayBean) {
        giveawayBean.retrieveMessage()
                .thenAccept(message -> {
                    for (MessageReaction reaction : message.getReactions()) {
                        if (JDAEmojiUtil.reactionEmoteEqualsEmoji(reaction.getReactionEmote(), giveawayBean.getEmoji())) {
                            reaction.retrieveUsers().queue(users ->
                                    processGiveaway(channel, guildBean, giveawayBean, message, new ArrayList<>(users))
                            );
                            break;
                        }
                    }
                });
    }

    private void processGiveaway(TextChannel channel, GuildBean guildBean, GiveawayBean giveawayBean, Message message, ArrayList<User> users) {
        users.removeIf(user -> user.isBot() || !channel.getGuild().isMember(user));
        Collections.shuffle(users);
        List<User> winners = users.subList(0, Math.min(users.size(), giveawayBean.getWinners()));

        StringBuilder mentions = new StringBuilder();
        for (User user : winners) {
            mentions.append(user.getAsMention()).append(" ");
        }

        CommandProperties commandProps = Command.getCommandProperties(GiveawayCommand.class);
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(commandProps.emoji() + " " + giveawayBean.getTitle())
                .setDescription(TextManager.getString(guildBean.getLocale(), "utility", "giveaway_results", winners.size() != 1));
        giveawayBean.getImageUrl().ifPresent(eb::setImage);
        if (winners.size() > 0) {
            eb.addField(
                    Emojis.EMPTY_EMOJI,
                    new ListGen<User>().getList(winners, ListGen.SLOT_TYPE_BULLET, user -> "**" + StringUtil.escapeMarkdown(user.getAsTag()) + "**"),
                    false
            );
        } else {
            eb.setDescription(TextManager.getString(guildBean.getLocale(), "utility", "giveaway_results_empty"));
        }

        giveawayBean.stop();
        message.editMessage(mentions.toString())
                .embed(eb.build())
                .queue();
        if (PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), GiveawayCommand.class, channel, Permission.MESSAGE_WRITE)) {
            channel.sendMessage(mentions.toString()).flatMap(Message::delete).queue();
        }
    }

}
