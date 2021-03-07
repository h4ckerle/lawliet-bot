package core;

import commands.Command;
import commands.CommandManager;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.utilitycategory.AlertsCommand;
import core.utils.TimeUtil;
import mysql.modules.tracker.TrackerBean;
import mysql.modules.tracker.TrackerBeanSlot;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlertScheduler {

    private static final AlertScheduler ourInstance = new AlertScheduler();

    public static AlertScheduler getInstance() {
        return ourInstance;
    }

    private AlertScheduler() {
    }

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private boolean active = false;

    public synchronized void start(TrackerBean trackerBean) {
        if (active) return;
        active = true;

        MainLogger.get().info("Starting {} alerts", trackerBean.getSlots().size());
        new ArrayList<>(trackerBean.getSlots())
                .forEach(this::registerAlert);
    }

    public void registerAlert(TrackerBeanSlot slot) {
        executorService.schedule(() -> {
            if (slot.isActive() && ShardManager.getInstance().guildIsManaged(slot.getGuildId()) && manageAlert(slot)) {
                registerAlert(slot);
            }
        }, TimeUtil.getMillisBetweenInstants(Instant.now(), slot.getNextRequest()), TimeUnit.MILLISECONDS);
    }

    private boolean manageAlert(TrackerBeanSlot slot) {
        Instant minInstant = Instant.now().plus(1, ChronoUnit.MINUTES);

        try {
            processAlert(slot);
        } catch (Throwable throwable) {
            MainLogger.get().error("Error in tracker \"{}\" with key \"{}\"", slot.getCommandTrigger(), slot.getCommandKey(), throwable);
            minInstant = Instant.now().plus(10, ChronoUnit.MINUTES);
            if (throwable.toString().contains("Unknown Channel"))
                slot.delete();
        }

        if (slot.isActive()) {
            if (minInstant.isAfter(slot.getNextRequest()))
                slot.setNextRequest(minInstant);
            return true;
        }

        return false;
    }

    private void processAlert(TrackerBeanSlot slot) throws Throwable {
        Optional<Command> commandOpt = CommandManager.createCommandByTrigger(slot.getCommandTrigger(), slot.getGuildBean().getLocale(), slot.getGuildBean().getPrefix());
        if (commandOpt.isEmpty()) {
            MainLogger.get().error("Invalid command for alert: {}", slot.getCommandTrigger());
            slot.stop();
            return;
        }

        OnTrackerRequestListener command = commandOpt.map(c -> (OnTrackerRequestListener) c).get();
        Optional<TextChannel> channelOpt = slot.getTextChannel();
        if (channelOpt.isPresent()) {
            if (PermissionCheckRuntime.getInstance().botHasPermission(((Command) command).getLocale(), AlertsCommand.class, channelOpt.get(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS)) {
                switch (command.onTrackerRequest(slot)) {
                    case STOP:
                        slot.stop();
                        break;

                    case STOP_AND_DELETE:
                        slot.delete();
                        break;

                    case STOP_AND_SAVE:
                        slot.stop();
                        slot.save();
                        break;

                    case CONTINUE:
                        break;

                    case CONTINUE_AND_SAVE:
                        slot.save();
                        break;
                }
            }
        } else if (slot.getGuild().isPresent()) {
            slot.delete();
        }
    }

}
