package com.apngplayer;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApngViewManager extends SimpleViewManager<ApngView> {
    private static final String TAG = "ApngViewManager";
    public static final String REACT_CLASS = "ApngView";
    
    private final ReactApplicationContext reactContext;
    private final ExecutorService assetExecutor = Executors.newSingleThreadExecutor();

    public ApngViewManager(ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @NonNull
    @Override
    protected ApngView createViewInstance(@NonNull ThemedReactContext context) {
        return new ApngView(context);
    }

    @ReactProp(name = "source")
    public void setSource(ApngView view, @Nullable ReadableMap source) {
        if (source == null) {
            view.setSource(null);
            return;
        }

        String uri = null;
        if (source.hasKey("uri") && !source.isNull("uri")) {
            uri = source.getString("uri");
        }

        if (uri == null || uri.isEmpty()) {
            view.setSource(null);
            return;
        }

        Log.d(TAG, "setSource: " + uri);

        // Handle different URI schemes
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            // HTTP URLs - pass directly, view handles download
            view.setSource(uri);
        } else if (uri.startsWith("file://")) {
            // Strip file:// prefix for local files
            String path = uri.substring(7);
            view.setSource(path);
        } else if (uri.startsWith("asset://") || uri.startsWith("asset:/")) {
            // Load from Android assets
            loadFromAsset(view, uri);
        } else if (isBundledResourceName(uri)) {
            // Load from bundled assets (release build)
            loadFromBundledAsset(view, uri);
        } else {
            // Plain path - pass directly
            view.setSource(uri);
        }
    }

    /**
     * Check if the URI looks like a bundled asset resource name (from release build)
     * These look like: "src_assets_test_apng" (no slashes, no scheme, no dots except extension)
     */
    private boolean isBundledResourceName(String uri) {
        if (uri == null || uri.isEmpty()) return false;
        if (uri.contains("/") || uri.contains(":")) return false;
        // Check if it looks like a resource name (alphanumeric with underscores, starting with letter)
        return uri.matches("^[a-zA-Z][a-zA-Z0-9_]*$");
    }

    /**
     * Detect file extension from resource name (e.g., "src_assets_test_avif" -> ".avif")
     */
    private String detectExtension(String resourceName) {
        String lower = resourceName.toLowerCase();
        if (lower.endsWith("_avif") || lower.contains("_avif_")) return ".avif";
        if (lower.endsWith("_apng") || lower.contains("_apng_")) return ".apng";
        if (lower.endsWith("_png") || lower.contains("_png_")) return ".png";
        return ".apng"; // default
    }

    /**
     * Load APNG/AVIF from bundled assets (for release build)
     * React Native bundles custom assets into the APK's res/raw folder or assets folder.
     */
    private void loadFromBundledAsset(ApngView view, String resourceName) {
        assetExecutor.execute(() -> {
            try {
                String extension = detectExtension(resourceName);
                
                // First, try to find in assets folder
                String foundPath = findAnimatedAsset(resourceName, "");
                if (foundPath != null) {
                    // Use extension from found file
                    String foundExt = foundPath.substring(foundPath.lastIndexOf('.'));
                    // Copy from assets to cache
                    File cacheFile = new File(reactContext.getCacheDir(), 
                        "anim_bundled_" + resourceName.hashCode() + foundExt);
                    
                    if (!cacheFile.exists()) {
                        InputStream is = reactContext.getAssets().open(foundPath);
                        FileOutputStream fos = new FileOutputStream(cacheFile);
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                        fos.close();
                        is.close();
                        Log.d(TAG, "Copied bundled asset to cache: " + cacheFile.getAbsolutePath());
                    }
                    
                    view.post(() -> view.setSource(cacheFile.getAbsolutePath()));
                    return;
                }
                
                // Fallback: try raw resources
                int resourceId = reactContext.getResources().getIdentifier(
                    resourceName, "raw", reactContext.getPackageName());
                if (resourceId != 0) {
                    File cacheFile = new File(reactContext.getCacheDir(), 
                        "anim_raw_" + resourceName.hashCode() + extension);
                    
                    if (!cacheFile.exists()) {
                        InputStream is = reactContext.getResources().openRawResource(resourceId);
                        FileOutputStream fos = new FileOutputStream(cacheFile);
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                        fos.close();
                        is.close();
                        Log.d(TAG, "Copied raw resource to cache: " + cacheFile.getAbsolutePath());
                    }
                    
                    view.post(() -> view.setSource(cacheFile.getAbsolutePath()));
                    return;
                }
                
                Log.e(TAG, "Bundled asset not found: " + resourceName);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load bundled asset: " + resourceName, e);
            }
        });
    }

    /**
     * Search assets folder for an .apng, .avif, or .png file matching the resource name
     */
    private String findAnimatedAsset(String resourceName, String path) {
        try {
            String[] files = reactContext.getAssets().list(path);
            if (files == null) return null;
            
            // Extract a search key from the resource name
            String searchKey = resourceName.toLowerCase().replace("_", "");
            
            for (String file : files) {
                String fullPath = path.isEmpty() ? file : path + "/" + file;
                
                if (file.endsWith(".apng") || file.endsWith(".avif") || file.endsWith(".png")) {
                    // Normalize filename for comparison
                    String normalized = file.replace(".apng", "").replace(".avif", "").replace(".png", "")
                        .replace("-", "").replace("_", "").toLowerCase();
                    String baseFileName = file.substring(0, file.lastIndexOf('.')).toLowerCase();
                    if (normalized.contains(searchKey) || searchKey.contains(normalized) ||
                        resourceName.toLowerCase().contains(baseFileName)) {
                        Log.d(TAG, "Found animated asset: " + fullPath + " for resource: " + resourceName);
                        return fullPath;
                    }
                }
                
                // Recurse into directories
                String[] subFiles = reactContext.getAssets().list(fullPath);
                if (subFiles != null && subFiles.length > 0) {
                    String found = findAnimatedAsset(resourceName, fullPath);
                    if (found != null) return found;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Load APNG/AVIF from Android assets by copying to cache first
     */
    private void loadFromAsset(ApngView view, String assetUri) {
        final String assetPath = assetUri.replace("asset://", "").replace("asset:/", "")
            .replaceFirst("^/", "");
        
        // Preserve original extension
        String extension = ".apng";
        int dotIndex = assetPath.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = assetPath.substring(dotIndex);
        }
        final String ext = extension;
        
        assetExecutor.execute(() -> {
            try {
                File tempFile = new File(reactContext.getCacheDir(), 
                    "anim_asset_" + assetPath.hashCode() + ext);
                
                if (!tempFile.exists()) {
                    InputStream is = reactContext.getAssets().open(assetPath);
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                    fos.close();
                    is.close();
                }
                
                view.post(() -> view.setSource(tempFile.getAbsolutePath()));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load asset: " + assetPath, e);
            }
        });
    }

    @ReactProp(name = "autoPlay", defaultBoolean = true)
    public void setAutoPlay(ApngView view, boolean value) {
        view.setAutoPlay(value);
    }

    @ReactProp(name = "paused", defaultBoolean = false)
    public void setPaused(ApngView view, boolean value) {
        view.setPaused(value);
    }

    @ReactProp(name = "loopCount", defaultInt = 0)
    public void setLoopCount(ApngView view, int value) {
        view.setLoopCount(value);
    }

    @ReactProp(name = "resizeMode")
    public void setResizeMode(ApngView view, @Nullable String value) {
        view.setResizeMode(value);
    }
}
