package core;

import java.util.Arrays;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class ExceptionFilter extends Filter<ILoggingEvent> {

    private final String[] FILTERS = {
            "10003",    /* Unknown channel */
            "10007",    /* Unknown member */
            "10008",    /* Unknown message */
            "10011",    /* Unknown role */
            "30007",    /* Maximum number of webhook reached */
            "50001",    /* Missing access */
            "50007",    /* Cannot send messages to this user */
            "90001",    /* Reaction blocked */
            "The Requester has been stopped! No new requests can be requested!",
            "Timeout",
            "Received a GuildVoiceState with a channel ID for a non-existent channel!",
            "There was an I/O error while executing a REST request: timeout",
            "500: Internal Server Error"
    };

    public ExceptionFilter() {
    }

    @Override
    public FilterReply decide(final ILoggingEvent event) {
        if (!shouldBeVisible(event.getFormattedMessage())) {
            return FilterReply.DENY;
        }

        final IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null) {
            return FilterReply.NEUTRAL;
        }

        if (!(throwableProxy instanceof ThrowableProxy)) {
            return FilterReply.NEUTRAL;
        }

        final ThrowableProxy throwableProxyImpl = (ThrowableProxy) throwableProxy;
        if (!shouldBeVisible(throwableProxyImpl.getThrowable().toString())) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }

    public boolean shouldBeVisible(String message) {
        return !Program.isProductionMode() || Arrays.stream(FILTERS).noneMatch(filter -> message.toLowerCase().contains(filter.toLowerCase()));
    }

}
