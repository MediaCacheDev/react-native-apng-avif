import React, { forwardRef, useMemo } from 'react';
import { Image, Platform, StyleSheet } from 'react-native';
import type { ViewProps } from 'react-native';
import NativeApngAvifView from './ApngAvifViewNativeComponent';

/**
 * Where the animation comes from.
 *
 * - `https://…` / `http://…`: downloaded once and cached, under `cacheKey` when given.
 * - `asset://path/file.apng`: the app's bundled files — Android APK `assets/`, the iOS main bundle,
 *   or `/path/file.apng` from the site root on web. Read in place, never copied.
 * - `file:///abs/path` or `/abs/path`.
 * - `require('./file.apng')`, in debug and release builds.
 */
export type AnimatedImageSource = { uri: string; cacheKey?: string } | number | string;

export type AnimatedImageResizeMode = 'contain' | 'cover' | 'stretch' | 'center';

export interface AnimatedImageLoadEvent {
  /** Intrinsic size of the animation, in pixels. */
  width: number;
  height: number;
}

export interface AnimatedImageErrorEvent {
  message: string;
}

export interface AnimatedImageViewProps extends ViewProps {
  /** An APNG or AVIF. The format is read from the file itself, not from its extension. */
  source: AnimatedImageSource;
  /** @default true */
  autoPlay?: boolean;
  /** @default false */
  paused?: boolean;
  /** @default true */
  loop?: boolean;
  /**
   * How many times to play when `loop` is false; 0 plays forever.
   *
   * @default 0
   */
  loopCount?: number;
  /** @default 'contain' */
  resizeMode?: AnimatedImageResizeMode;
  /**
   * Identifies a remote file's CONTENTS rather than its location. Use it for presigned URLs, whose
   * query string changes on every request, and for files rewritten at the same URL.
   */
  cacheKey?: string;
  onLoad?: (event: { nativeEvent: AnimatedImageLoadEvent }) => void;
  onError?: (event: { nativeEvent: AnimatedImageErrorEvent }) => void;
}

interface NativeSource {
  uri: string;
  cacheKey?: string;
}

export type AnimatedImageViewRef = React.ElementRef<typeof NativeApngAvifView>;

// From the codegen spec: a Fabric component on the new architecture, a view manager on the old
// one. Null on the web.
const NativeAnimatedImageView = Platform.OS === 'web' ? null : NativeApngAvifView;

const OBJECT_FIT: Record<AnimatedImageResizeMode, 'contain' | 'cover' | 'fill' | 'none'> = {
  contain: 'contain',
  cover: 'cover',
  stretch: 'fill',
  center: 'none',
};

const resolveSource = (
  source: AnimatedImageSource,
  cacheKey: string | undefined,
): NativeSource | null => {
  if (typeof source === 'number') {
    // react-native-web has no resolveAssetSource; its bundler hands over a URL string instead.
    const resolved =
      typeof Image.resolveAssetSource === 'function' ? Image.resolveAssetSource(source) : null;
    return resolved?.uri ? { uri: resolved.uri, cacheKey } : null;
  }
  if (typeof source === 'string') {
    return source ? { uri: source, cacheKey } : null;
  }
  return source?.uri ? { uri: source.uri, cacheKey: cacheKey ?? source.cacheKey } : null;
};

/** On the web, asset://path is served from the site root. */
const toWebUrl = (uri: string): string =>
  uri.startsWith('asset://') ? '/' + uri.slice('asset://'.length).replace(/^\/+/, '') : uri;

/**
 * Plays an animated APNG or AVIF, with alpha.
 *
 * Android decodes with APNG4Android, iOS with SDWebImage (plus libavif for AVIF), and web hands
 * the file to the browser through `<img>`.
 */
export const AnimatedImageView = forwardRef<AnimatedImageViewRef, AnimatedImageViewProps>(
  (
    {
      source,
      autoPlay = true,
      paused = false,
      loop = true,
      loopCount = 0,
      resizeMode = 'contain',
      cacheKey,
      onLoad,
      onError,
      style,
      ...rest
    },
    ref,
  ) => {
    const resolved = useMemo(() => resolveSource(source, cacheKey), [source, cacheKey]);

    if (Platform.OS === 'web') {
      if (!resolved) return null;
      // `<img>` can't pause or count loops: `paused`, `autoPlay={false}` and `loopCount` have no
      // effect here. RN-only props are dropped rather than spread, as the DOM rejects them.
      return (
        <img
          ref={ref as unknown as React.Ref<HTMLImageElement>}
          src={toWebUrl(resolved.uri)}
          alt={rest.accessibilityLabel ?? ''}
          id={rest.nativeID}
          data-testid={rest.testID}
          draggable={false}
          onLoad={(event) =>
            onLoad?.({
              nativeEvent: {
                width: event.currentTarget.naturalWidth,
                height: event.currentTarget.naturalHeight,
              },
            })
          }
          onError={() => onError?.({ nativeEvent: { message: `Failed to load ${resolved.uri}` } })}
          style={{
            display: 'block',
            objectFit: OBJECT_FIT[resizeMode],
            ...(StyleSheet.flatten(style) as React.CSSProperties),
          }}
        />
      );
    }

    if (!NativeAnimatedImageView) return null;

    return (
      <NativeAnimatedImageView
        ref={ref}
        source={resolved}
        autoPlay={autoPlay}
        paused={paused}
        loopCount={loop ? 0 : loopCount}
        resizeMode={resizeMode}
        onLoad={onLoad}
        onError={onError}
        style={style}
        {...rest}
      />
    );
  },
);

AnimatedImageView.displayName = 'AnimatedImageView';
