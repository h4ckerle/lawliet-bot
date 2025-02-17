package commands.runnables.utilitycategory;

import java.util.List;
import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.AutoChannel;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "autochannel",
        botGuildPermissions = { Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT },
        userGuildPermissions = { Permission.MANAGE_CHANNEL },
        emoji = "🔊",
        executableWithoutArgs = true,
        aliases = { "tempchannel" }
)
public class AutoChannelCommand extends NavigationAbstract {

    private AutoChannelBean autoChannelBean;

    public AutoChannelCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        autoChannelBean = DBAutoChannel.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(4);
        return true;
    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        switch (state) {
            case 1:
                List<VoiceChannel> channelList = MentionUtil.getVoiceChannels(event.getMessage(), input).getList();
                if (channelList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return Response.FALSE;
                } else {
                    VoiceChannel voiceChannel = channelList.get(0);
                    String channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), voiceChannel, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS);
                    if (channelMissingPerms != null) {
                        setLog(LogStatus.FAILURE, channelMissingPerms);
                        return Response.FALSE;
                    }

                    Category parent = voiceChannel.getParent();
                    if (parent != null) {
                        String categoryMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), parent, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL);
                        if (categoryMissingPerms != null) {
                            setLog(LogStatus.FAILURE, categoryMissingPerms);
                            return Response.FALSE;
                        }
                    }

                    autoChannelBean.setParentChannelId(voiceChannel.getIdLong());
                    setLog(LogStatus.SUCCESS, getString("channelset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 2:
                if (input.length() > 0 && input.length() < 50) {
                    autoChannelBean.setNameMask(input);
                    setLog(LogStatus.SUCCESS, getString("channelnameset"));
                    setState(0);
                    return Response.TRUE;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "args_too_long", "50"));
                    return Response.FALSE;
                }

            default:
                return null;
        }
    }

    @Override
    public boolean controllerReaction(GenericGuildMessageReactionEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        removeNavigationWithMessage();
                        return false;

                    case 0:
                        autoChannelBean.toggleActive();
                        setLog(LogStatus.SUCCESS, getString("activeset", autoChannelBean.isActive()));
                        return true;

                    case 1:
                        setState(1);
                        return true;

                    case 2:
                        setState(2);
                        return true;

                    case 3:
                        autoChannelBean.toggleLocked();
                        setLog(LogStatus.SUCCESS, getString("lockedset", autoChannelBean.isLocked()));
                        return true;

                    default:
                        return false;
                }

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }

            case 2:
                if (i == -1) {
                    setState(0);
                    return true;
                }

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(int state) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mactive"), StringUtil.getOnOffForBoolean(getLocale(), autoChannelBean.isActive()), true)
                        .addField(getString("state0_mchannel"), StringUtil.escapeMarkdown(autoChannelBean.getParentChannel().map(GuildChannel::getName).orElse(notSet)), true)
                        .addField(getString("state0_mchannelname"), AutoChannel.resolveVariables(
                                StringUtil.escapeMarkdown(autoChannelBean.getNameMask()),
                                "`%VCNAME`",
                                "`%INDEX`",
                                "`%CREATOR`"
                        ), true)
                        .addField(getString("state0_mlocked"), getString("state0_mlocked_desc", StringUtil.getOnOffForBoolean(getLocale(), autoChannelBean.isLocked())), true);

            case 1:
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            default:
                return null;
        }
    }

}
