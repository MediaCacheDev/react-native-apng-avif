# Changelog

## 1.0.0

First public release.

- `AnimatedImageView` plays animated APNG and AVIF with alpha on Android (APNG4Android, with libavif for AVIF), iOS (SDWebImage, with SDWebImageAVIFCoder and libavif/dav1d for AVIF) and the web (`<img>`). The format is read from the file, so URLs without an extension work.
- Sources: `https://` (cached under `cacheKey`), `file://`, absolute paths, `asset://`, `require()`, and `content://` on Android. Bundled files are read in place.
- Props: `autoPlay`, `paused`, `loop`, `loopCount` and `resizeMode`. Events: `onLoad` with the pixel size, and `onError`.
- Works on both React Native architectures: the new one (Fabric) and the old one.
- On the web, `paused`, `autoPlay={false}` and the loop props have no effect, because `<img>` has no playback control.
