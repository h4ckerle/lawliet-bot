package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

@CommandProperties(
        trigger = "bully",
        emoji = "\uD83D\uDE08",
        executableWithoutArgs = true
)
public class BullyCommand extends InteractionAbstract {

    public BullyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[] {
                "https://media.discordapp.net/attachments/736272234628251689/736272238071906314/bully.gif",
                "https://media.discordapp.net/attachments/736272234628251689/736272249224560720/bully.gif",
                "https://media.discordapp.net/attachments/736272234628251689/736272262466109460/bully.gif",
                "https://media.discordapp.net/attachments/736272234628251689/736272267989876866/bully.gif",
                "https://media.discordapp.net/attachments/736272234628251689/736272276118569030/bully.gif",
                "https://media.discordapp.net/attachments/736272234628251689/736272279574413322/bully.gif",
                "https://media.discordapp.net/attachments/736272234628251689/736272283198554212/bully.gif",
                "https://media.discordapp.net/attachments/736272234628251689/736272291247292426/bully.gif"
        };
    }

}
