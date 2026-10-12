# react-native-apng-avif

Animated **APNG** and **AVIF** with full alpha transparency, in React Native on Android, iOS and
Web. One component, `AnimatedImageView`, plays both formats. It reads the format from the file
itself, so URLs without an extension work too.

| Platform | APNG | AVIF |
|---|---|---|
| Android | [APNG4Android](https://github.com/penfeizhou/APNG4Android) | APNG4Android + libavif |
| iOS | [SDWebImage](https://github.com/SDWebImage/SDWebImage) (ImageIO) | [SDWebImageAVIFCoder](https://github.com/SDWebImage/SDWebImageAVIFCoder) + libavif (dav1d) |
| Web | Browser `<img>` | Browser `<img>` |

## Installation

```bash
npm install react-native-apng-avif
```

### iOS

The iOS code is Swift, and it imports Objective-C pods that don't define modules by themselves.
Add these lines to your app's `Podfile`, inside the target:

```ruby
pod 'SDWebImage', :modular_headers => true
pod 'SDWebImageAVIFCoder', :modular_headers => true
pod 'libavif/libdav1d', :modular_headers => true
```

Then run `cd ios && pod install`. Name the `libdav1d` subspec rather than plain `libavif`: the
plain pod pulls in libaom, a larger library that also includes an encoder.

### Android

No setup needed. Autolinking registers the package.

## Usage

```tsx
import { AnimatedImageView } from 'react-native-apng-avif';

<AnimatedImageView
  source={{ uri: 'https://example.com/glow.avif' }}
  style={{ width: 200, height: 200 }}
/>

<AnimatedImageView
  source={{ uri: 'asset://media/glow.apng' }}
  style={{ width: 200, height: 200 }}
  onLoad={(e) => console.log(e.nativeEvent.width, e.nativeEvent.height)}
  onError={(e) => console.warn(e.nativeEvent.message)}
/>
```

### Sources

| Form | Android | iOS | Web |
|---|---|---|---|
| `https://…`, `http://…` | Downloaded once into the cache dir | SDWebImage (memory + disk cache) | `<img src>` |
| `asset://path/file.apng` | Streamed from the APK's `assets/` folder | `<main bundle>/path/file.apng` | From the site root: `/path/file.apng` |
| `file:///abs/path`, `/abs/path` | Read directly | Read directly | n/a |
| `content://…` | `ContentResolver` | n/a | n/a |
| `require('./file.apng')` | Works in debug and release | Works in debug and release | Works if your bundler emits a URL for it |

Nothing is copied: bundled files are read in place.

Remote files are cached by URL. Pass `cacheKey` when the URL doesn't identify the contents, for
example a presigned URL whose query string changes on every request, or a file rewritten at the
same URL.

For `require()`, add the extensions to Metro's `assetExts`
(`resolver.assetExts: [...defaults, 'apng', 'avif']`). Prefer the `.apng` extension over `.png`
for animated PNGs. On Android, Metro puts `.png` files in `res/drawable`, where the release build's
PNG optimisation may re-encode them and drop the animation.

### Props

| Prop | Type | Default | |
|---|---|---|---|
| `source` | `{ uri, cacheKey? }` \| `string` \| `require()` | required | |
| `autoPlay` | `boolean` | `true` | |
| `paused` | `boolean` | `false` | |
| `loop` | `boolean` | `true` | |
| `loopCount` | `number` | `0` | Plays when `loop` is false; `0` plays forever. |
| `resizeMode` | `'contain' \| 'cover' \| 'stretch' \| 'center'` | `'contain'` | |
| `cacheKey` | `string` | | Identifies a remote file's contents. |
| `onLoad` | `(e) => void` | | `e.nativeEvent`: `{ width, height }`, in pixels. |
| `onError` | `(e) => void` | | `e.nativeEvent`: `{ message }`. |

It also accepts the usual `View` props (`style`, `testID`, `accessibilityLabel`, …).

`ApngView` still works as a deprecated alias of `AnimatedImageView`.

### Web limitations

The browser's `<img>` has no playback control, so on web:

- `paused` and `autoPlay={false}` have no effect;
- `loop={false}` / `loopCount` follow the loop count stored in the file.

Animated AVIF needs a recent browser; see [caniuse](https://caniuse.com/avif).

## Glow buttons

To make a hover/press glow button from an APNG or AVIF sheet, use
[alpha-glow-button](https://github.com/danielkpl2/alpha-glow-button): `import { GlowButton } from
'alpha-glow-button/apng-avif'`.

## License

MIT. See [NOTICE](NOTICE) for the libraries this package depends on.
