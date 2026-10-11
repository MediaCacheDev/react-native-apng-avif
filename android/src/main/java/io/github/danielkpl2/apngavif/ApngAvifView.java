package io.github.danielkpl2.apngavif;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.UIManagerHelper;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.github.penfeizhou.animation.FrameAnimationDrawable;
import com.github.penfeizhou.animation.apng.APNGDrawable;
import com.github.penfeizhou.animation.avif.AVIFDrawable;
import com.github.penfeizhou.animation.loader.AssetStreamLoader;
import com.github.penfeizhou.animation.loader.FileLoader;
import com.github.penfeizhou.animation.loader.Loader;
import com.github.penfeizhou.animation.loader.ResourceStreamLoader;
import com.github.penfeizhou.animation.loader.StreamLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plays an animated APNG or AVIF.
 *
 * Accepted sources: {@code http(s)://} (downloaded once into the cache dir, keyed by
 * {@code cacheKey}), {@code file://} or an absolute path, {@code asset://} (APK assets, streamed),
 * {@code content://}, and a bare resource name, which is what {@code require()} resolves to in an
 * Android release build. Nothing is copied apart from the remote download.
 */
public class ApngAvifView extends AppCompatImageView {
    private static final String TAG = "ApngAvifView";
    private static final String CACHE_DIR = "react-native-apng-avif";

    private static final ExecutorService IO = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, TAG + "-io");
        thread.setDaemon(true);
        return thread;
    });

    private interface Opener {
        InputStream open() throws IOException;
    }

    private static final class Resolved {
        final Opener opener;
        final Loader loader;

        Resolved(Opener opener, Loader loader) {
            this.opener = opener;
            this.loader = loader;
        }
    }

    private FrameAnimationDrawable<?> drawable;
    private boolean autoPlay = true;
    private boolean paused = false;
    private int loopCount = 0;

    @Nullable private String uri;
    @Nullable private String cacheKey;
    // Bumped on every source change; a load that finishes under an older value is dropped, so a
    // slow download can't replace the image the caller asked for after it.
    private int generation = 0;

    public ApngAvifView(Context context) {
        super(context);
        setScaleType(ScaleType.FIT_CENTER);
        setBackgroundColor(0x00000000);
        setClickable(false);
    }

    public void setSource(@Nullable String uri, @Nullable String cacheKey) {
        if (TextUtils.equals(uri, this.uri) && TextUtils.equals(cacheKey, this.cacheKey)) {
            return;
        }
        this.uri = uri;
        this.cacheKey = cacheKey;
        load();
    }

    public void setAutoPlay(boolean value) {
        autoPlay = value;
        updatePlayback();
    }

    public void setPaused(boolean value) {
        paused = value;
        updatePlayback();
    }

    public void setLoopCount(int value) {
        loopCount = Math.max(0, value);
        if (drawable != null) {
            drawable.setLoopLimit(loopCount);
        }
    }

    public void setResizeMode(@Nullable String resizeMode) {
        if (resizeMode == null) {
            setScaleType(ScaleType.FIT_CENTER);
            return;
        }
        switch (resizeMode) {
            case "cover":
                setScaleType(ScaleType.CENTER_CROP);
                break;
            case "stretch":
                setScaleType(ScaleType.FIT_XY);
                break;
            case "center":
                setScaleType(ScaleType.CENTER_INSIDE);
                break;
            default:
                setScaleType(ScaleType.FIT_CENTER);
                break;
        }
    }

    /** Called when React drops the view; the view is not reused after this. */
    public void release() {
        generation++;
        clearDrawable();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updatePlayback();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Stop, but keep the drawable: a view detached by navigation or list recycling comes back
        // with the same props, and React won't set the source again to reload it.
        if (drawable != null) {
            drawable.stop();
        }
    }

    private void load() {
        final int token = ++generation;
        clearDrawable();

        final String source = uri;
        if (TextUtils.isEmpty(source)) {
            return;
        }
        final String key = TextUtils.isEmpty(cacheKey) ? source : cacheKey;
        final Context context = getContext().getApplicationContext();

        IO.execute(() -> {
            try {
                Resolved resolved = resolve(context, source, key);
                FrameAnimationDrawable<?> created =
                        isAvif(resolved.opener) ? new AVIFDrawable(resolved.loader) : new APNGDrawable(resolved.loader);
                int width = created.getIntrinsicWidth();
                int height = created.getIntrinsicHeight();
                if (width <= 0 || height <= 0) {
                    throw new IOException("Not an APNG or AVIF image (no dimensions)");
                }
                post(() -> {
                    if (token != generation) {
                        created.stop();
                        return;
                    }
                    show(created);
                    emitLoad(width, height);
                });
            } catch (Throwable error) {
                Log.w(TAG, "Failed to load " + source, error);
                post(() -> {
                    if (token == generation) {
                        emitError(error.getMessage() != null ? error.getMessage() : error.toString());
                    }
                });
            }
        });
    }

    private static Resolved resolve(Context context, String source, String key) throws IOException {
        Uri parsed = Uri.parse(source);
        String scheme = parsed.getScheme();

        if (scheme == null) {
            if (source.startsWith("/")) {
                return fromFile(new File(source));
            }
            return fromResource(context, source);
        }

        switch (scheme.toLowerCase()) {
            case "http":
            case "https":
                return fromFile(download(context, source, key));
            case "file": {
                String path = parsed.getPath();
                if (path == null) {
                    throw new IOException("Invalid file URI: " + source);
                }
                if (path.startsWith("/android_asset/")) {
                    return fromAsset(context, path.substring("/android_asset/".length()));
                }
                return fromFile(new File(path));
            }
            case "asset": {
                // asset://dir/file.apng parses with "dir" as the host, so rejoin host and path.
                String path = (parsed.getHost() != null ? parsed.getHost() : "")
                        + (parsed.getPath() != null ? parsed.getPath() : "");
                return fromAsset(context, path.replaceFirst("^/+", ""));
            }
            default:
                return fromContentResolver(context, parsed);
        }
    }

    private static Resolved fromFile(File file) throws IOException {
        if (!file.isFile()) {
            throw new IOException("File not found: " + file);
        }
        final String path = file.getAbsolutePath();
        return new Resolved(() -> new FileInputStream(path), new FileLoader(path));
    }

    private static Resolved fromAsset(Context context, String path) {
        return new Resolved(() -> context.getAssets().open(path), new AssetStreamLoader(context, path));
    }

    /**
     * A release-build {@code require()}: Metro packages the file as a resource and the JS side
     * receives only its name. Looked up by that exact name — non-image extensions go to
     * {@code res/raw}, a {@code .png} to {@code res/drawable}.
     */
    private static Resolved fromResource(Context context, String name) throws IOException {
        String packageName = context.getPackageName();
        int id = context.getResources().getIdentifier(name, "raw", packageName);
        if (id == 0) {
            id = context.getResources().getIdentifier(name, "drawable", packageName);
        }
        if (id == 0) {
            throw new IOException("No raw or drawable resource named " + name);
        }
        final int resourceId = id;
        return new Resolved(
                () -> context.getResources().openRawResource(resourceId),
                new ResourceStreamLoader(context, resourceId));
    }

    private static Resolved fromContentResolver(Context context, Uri uri) {
        Opener opener = () -> {
            InputStream stream = context.getContentResolver().openInputStream(uri);
            if (stream == null) {
                throw new IOException("Unable to open " + uri);
            }
            return stream;
        };
        return new Resolved(opener, new StreamLoader() {
            @Override
            public InputStream getInputStream() throws IOException {
                return opener.open();
            }
        });
    }

    private static File download(Context context, String url, String key) throws IOException {
        File dir = new File(context.getCacheDir(), CACHE_DIR);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create cache dir " + dir);
        }
        File target = new File(dir, sha256(key));
        if (target.isFile() && target.length() > 0) {
            return target;
        }

        File partial = new File(dir, target.getName() + ".part" + Thread.currentThread().getId());
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(30_000);
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("HTTP " + status + " for " + url);
            }
            try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(partial)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            if (!partial.renameTo(target)) {
                throw new IOException("Cannot move download into " + target);
            }
            return target;
        } finally {
            connection.disconnect();
            //noinspection ResultOfMethodCallIgnored
            partial.delete();
        }
    }

    /**
     * True when the header is an ISO-BMFF {@code ftyp} box naming an AVIF brand ({@code avif} for
     * stills, {@code avis} for sequences) as the major or a compatible brand. Anything else is
     * handed to the APNG decoder, which also plays a plain PNG as a single frame.
     */
    private static boolean isAvif(Opener opener) throws IOException {
        byte[] header = new byte[64];
        int length = 0;
        try (InputStream in = opener.open()) {
            int read;
            while (length < header.length && (read = in.read(header, length, header.length - length)) != -1) {
                length += read;
            }
        }
        if (length < 16 || !ascii(header, 4).equals("ftyp")) {
            return false;
        }
        int boxSize = ((header[0] & 0xff) << 24) | ((header[1] & 0xff) << 16) | ((header[2] & 0xff) << 8) | (header[3] & 0xff);
        int end = Math.min(length, boxSize > 0 ? boxSize : length);
        // Major brand at 8, minor version at 12, compatible brands from 16.
        for (int offset = 8; offset + 4 <= end; offset += (offset == 8 ? 8 : 4)) {
            String brand = ascii(header, offset);
            if (brand.equals("avif") || brand.equals("avis")) {
                return true;
            }
        }
        return false;
    }

    private static String ascii(byte[] bytes, int offset) {
        return new String(bytes, offset, 4, StandardCharsets.US_ASCII);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private void show(FrameAnimationDrawable<?> created) {
        drawable = created;
        drawable.setLoopLimit(loopCount);
        setImageDrawable(drawable);
        updatePlayback();
    }

    private void clearDrawable() {
        if (drawable != null) {
            drawable.stop();
            drawable = null;
        }
        setImageDrawable(null);
    }

    private void updatePlayback() {
        if (drawable == null) {
            return;
        }
        if (!autoPlay || paused || !isAttachedToWindow()) {
            drawable.stop();
        } else if (!drawable.isRunning()) {
            drawable.start();
        }
    }

    private void emitLoad(int width, int height) {
        WritableMap data = Arguments.createMap();
        data.putInt("width", width);
        data.putInt("height", height);
        emit(ApngAvifEvent.LOAD, data);
    }

    private void emitError(String message) {
        WritableMap data = Arguments.createMap();
        data.putString("message", message);
        emit(ApngAvifEvent.ERROR, data);
    }

    private void emit(String name, WritableMap data) {
        if (!(getContext() instanceof ReactContext)) {
            return;
        }
        EventDispatcher dispatcher = UIManagerHelper.getEventDispatcherForReactTag((ReactContext) getContext(), getId());
        if (dispatcher != null) {
            dispatcher.dispatchEvent(new ApngAvifEvent(UIManagerHelper.getSurfaceId(this), getId(), name, data));
        }
    }
}
