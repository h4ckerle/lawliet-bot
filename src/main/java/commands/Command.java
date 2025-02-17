package commands;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import commands.listeners.*;
import constants.LogStatus;
import core.Program;
import core.TextManager;
import core.atomicassets.AtomicGuild;
import core.atomicassets.AtomicMember;
import core.atomicassets.AtomicTextChannel;
import core.schedule.MainScheduler;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.EmojiUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.json.JSONObject;

public abstract class Command implements OnTriggerListener {

    private final long id = System.nanoTime();
    private final String category;
    private final String prefix;
    private Locale locale;
    private final CommandProperties commandProperties;
    private final JSONObject attachments = new JSONObject();
    private boolean loadingReactionSet = false;
    private final ArrayList<Runnable> completedListeners = new ArrayList<>();
    private AtomicBoolean isProcessing;
    private AtomicGuild atomicGuild;
    private AtomicTextChannel atomicTextChannel;
    private AtomicMember atomicMember;
    private long drawMessageId = 0;
    private LogStatus logStatus = null;
    private String log = "";
    private GuildMessageReceivedEvent event = null;
    private boolean canHaveTimeOut = true;

    public Command(Locale locale, String prefix) {
        this.locale = locale;
        this.prefix = prefix;
        commandProperties = this.getClass().getAnnotation(CommandProperties.class);
        category = CategoryCalculator.getCategoryByCommand(this.getClass());
    }

    public void addLoadingReaction(Message message, AtomicBoolean isProcessing) {
        this.isProcessing = isProcessing;
        MainScheduler.getInstance().schedule(
                3, ChronoUnit.SECONDS,
                getTrigger() + "_idle",
                () -> addLoadingReactionInstantly(message, isProcessing)
        );
    }

    public void addLoadingReactionInstantly() {
        if (isProcessing != null) {
            addLoadingReactionInstantly(event.getMessage(), isProcessing);
        }
    }

