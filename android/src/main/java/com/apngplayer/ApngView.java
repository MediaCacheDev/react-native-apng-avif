package com.apngplayer;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.github.penfeizhou.animation.FrameAnimationDrawable;
import com.github.penfeizhou.animation.apng.APNGDrawable;
import com.github.penfeizhou.animation.avif.AVIFDrawable;
import com.github.penfeizhou.animation.loader.FileLoader;
import com.github.penfeizhou.animation.loader.Loader;
import com.github.penfeizhou.animation.loader.StreamLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ApngView extends AppCompatImageView {
    private static final String TAG = "ApngView";

    private FrameAnimationDrawable<?> animationDrawable;
    private boolean autoPlay = true;
    private boolean paused = false;
    private int loopCount = 0;
    private String currentUri = null;

    public ApngView(Context context) {
        super(context);
        init();
    }

    public ApngView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ApngView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setScaleType(ScaleType.FIT_CENTER);
        setBackgroundColor(0x00000000);
        setClickable(false);
    }

    public void setSource(@Nullable String uriString) {
        releaseDrawable();
        currentUri = uriString;

        if (TextUtils.isEmpty(uriString)) {
            setImageDrawable(null);
            return;
        }

        try {
            Uri uri = Uri.parse(uriString);
            String scheme = uri.getScheme();

            if (scheme == null) {
                // Treat as file path
                File file = new File(uriString);
                if (!file.exists()) {
                    Log.w(TAG, "File does not exist: " + uriString);
                    setImageDrawable(null);
                    return;
                }
                loadDrawable(new FileLoader(file.getAbsolutePath()));
                return;
            }

            if ("file".equalsIgnoreCase(scheme)) {
                String path = uri.getPath();
                if (path == null) {
                    Log.w(TAG, "Invalid file URI: " + uriString);
                    setImageDrawable(null);
                    return;
                }
                // Handle android_asset path
                if (path.startsWith("/android_asset/")) {
                    final String assetPath = path.substring("/android_asset/".length());
                    final Context context = getContext().getApplicationContext();
                    loadDrawable(new StreamLoader() {
                        @Override
                        public InputStream getInputStream() throws IOException {
                            return context.getAssets().open(assetPath);
                        }
                    });
                    return;
                }
                loadDrawable(new FileLoader(path));
                return;
            }

            // Handle asset:// scheme (React Native bundled assets)
            if ("asset".equalsIgnoreCase(scheme)) {
                String assetPath = uri.getPath();
                if (assetPath != null && assetPath.startsWith("/")) {
                    assetPath = assetPath.substring(1);
                }
                final String finalAssetPath = assetPath;
                final Context context = getContext().getApplicationContext();
                loadDrawable(new StreamLoader() {
                    @Override
                    public InputStream getInputStream() throws IOException {
                        return context.getAssets().open(finalAssetPath);
                    }
                });
                return;
            }

            // Handle http/https (for dev server or remote URLs)
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                loadFromUrl(uriString);
                return;
            }

            // Fallback: use ContentResolver
            final Context context = getContext().getApplicationContext();
            loadDrawable(new StreamLoader() {
                @Override
                public InputStream getInputStream() throws IOException {
                    InputStream inputStream = context.getContentResolver().openInputStream(uri);
                    if (inputStream == null) {
                        throw new IOException("Unable to open input stream for URI: " + uri);
                    }
                    return inputStream;
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "Failed to load APNG source: " + uriString, e);
            setImageDrawable(null);
        }
    }

    private void loadFromUrl(final String urlString) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlString);
                    final InputStream inputStream = url.openStream();

                    // Need to buffer the stream since it might be read multiple times
                    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, bytesRead);
                    }
                    inputStream.close();
                    final byte[] imageData = buffer.toByteArray();

                    // Load on main thread
                    post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                loadDrawable(new StreamLoader() {
                                    @Override
                                    public InputStream getInputStream() throws IOException {
                                        return new ByteArrayInputStream(imageData);
                                    }
                                });
                            } catch (IOException e) {
                                Log.e(TAG, "Failed to load APNG from URL buffer", e);
                                setImageDrawable(null);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to download APNG from URL: " + urlString, e);
                    post(new Runnable() {
                        @Override
                        public void run() {
                            setImageDrawable(null);
                        }
                    });
                }
            }
        }).start();
    }

    public void setAutoPlay(boolean value) {
        autoPlay = value;
        updatePlaybackState();
    }

    public void setPaused(boolean value) {
        paused = value;
        updatePlaybackState();
    }

    public void setLoopCount(int value) {
        loopCount = Math.max(0, value);
        applyLoopCount();
    }

    public void setResizeMode(String resizeMode) {
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
            case "contain":
            default:
                setScaleType(ScaleType.FIT_CENTER);
                break;
        }
    }

    private void loadDrawable(Loader loader) throws IOException {
        try {
            // Detect file type from URI and create appropriate drawable
            boolean isAvif = isAvifFile(currentUri);
            
            if (isAvif) {
                animationDrawable = new AVIFDrawable(loader);
                Log.d(TAG, "Created AVIFDrawable for: " + currentUri);
            } else {
                animationDrawable = new APNGDrawable(loader);
                Log.d(TAG, "Created APNGDrawable for: " + currentUri);
            }
            
            // Validate the drawable
            int width = animationDrawable.getIntrinsicWidth();
            int height = animationDrawable.getIntrinsicHeight();
            if (width <= 0 || height <= 0) {
                throw new IOException("Invalid animation dimensions: " + width + "x" + height);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to create or validate animation drawable", e);
            animationDrawable = null;
            setImageDrawable(null);
            return;
        }
        applyLoopCount();
        setImageDrawable(animationDrawable);
        updatePlaybackState();
    }

    private boolean isAvifFile(String uri) {
        if (uri == null) return false;
        String lowerUri = uri.toLowerCase();
        return lowerUri.endsWith(".avif") || lowerUri.contains(".avif?");
    }

    private void applyLoopCount() {
        if (animationDrawable == null) {
            return;
        }

        if (loopCount <= 0) {
            animationDrawable.setLoopLimit(0); // 0 == infinite loop
        } else {
            animationDrawable.setLoopLimit(loopCount);
        }
    }

    private void updatePlaybackState() {
        if (animationDrawable == null) {
            return;
        }

        if (!autoPlay || paused) {
            animationDrawable.stop();
        } else {
            if (!animationDrawable.isRunning()) {
                try {
                    animationDrawable.start();
                } catch (Throwable e) {
                    Log.e(TAG, "Failed to start animation playback", e);
                    releaseDrawable();
                }
            }
        }
    }

    private void releaseDrawable() {
        if (animationDrawable != null) {
            animationDrawable.stop();
            animationDrawable = null;
        }
        currentUri = null;
        setImageDrawable(null);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseDrawable();
    }
}
