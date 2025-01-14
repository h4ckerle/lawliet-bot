package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.GelbooruAbstract;

@CommandProperties(
        trigger = "ahegao",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true
)
public class AhegaoCommand extends GelbooruAbstract {

    public AhegaoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "animated ahegao -yaoi -yuri";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}