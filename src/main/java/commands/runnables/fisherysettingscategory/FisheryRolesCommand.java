package commands.runnables.fisherysettingscategory;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import constants.Response;
import constants.Settings;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildBean;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "fisheryroles",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "📜",
        executableWithoutArgs = true,
        aliases = { "fishingroles", "fishroles", "fisheryr" }
)
public class FisheryRolesCommand extends NavigationAbstract {

    private static final int MAX_ROLES = 50;

    private GuildBean guildBean;
    private FisheryGuildBean fisheryGuildBean;

    public FisheryRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        fisheryGuildBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong());

        checkRolesWithLog(event.getGuild(), fisheryGuildBean.getRoles());
        registerNavigationListener(12);
        return true;
    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        switch (state) {
            case 1:
                List<Role> roleList = MentionUtil.getRoles(event.getMessage(), input).getList();
                if (roleList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return Response.FALSE;
                } else {
                    if (!checkRolesWithLog(event.getMember().getGuild(), event.getMember(), roleList)) {
                        return Response.FALSE;
                    }

                    CustomObservableList<Role> roles = fisheryGuildBean.getRoles();
                    int existingRoles = 0;
                    for (Role role : roleList) {
                        if (roles.contains(role)) {
                            existingRoles++;
                        }
                    }

                    if (existingRoles >= roleList.size()) {
                        setLog(LogStatus.FAILURE, getString("roleexists", roleList.size() != 1));
                        return Response.FALSE;
                    }

                    int rolesAdded = 0;
                    for (Role role : roleList) {
                        if (!roles.contains(role)) {
                            roles.add(role);
                            rolesAdded++;
                            roles.sort(Comparator.comparingInt(Role::getPosition));
                        }
                    }

                    setLog(LogStatus.SUCCESS, getString("roleadd", (rolesAdded - existingRoles) != 1, String.valueOf(rolesAdded)));
                    setState(0);
                    return Response.TRUE;
                }

            case 3:
                List<TextChannel> channelList = MentionUtil.getTextChannels(event.getMessage(), input).getList();
                if (channelList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return Response.FALSE;
                } else {
                    TextChannel channel = channelList.get(0);
                    if (checkWriteInChannelWithLog(channel)) {
                        guildBean.setFisheryAnnouncementChannelId(channel.getIdLong());
                        setLog(LogStatus.SUCCESS, getString("announcementchannelset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }

            case 4:
                if (input.contains("-") && !input.replaceFirst("-", "").contains("-")) {
                    String[] parts = (input + " ").split("-");
                    long priceMin = MentionUtil.getAmountExt(parts[0]);
                    long priceMax = MentionUtil.getAmountExt(parts[1]);

                    if (priceMin >= -1 && priceMax >= -1 && priceMin <= Settings.FISHERY_MAX && priceMax <= Settings.FISHERY_MAX) {
                        if (priceMin == -1) priceMin = guildBean.getFisheryRoleMin();
                        if (priceMax == -1) priceMax = guildBean.getFisheryRoleMax();
                        guildBean.setFisheryRolePrices(priceMin, priceMax);
                        setLog(LogStatus.SUCCESS, getString("pricesset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number", "0", StringUtil.numToString(Settings.FISHERY_MAX)));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, getString("prices_wrongvalues"));
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
                        if (fisheryGuildBean.getRoles().size() < MAX_ROLES) {
                            setState(1);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("toomanyroles", String.valueOf(MAX_ROLES)));
                            return true;
                        }

                    case 1:
                        if (fisheryGuildBean.getRoles().size() > 0) {
                            setState(2);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("norolesset"));
                            return true;
                        }

                    case 2:
                        guildBean.toggleFisherySingleRoles();
                        setLog(LogStatus.SUCCESS, getString("singleroleset", guildBean.isFisherySingleRoles()));
                        return true;

                    case 3:
                        setState(3);
                        return true;

                    case 4:
                        setState(4);
                        return true;

                    default:
                        return false;
                }

            case 1:

            case 4:
                if (i == -1) {
                    setState(0);
                    return true;
                }
                return false;

            case 2:
                CustomObservableList<Role> roles = fisheryGuildBean.getRoles();
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i < roles.size()) {
                    roles.remove(i);
                    setLog(LogStatus.SUCCESS, getString("roleremove"));
                    if (roles.size() == 0) setState(0);
                    return true;
                }
                return false;

            case 3:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i == 0) {
                    guildBean.setFisheryAnnouncementChannelId(null);
                    setState(0);
                    setLog(LogStatus.SUCCESS, getString("announcementchannelset"));
                    return true;
                }
                return false;

            default:
                return false;
        }
    }

    private String getRoleString(Role role) {
        int n = fisheryGuildBean.getRoles().indexOf(role);
        return getString(
                "state0_rolestring",
                role.getAsMention(), StringUtil.numToString(Fishery.getFisheryRolePrice(role.getGuild(), fisheryGuildBean.getRoles().size(), n))
        );
    }

    @Override
    public EmbedBuilder draw(int state) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));

                return EmbedFactory.getEmbedDefault(this, getString("state0_description", String.valueOf(MAX_ROLES)))
                        .addField(getString("state0_mroles"), new ListGen<Role>().getList(fisheryGuildBean.getRoles(), getLocale(), this::getRoleString), false)
                        .addField(getString("state0_msinglerole", StringUtil.getOnOffForBoolean(getLocale(), guildBean.isFisherySingleRoles())), getString("state0_msinglerole_desc"), false)
                        .addField(getString("state0_mannouncementchannel"), guildBean.getFisheryAnnouncementChannel().map(IMentionable::getAsMention).orElse(notSet), true)
                        .addField(getString("state0_mroleprices"), getString("state0_mroleprices_desc", StringUtil.numToString(guildBean.getFisheryRoleMin()), StringUtil.numToString(guildBean.getFisheryRoleMax())), true);

            case 1:
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                CustomObservableList<Role> roles = fisheryGuildBean.getRoles();
                String[] roleStrings = new String[roles.size()];
                for (int i = 0; i < roleStrings.length; i++) {
                    roleStrings[i] = getRoleString(roles.get(i));
                }
                setOptions(roleStrings);

                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            case 3:
                setOptions(new String[] { getString("state3_options") });
                return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title"));

            case 4:
                return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));

            default:
                return null;
        }
    }

}
