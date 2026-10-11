# react-native-apng-player

React Native module for playing **APNG** (Animated PNG) and **AVIF** (AV1 Image File Format) animations on iOS, Android, and Web.

## Features

- Play APNG and AVIF animations with full alpha transparency
- Support for local files, bundled assets, and remote URLs
- Control playback (autoPlay, paused, loop)
- Resize modes (contain, cover, stretch, center)
- Automatic format detection based on file extension

## Supported Formats

| Format | Extension | Description |
|--------|-----------|-------------|
| APNG | `.apng`, `.png` | Animated PNG - lossless, good for faint effects |
| AVIF | `.avif` | AV1 Image Format - excellent compression with alpha |

## Installation

```bash
npm install react-native-apng-player
cd ios && pod install
```

## Usage

```tsx
import { ApngView } from 'react-native-apng-player';

// APNG animation
<ApngView
  source={{ uri: 'https://example.com/animation.apng' }}
  style={{ width: 200, height: 200 }}
  autoPlay={true}
  loop={true}
/>

// AVIF animation (same component, auto-detected)
<ApngView
  source={{ uri: 'https://example.com/animation.avif' }}
  style={{ width: 200, height: 200 }}
  autoPlay={true}
  loop={true}
/>
```

## Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| source | `{ uri?: string }` | required | Source of the animation file (APNG or AVIF) |
| autoPlay | `boolean` | `true` | Start playing automatically |
| paused | `boolean` | `false` | Pause the animation |
| loop | `boolean` | `true` | Loop the animation |
| loopCount | `number` | `0` | Number of loops (0 = infinite) |
| resizeMode | `'contain' \| 'cover' \| 'stretch' \| 'center'` | `'contain'` | How to resize the image |
| cacheKey | `string` | - | Optional cache key for the source |

## GlowButton

A button whose glow is a pre-rendered animation rather than a live shader.

`react-native-animated-glow`'s studio can export several interaction states into **one** APNG,
stacked as rows: `default`, `hover`, `press`, in that order. `GlowButton` shows the row matching
what the pointer is doing and clips the rest, so hover and press cost no extra file, no extra
decoder, and no Skia at all.

```tsx
import { GlowButton } from 'react-native-apng-player';

<GlowButton
  source={{ uri: 'file:///.../glow-30fps-default-hover-press.apng' }}
  states={['default', 'hover', 'press']}
  content={{ width: 220, height: 90 }}
  onPress={submit}
>
  <Text style={{ color: '#fff' }}>Submit</Text>
</GlowButton>
```

### How the sheet is laid out

Each row is the button **plus the margin its glow bleeds into** — 100px a side by default,
matching `GLOW_CANVAS_MARGIN` in the glow library. So a 220×90 button exports rows of 420×290, and
a three-state sheet is 420×870.

The button occupies its `content` box in layout and the glow spills outside it, exactly as a live
glow does. The visible window is the padded row rather than the content box, because clipping to
the content would cut the glow off at the button's edge.

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| source | `{ uri }` \| `number` \| `string` | required | The stacked animation |
| states | `('default'\|'hover'\|'press')[]` | `['default']` | The rows the file holds, in order. The file cannot report this itself |
| content | `{ width, height }` | required | The button's own size, excluding the glow margin |
| margin | `number` | `100` | Padding baked into each row |
| cacheKey | `string` | - | Identifies the file's **contents**. Needed when a file is rewritten at the same path, or the first version keeps showing |
| activeState | `'default'\|'hover'\|'press'` | - | Pins the row shown, ignoring the pointer |
| onPress | `() => void` | - | |
| disabled | `boolean` | `false` | Also holds the row at `default` |
| paused | `boolean` | `false` | |

A state the sheet does not hold falls back down the interaction order: without a `press` row, a
press shows `hover`; without either, `default`. A button that stopped responding altogether would
be worse than one that responds less.

The geometry is exported too — `glowSheetLayout`, `rowForState`, `rowOffset`, `STATE_ORDER`,
`DEFAULT_GLOW_MARGIN` — for anyone laying a sheet out by hand.

## Supported Source Types

- **Remote URL**: `{ uri: 'https://example.com/file.apng' }` or `{ uri: 'https://example.com/file.avif' }`
- **Local file**: `{ uri: 'file:///path/to/file.apng' }` or `{ uri: 'file:///path/to/file.avif' }`
- **Bundled asset**: `{ uri: 'asset://path/to/file.apng' }` or `require('./file.apng')`

## Platform Support

| Platform | APNG | AVIF |
|----------|------|------|
| Android | ✅ via penfeizhou/APNG4Android | ✅ via penfeizhou/APNG4Android |
| iOS | ✅ via SDWebImage | ✅ via SDWebImageAVIFCoder |
| Web | ✅ Native browser support | ✅ Native browser support (Chrome 85+, Firefox 93+)
