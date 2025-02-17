package commands.runnables.moderationcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import core.utils.BotPermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

@CommandProperties(
        trigger = "kick",
        botGuildPermissions = Permission.KICK_MEMBERS,
        userGuildPermissions = Permission.KICK_MEMBERS,
        emoji = "\uD83D\uDEAA",
        executableWithoutArgs = false
)
public class KickCommand extends WarnCommand {

    public KickCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false, false);
    }

    @Override
    protected void process(Guild guild, User target, String reason) {
        guild.kick(target.getId(), reason)
                .submit()
                .exceptionally(e -> {
                    guild.kick(target.getId()).queue();
                    return null;
                });
    }

    @Override
    protected boolean canProcess(Member executor, User target) {
        return BotPermissionUtil.canInteract(executor.getGuild(), target);
    }

}
