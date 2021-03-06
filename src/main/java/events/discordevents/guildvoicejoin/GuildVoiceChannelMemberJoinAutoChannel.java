package events.discordevents.guildvoicejoin;

import commands.runnables.utilitycategory.AutoChannelCommand;
import constants.PermissionDeprecated;
import core.ShardManager;
import core.PermissionCheckRuntime;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceJoinAbstract;
import modules.AutoChannel;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import mysql.modules.server.DBServer;
import mysql.modules.server.GuildBean;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.ServerVoiceChannelBuilder;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@DiscordEvent
public class GuildVoiceChannelMemberJoinAutoChannel extends GuildVoiceJoinAbstract {

    @Override
    public boolean onGuildVoiceJoin(ServerVoiceChannelMemberJoinEvent event) throws Throwable {
        if (userIsNotConnected(event.getChannel(), event.getUser())) return true;

        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().retrieve(event.getServer().getId());
        if (autoChannelBean.isActive() && event.getChannel().getId() == autoChannelBean.getParentChannelId().orElse(0L)) {
            GuildBean guildBean = DBServer.getInstance().retrieve(event.getServer().getId());
            if (PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), AutoChannelCommand.class, event.getServer(), PermissionDeprecated.MANAGE_CHANNELS_ON_SERVER | PermissionDeprecated.MOVE_MEMBERS | PermissionDeprecated.CONNECT) &&
                    (event.getChannel().getCategory().isEmpty() || PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), AutoChannelCommand.class, event.getChannel().getCategory().get(), PermissionDeprecated.MANAGE_CHANNELS_ON_SERVER))
            ) {
                int n = 1;

                for (int i = 0; i < 50; i++) {
                    if (!event.getServer().getChannelsByName(getNewVCName(autoChannelBean, event, n)).isEmpty()) n++;
                    else break;
                }

                if (userIsNotConnected(event.getChannel(), event.getUser()))
                    return true;

                //Create channel
                ServerVoiceChannelBuilder vcb = new ServerVoiceChannelBuilder(event.getServer())
                        .setName(getNewVCName(autoChannelBean, event, n))
                        .setBitrate(event.getChannel().getBitrate());
                if (event.getChannel().getCategory().isPresent())
                    vcb.setCategory(event.getChannel().getCategory().get());
                if (event.getChannel().getUserLimit().isPresent())
                    vcb.setUserlimit(event.getChannel().getUserLimit().get());
                if (autoChannelBean.isLocked())
                    vcb.setUserlimit(1);

                addOriginalPermissions(event.getChannel(), vcb);
                addBotPermissions(event.getChannel(), vcb);
                addCreaterPermissions(event.getUser(), vcb);

                ServerVoiceChannel vc;
                try {
                    vc = vcb.create().get();
                } catch (ExecutionException e) {
                    vcb.setName("???");
                    vc = vcb.create().get();
                }

                try {
                    event.getUser().move(vc).get();
                } catch (Throwable e) {
                    //Ignore
                    vc.delete().get();
                    return true;
                }

                autoChannelBean.getChildChannelIds().add(vc.getId());
                if (userIsNotConnected(vc, event.getUser())) {
                    vc.delete().get();
                    autoChannelBean.getChildChannelIds().remove(vc.getId());
                }
            }
        }

        return true;
    }

    private Permissions extractValidPermissions(Server server, Permissions originalPermissions) {
        PermissionsBuilder pb = new PermissionsBuilder();

        originalPermissions.getAllowedPermission().forEach(permissionType -> {
            if (BotPermissionUtil.botHasServerPermission(server, permissionType) &&
                    (BotPermissionUtil.hasAdminPermissions(server, ShardManager.getInstance().getSelf()) || permissionType != PermissionType.MANAGE_ROLES)
            ) {
                pb.setAllowed(permissionType);
            }
        });

        originalPermissions.getDeniedPermissions().forEach(permissionType -> {
            if (BotPermissionUtil.botHasServerPermission(server, permissionType) &&
                    (BotPermissionUtil.hasAdminPermissions(server, ShardManager.getInstance().getSelf()) || permissionType != PermissionType.MANAGE_ROLES)
            ) {
                pb.setDenied(permissionType);
            }
        });

        return pb.build();
    }

    private void addOriginalPermissions(ServerVoiceChannel sourceChannel, ServerVoiceChannelBuilder newChannel) {
        for (Map.Entry<Long, Permissions> entry : sourceChannel.getOverwrittenUserPermissions().entrySet()) {
            newChannel.addPermissionOverwrite(
                    sourceChannel.getServer().getMemberById(entry.getKey()).get(),
                    extractValidPermissions(sourceChannel.getServer(), entry.getValue())
            );
        }
        for (Map.Entry<Long, Permissions> entry : sourceChannel.getOverwrittenRolePermissions().entrySet()) {
            newChannel.addPermissionOverwrite(
                    sourceChannel.getServer().getRoleById(entry.getKey()).get(),
                    extractValidPermissions(sourceChannel.getServer(), entry.getValue())
            );
        }
    }

    private void addCreaterPermissions(User user, ServerVoiceChannelBuilder newChannel) {
        PermissionsBuilder pb = new PermissionsBuilder();
        pb.setState(PermissionType.MANAGE_CHANNELS, PermissionState.ALLOWED);
        newChannel.addPermissionOverwrite(user, pb.build());
    }

    private void addBotPermissions(ServerVoiceChannel sourceChannel, ServerVoiceChannelBuilder newChannel) {
        Permissions botPermission = null;
        for (Map.Entry<Long, Permissions> entry : sourceChannel.getOverwrittenUserPermissions().entrySet()) {
            if (ShardManager.getInstance().getSelf().getId() == entry.getKey()) {
                botPermission = entry.getValue();
                break;
            }
        }

        PermissionsBuilder botPermsBuilder = botPermission != null ? botPermission.toBuilder() : new PermissionsBuilder();
        Permissions permissions = botPermsBuilder
                .setState(PermissionType.MANAGE_CHANNELS, PermissionState.ALLOWED)
                .setState(PermissionType.CONNECT, PermissionState.ALLOWED)
                .build();

        newChannel.addPermissionOverwrite(ShardManager.getInstance().getSelf(), permissions);
    }

    private String getNewVCName(AutoChannelBean autoChannelBean, ServerVoiceChannelMemberJoinEvent event, int n) {
        String name = autoChannelBean.getNameMask();
        name = AutoChannel.resolveVariables(name, event.getChannel().getName(), String.valueOf(n), event.getUser().getDisplayName(event.getServer()));
        name = name.substring(0, Math.min(100, name.length()));
        return name;
    }

    private boolean userIsNotConnected(ServerVoiceChannel channel, User user) {
        Optional<ServerVoiceChannel> channelOpt = user.getConnectedVoiceChannel(channel.getServer());
        return channelOpt.isEmpty() || channelOpt.get().getId() != channel.getId();
    }

}