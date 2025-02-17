package modules.repair;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import commands.Command;
import commands.runnables.utilitycategory.AutoChannelCommand;
import core.PermissionCheckRuntime;
import core.ShardManager;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;

public class AutoChannelRepair {

    private static final AutoChannelRepair ourInstance = new AutoChannelRepair();

    public static AutoChannelRepair getInstance() {
        return ourInstance;
    }

    private AutoChannelRepair() {
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void start(JDA jda) {
        executorService.submit(() -> run(jda));
    }

    public void run(JDA jda) {
        DBAutoChannel.getInstance().retrieveAllChildChannelServerIds().stream()
                .filter(serverId -> ShardManager.getInstance().getResponsibleShard(serverId) == jda.getShardInfo().getShardId())
                .map(jda::getGuildById)
                .filter(Objects::nonNull)
                .forEach(this::deleteEmptyVoiceChannels);
    }

    private void deleteEmptyVoiceChannels(Guild guild) {
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().retrieve(guild.getIdLong());
        Locale locale = autoChannelBean.getGuildBean().getLocale();
        autoChannelBean.getChildChannelIds().transform(guild::getVoiceChannelById, ISnowflake::getIdLong).stream()
                .filter(vc -> vc.getMembers().isEmpty() && PermissionCheckRuntime.getInstance().botHasPermission(autoChannelBean.getGuildBean().getLocale(), AutoChannelCommand.class, vc, Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT))
                .forEach(vc -> vc.delete().reason(Command.getCommandLanguage(AutoChannelCommand.class, locale).getTitle()).queue());
    }

}
