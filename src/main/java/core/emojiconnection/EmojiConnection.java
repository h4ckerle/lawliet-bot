package core.emojiconnection;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import constants.Emojis;
import core.utils.EmojiUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;

public class EmojiConnection {

    private final String emoji;
    private final String connection;

    public EmojiConnection(String emoji, String connection) {
        this.connection = connection;
        this.emoji = emoji;
    }

    @Nonnull
    @CheckReturnValue
    public RestAction<Void> addReaction(Message message) {
        return message.addReaction(EmojiUtil.emojiAsReactionTag(emoji));
    }

    public boolean isEmoji(MessageReaction.ReactionEmote reactionEmote) {
        return this.emoji.equals(EmojiUtil.reactionEmoteAsMention(reactionEmote));
    }

    public String getConnection() {
        return connection;
    }

    public String getEmojiTag() {
        return emoji;
    }

    public static EmojiConnection[] getEmojiConnectionArray(TextChannel channel, boolean withBackButton, String... connections) {
        EmojiConnection[] array = new EmojiConnection[connections.length];
        int index = 0;
        for (int i = 0; i < array.length; i++) {
            if (withBackButton) {
                array[i] = new BackEmojiConnection(channel, connections[i]);
                withBackButton = false;
            } else {
                array[i] = new EmojiConnection(Emojis.LETTERS[index], connections[i]);
                index++;
            }
        }

        return array;
    }

    public static String getOptionsString(TextChannel channel, boolean withBackButton, int pageSize, String... connections) {
        EmojiConnection[] emojiConnections = getEmojiConnectionArray(channel, withBackButton, connections);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < emojiConnections.length; i++) {
            EmojiConnection emojiConnection = emojiConnections[i];

            if (pageSize != -1 && i == pageSize) sb.append("\n");
            sb.append(emojiConnection.getEmojiTag());
            sb.append(" | ");
            sb.append(emojiConnection.getConnection());
            sb.append("\n");
        }

        String str = sb.toString();

        return str.substring(0, str.length() - 1);
    }

}