    public void addLoadingReactionInstantly(Message message, AtomicBoolean isProcessing) {
        TextChannel channel = message.getTextChannel();
        if (isProcessing.get() &&
                !loadingReactionSet && BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_ADD_REACTION) &&
                !getCommandProperties().turnOffLoadingReaction()
        ) {
            loadingReactionSet = true;

            String reaction = EmojiUtil.getLoadingEmojiTag(message.getTextChannel());
            message.addReaction(reaction).queue();
            MainScheduler.getInstance().poll(100, getTrigger() + "_loading", () -> {
                if (isProcessing.get()) {
                    return true;
                } else {
                    message.removeReaction(reaction).queue();
                    loadingReactionSet = false;
                    return false;
                }
            });
        }
    }

    public synchronized CompletableFuture<Long> drawMessage(EmbedBuilder eb) {
        EmbedUtil.addLog(eb, logStatus, log);

        CompletableFuture<Long> future = new CompletableFuture<>();
        getTextChannel().ifPresentOrElse(channel -> {
            if (BotPermissionUtil.canWriteEmbed(channel)) {
                if (drawMessageId == 0) {
                    channel.sendMessage(eb.build())
                            .queue(message -> {
                                drawMessageId = message.getIdLong();
                                future.complete(drawMessageId);
                            }, future::completeExceptionally);
                } else {
                    channel.editMessageById(drawMessageId, eb.build())
                            .queue(v -> future.complete(drawMessageId), future::completeExceptionally);
                }
            } else {
                future.completeExceptionally(new PermissionException("Missing permissions"));
            }
        }, () -> future.completeExceptionally(new NoSuchElementException("No such text channel")));

        resetLog();
        return future;
    }

    public void resetDrawMessage() {
        drawMessageId = 0;
    }

    public void setDrawMessageId(long drawMessageId) {
        this.drawMessageId = drawMessageId;
    }

    public Optional<Long> getDrawMessageId() {
        return drawMessageId == 0 ? Optional.empty() : Optional.of(drawMessageId);
    }

    public LogStatus getLogStatus() {
        return logStatus;
    }

    public String getLog() {
        return log;
    }

    public void setLog(LogStatus logStatus, String string) {
        this.log = string;
        this.logStatus = logStatus;
    }

    public void resetLog() {
        this.log = "";
        this.logStatus = null;
    }

    public void deregisterListeners() {
        CommandContainer.getInstance().deregisterListener(OnReactionListener.class, this);
        CommandContainer.getInstance().deregisterListener(OnMessageInputListener.class, this);
    }

    public synchronized void onListenerTimeOutSuper() throws Throwable {
        if (canHaveTimeOut) {
            canHaveTimeOut = false;
            onListenerTimeOut();
        }
    }

    protected void onListenerTimeOut() throws Throwable {
    }

    public String getString(String key, String... args) {
        String text = TextManager.getString(locale, category, commandProperties.trigger() + "_" + key, args);
        if (prefix != null) text = text.replace("%PREFIX", prefix);
        return text;
    }

    public String getString(String key, int option, String... args) {
        String text = TextManager.getString(locale, category, commandProperties.trigger() + "_" + key, option, args);
        if (prefix != null) text = text.replace("%PREFIX", prefix);
        return text;
    }

    public String getString(String key, boolean secondOption, String... args) {
        String text = TextManager.getString(locale, category, commandProperties.trigger() + "_" + key, secondOption, args);
        if (prefix != null) text = text.replace("%PREFIX", prefix);
        return text;
    }

    public CommandLanguage getCommandLanguage() {
        String title = getString("title");
        String descLong = getString("helptext");
        String usage = getString("usage");
        String examples = getString("examples");
        return new CommandLanguage(title, descLong, usage, examples);
    }

    public Permission[] getAdjustedUserGuildPermissions() {
        return commandProperties.userGuildPermissions();
    }

    public Permission[] getAdjustedUserChannelPermissions() {
        Permission[] permissions = commandProperties.userChannelPermissions();
        return processUserPermissions(permissions);
    }

    public Permission[] getUserPermissions() {
        List<Permission> permissionList = new ArrayList<>(Arrays.asList(getCommandProperties().userGuildPermissions()));
        permissionList.addAll(Arrays.asList(getCommandProperties().userChannelPermissions()));
        return permissionList.toArray(new Permission[0]);
    }

    private Permission[] processUserPermissions(Permission[] permissions) {
        if (Arrays.stream(permissions).anyMatch(permission -> permission == Permission.ADMINISTRATOR)) {
            return new Permission[] { Permission.ADMINISTRATOR };
        }

        if ((this instanceof OnReactionListener || this instanceof OnStaticReactionAddListener || this instanceof OnStaticReactionRemoveListener) &&
                Arrays.stream(permissions).noneMatch(permission -> permission == Permission.MESSAGE_HISTORY)
        ) {
            permissions = Arrays.copyOf(permissions, permissions.length + 1);
            permissions[permissions.length - 1] = Permission.MESSAGE_HISTORY;
        }

        return permissions;
    }

    public boolean isModCommand() {
        return Arrays.stream(commandProperties.userGuildPermissions()).anyMatch(p -> p != Permission.MESSAGE_HISTORY) ||
                Arrays.stream(commandProperties.userChannelPermissions()).anyMatch(p -> p != Permission.MESSAGE_HISTORY);
    }

    public Permission[] getAdjustedBotGuildPermissions() {
        return commandProperties.botGuildPermissions();
    }

    public Permission[] getAdjustedBotChannelPermissions() {
        Permission[] permissions = commandProperties.botChannelPermissions();
        return processBotPermissions(permissions);
    }

    private Permission[] processBotPermissions(Permission[] permissions) {
        if (Arrays.stream(permissions).anyMatch(permission -> permission == Permission.ADMINISTRATOR)) {
            return new Permission[] { Permission.ADMINISTRATOR };
        }

        if (this instanceof OnReactionListener || this instanceof OnStaticReactionAddListener || this instanceof OnStaticReactionRemoveListener) {
            if (Arrays.stream(permissions).noneMatch(permission -> permission == Permission.MESSAGE_HISTORY)) {
                permissions = Arrays.copyOf(permissions, permissions.length + 1);
                permissions[permissions.length - 1] = Permission.MESSAGE_HISTORY;
            }
            if (Arrays.stream(permissions).noneMatch(permission -> permission == Permission.MESSAGE_ADD_REACTION)) {
                permissions = Arrays.copyOf(permissions, permissions.length + 1);
                permissions[permissions.length - 1] = Permission.MESSAGE_ADD_REACTION;
            }
        }

        return permissions;
    }

    public boolean canRunOnGuild(long guildId, long userId) {
        long[] allowedServerIds = commandProperties.exclusiveGuilds();
        long[] allowedUserIds = commandProperties.exclusiveUsers();

        return ((allowedServerIds.length == 0) || Arrays.stream(allowedServerIds).anyMatch(checkServerId -> checkServerId == guildId)) &&
                ((allowedUserIds.length == 0) || Arrays.stream(allowedUserIds).anyMatch(checkUserId -> checkUserId == userId)) &&
                (!commandProperties.onlyPublicVersion() || Program.isPublicVersion());
    }

    public Optional<LocalDate> getReleaseDate() {
        int[] releaseDateArray = commandProperties.releaseDate();
        return Optional.ofNullable(releaseDateArray.length == 3 ? LocalDate.of(releaseDateArray[0], releaseDateArray[1], releaseDateArray[2]) : null);
    }

    public void addCompletedListener(Runnable runnable) {
        completedListeners.add(runnable);
    }

    public List<Runnable> getCompletedListeners() {
        return Collections.unmodifiableList(completedListeners);
    }

    public long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getTrigger() {
        return getCommandProperties().trigger();
    }

    public JSONObject getAttachments() {
        return attachments;
    }

    public void setAtomicAssets(TextChannel textChannel, Member member) {
        atomicGuild = new AtomicGuild(textChannel.getGuild());
        atomicTextChannel = new AtomicTextChannel(textChannel);
        atomicMember = new AtomicMember(member);
    }

    public Optional<GuildMessageReceivedEvent> getGuildMessageReceivedEvent() {
        return Optional.ofNullable(event);
    }

    public void setGuildMessageReceivedEvent(GuildMessageReceivedEvent event) {
        this.event = event;
    }

    public Optional<Guild> getGuild() {
        return Optional.ofNullable(atomicGuild)
                .flatMap(AtomicGuild::get);
    }

    public Optional<TextChannel> getTextChannel() {
        return Optional.ofNullable(atomicTextChannel)
                .flatMap(AtomicTextChannel::get);
    }

    public Optional<Member> getMember() {
        return Optional.ofNullable(atomicMember)
                .flatMap(AtomicMember::get);
    }

    public Optional<Long> getGuildId() {
        return Optional.ofNullable(atomicGuild)
                .map(AtomicGuild::getIdLong);
    }

    public Optional<Long> getTextChannelId() {
        return Optional.ofNullable(atomicTextChannel)
                .map(AtomicTextChannel::getIdLong);
    }

    public Optional<Long> getMemberId() {
        return Optional.ofNullable(atomicMember)
                .map(AtomicMember::getIdLong);
    }

    public CommandProperties getCommandProperties() {
        return commandProperties;
    }

    public static String getCategory(Class<? extends Command> clazz) {
        return CategoryCalculator.getCategoryByCommand(clazz);
    }

    public static CommandProperties getCommandProperties(Class<? extends Command> clazz) {
        return clazz.getAnnotation(CommandProperties.class);
    }

    public static CommandLanguage getCommandLanguage(Class<? extends Command> clazz, Locale locale) {
        String trigger = getCommandProperties(clazz).trigger();
        String category = getCategory(clazz);

        String title = TextManager.getString(locale, category, trigger + "_title");
        String descLong = TextManager.getString(locale, category, trigger + "_helptext");
        String usage = TextManager.getString(locale, category, trigger + "_usage");
        String examples = TextManager.getString(locale, category, trigger + "_examples");
        return new CommandLanguage(title, descLong, usage, examples);
    }

}
