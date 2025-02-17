package modules.porn;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PornImageCache {

    private static final PornImageCache ourInstance = new PornImageCache();

    public static PornImageCache getInstance() {
        return ourInstance;
    }

    private PornImageCache() {
    }

    private final LoadingCache<String, PornImageCacheSearchKey> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(50)
            .build(
                    new CacheLoader<>() {
                        @Override
                        public PornImageCacheSearchKey load(@NonNull String searchKey) {
                            return new PornImageCacheSearchKey();
                        }
                    }
            );

    public PornImageCacheSearchKey get(@NonNull String domain, @NonNull String searchKey) {
        try {
            return cache.get(domain + "|" + searchKey.toLowerCase());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}