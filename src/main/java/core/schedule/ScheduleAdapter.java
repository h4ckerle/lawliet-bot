package core.schedule;

import java.util.TimerTask;
import core.MainLogger;

public class ScheduleAdapter extends TimerTask {

    private final ScheduleInterface scheduleEvent;

    public ScheduleAdapter(ScheduleInterface scheduleEvent) {
        this.scheduleEvent = scheduleEvent;
    }

    @Override
    public void run() {
        try {
            scheduleEvent.run();
        } catch (Throwable throwable) {
            MainLogger.get().error("Scheduled event failed", throwable);
        }
    }

}
