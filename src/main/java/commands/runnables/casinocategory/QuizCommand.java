package commands.runnables.casinocategory;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoAbstract;
import constants.Category;
import constants.Emojis;
import constants.LogStatus;
import constants.Settings;
import core.EmbedFactory;
import core.TextManager;
import core.internet.HttpRequest;
import core.schedule.MainScheduler;
import core.utils.EmbedUtil;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import org.json.JSONArray;
import org.json.JSONObject;

@CommandProperties(
        trigger = "quiz",
        emoji = "❔",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        deleteOnTimeOut = true,
        executableWithoutArgs = true
)
public class QuizCommand extends CasinoAbstract {

    private int difficulty;
    private String question;
    private String[] answers;
    private int correctAnswer;
    private int answerSelected;
    private final String url;

    public QuizCommand(Locale locale, String prefix) {
        this(locale, prefix, true, "https://opentdb.com/api.php?amount=1");
    }

    public QuizCommand(Locale locale, String prefix, boolean allowBet, String url) {
        super(locale, prefix, allowBet, true);
        this.url = url;
    }

    @Override
    public String[] onGameStart(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        String dataString, diffString;
        JSONObject data;
        addLoadingReactionInstantly();
        dataString = HttpRequest.getData(url).get().getContent().get();
        data = new JSONObject(dataString).getJSONArray("results").getJSONObject(0);
        diffString = data.getString("difficulty");

        switch (diffString) {
            case "easy" -> difficulty = 0;
            case "medium" -> difficulty = 1;
            case "hard" -> difficulty = 2;
        }

        question = StringUtil.decryptString(data.getString("question"));

        ArrayList<String> orderedAnswers = new ArrayList<>();
        orderedAnswers.add(StringUtil.decryptString(data.getString("correct_answer")));

        JSONArray answersJSON = data.getJSONArray("incorrect_answers");
        for (int i = 0; i < answersJSON.length(); i++) {
            orderedAnswers.add(StringUtil.decryptString(answersJSON.getString(i)));
        }

        answers = new String[orderedAnswers.size()];
        Random r = new Random();
        correctAnswer = -1;
        for (int i = 0; i < answers.length; i++) {
            int select = r.nextInt(orderedAnswers.size());
            answers[i] = orderedAnswers.get(select);
            if (select == 0 && correctAnswer == -1) correctAnswer = i;
            orderedAnswers.remove(select);
        }

        setCompareKey("quiz_" + answers.length + "_" + difficulty);
        MainScheduler.getInstance().schedule(10, ChronoUnit.SECONDS, "quiz_timeup", this::onTimeUp);
        return Arrays.copyOf(Emojis.LETTERS, answers.length);
    }

    @Override
    public boolean onReactionCasino(GenericGuildMessageReactionEvent event) {
        for (int i = 0; i < answers.length; i++) {
            if (EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), Emojis.LETTERS[i])) {
                onAnswerSelected(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public EmbedBuilder drawCasino(String playerName, long coinsInput) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .addField(getString("question"), question, false)
                .addField(getString("answers"), getAnswersString(), false);

        if (coinsInput != 0) {
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));
        }

        String label = "tutorial";
        if (getStatus() == Status.ACTIVE) {
            label = "tutorial_start";
        }

        eb.addField(Emojis.EMPTY_EMOJI, getString(label, playerName, StringUtil.numToString(coinsInput), Emojis.COUNTDOWN_10), false);
        return eb;
    }

    private String getAnswersString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < answers.length; i++) {
            if (getStatus() != Status.ACTIVE && correctAnswer == i) {
                sb.append(Emojis.CHECKMARK);
            } else if (getStatus() != Status.ACTIVE && answerSelected == i) {
                sb.append(Emojis.X);
            } else {
                sb.append(Emojis.LETTERS[i]);
            }
            sb.append(" | ").append(answers[i]).append("\n");
        }

        return sb.toString();
    }

    private void onTimeUp() {
        if (getStatus() == Status.ACTIVE) {
            onAnswerSelected(-1);
            drawMessage(draw());
        }
    }

    private void onAnswerSelected(int selected) {
        if (selected == correctAnswer) {
            win(answers.length * (difficulty + 1) / 8.0);
            setLog(LogStatus.WIN, getString("correct"));
        } else {
            lose();
            setLog(LogStatus.LOSE, selected == -1 ? getString("timeup") : getString("wrong"));
        }

        answerSelected = selected;
        MainScheduler.getInstance().schedule(Settings.TIME_OUT_MINUTES, ChronoUnit.MINUTES, "quiz_remove", this::deregisterListenersWithMessage);
    }

}
