package Modules.Twitch;

import Core.Internet.HttpProperty;
import Core.Internet.HttpRequest;
import Core.SecretManager;
import Core.Utils.InternetUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TwitchController {

    private static final TwitchController ourInstance = new TwitchController();
    public static TwitchController getInstance() { return ourInstance; }
    private TwitchController() {}

    private final LoadingCache<String, Optional<TwitchUser>> userCache = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<>() {
                        @Override
                        public Optional<TwitchUser> load(@NonNull String channelName) throws ExecutionException, InterruptedException, UnsupportedEncodingException {
                            return getTwitchUser(channelName);
                        }
                    });
    private final LoadingCache<String, JSONObject> channelMetaCache = CacheBuilder.newBuilder()
            .expireAfterWrite(4, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<>() {
                        @Override
                        public JSONObject load(@NonNull String channelId) throws ExecutionException, InterruptedException {
                            return fetchApi("https://api.twitch.tv/kraken/streams/" + channelId);
                        }
                    });


    public Optional<TwitchStream> getStream(String channelName) throws ExecutionException {
        Optional<TwitchUser> twitchUserOpt = userCache.get(channelName);

        if (twitchUserOpt.isPresent()) {
            TwitchUser twitchUser = twitchUserOpt.get();
            JSONObject data = channelMetaCache.get(twitchUser.getChannelId());

            if (data.isNull("stream"))
                return Optional.of(new TwitchStream(twitchUser));

            return Optional.of(parseStreamData(twitchUser, data));
        }

        return Optional.empty();
    }

    private TwitchStream parseStreamData(TwitchUser twitchUser, JSONObject data) {
        data = data.getJSONObject("stream");
        final JSONObject channelJSON = data.getJSONObject("channel");

        final String previewImage = data.getJSONObject("preview").getString("large");
        final String game = channelJSON.getString("game");
        final String status = channelJSON.getString("status");
        final int viewers = data.getInt("viewers");
        final int followers = channelJSON.getInt("followers");

        return new TwitchStream(twitchUser, previewImage, game, status, viewers, followers);
    }

    private Optional<TwitchUser> getTwitchUser(String channelName) throws ExecutionException, InterruptedException, UnsupportedEncodingException {
        JSONObject data = fetchApi("https://api.twitch.tv/kraken/users?login=" + InternetUtil.encodeForURL(channelName));
        JSONArray users = data.getJSONArray("users");

        if (users.length() == 0)
            return Optional.empty();

        final String channelId = users.getJSONObject(0).getString("_id");
        final String logoUrl = users.getJSONObject(0).getString("logo");
        final String displayName = users.getJSONObject(0).getString("display_name");
        return Optional.of(new TwitchUser(channelId, channelName, displayName, logoUrl));
    }

    private JSONObject fetchApi(String url) throws ExecutionException, InterruptedException {
        HttpProperty[] properties = {
                new HttpProperty("Accept", "application/vnd.twitchtv.v5+json"),
                new HttpProperty("Client-ID", SecretManager.getString("twitch.clientid"))
        };

        String content = HttpRequest.getData(url, properties)
                .get().getContent().get();
        return new JSONObject(content);
    }

}
