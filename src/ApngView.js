import React, { useMemo } from 'react';
import { Platform, Image, requireNativeComponent } from 'react-native';

const NativeApngView = Platform.OS !== 'web'
  ? requireNativeComponent('ApngView')
  : null;

/**
 * ApngView - A React Native component for playing APNG and AVIF animations
 * 
 * Supported formats:
 * - APNG (Animated PNG)
 * - AVIF (AV1 Image File Format) - animated sequences
 * 
 * The component automatically detects the file type based on the file extension.
 */
export const ApngView = React.forwardRef((props, ref) => {
  const {
    source,
    autoPlay = true,
    paused = false,
    loop = true,
    loopCount = 0,
    resizeMode = 'contain',
    cacheKey,
    style,
    ...rest
  } = props;

  // Resolve source to a format the native module understands
  const resolvedSource = useMemo(() => {
    if (!source) return null;

    // Handle require() result (number)
    if (typeof source === 'number') {
      const resolved = Image.resolveAssetSource(source);
      if (resolved) {
        return {
          uri: resolved.uri,
          cacheKey: cacheKey || resolved.uri,
        };
      }
      return null;
    }

    // Handle object with uri
    if (typeof source === 'object' && source.uri) {
      return {
        uri: source.uri,
        cacheKey: cacheKey || source.cacheKey || source.uri,
      };
    }

    // Handle string URI directly
    if (typeof source === 'string') {
      return {
        uri: source,
        cacheKey: cacheKey || source,
      };
    }

    return null;
  }, [source, cacheKey]);

  if (Platform.OS === 'web') {
    // Web fallback - just use img tag for APNG (browsers support it natively)
    const uri = resolvedSource?.uri;
    if (!uri) return null;

    return (
      <img
        ref={ref}
        src={uri}
        style={{
          objectFit: resizeMode === 'cover' ? 'cover' : 
                     resizeMode === 'stretch' ? 'fill' : 
                     resizeMode === 'center' ? 'none' : 'contain',
          // Flattened, because an RN style may be an array or a registered id and a DOM
          // element takes neither. `GlowButton` passes a plain object, but a caller need not.
          ...(Array.isArray(style) ? Object.assign({}, ...style.filter(Boolean)) : style),
        }}
        {...rest}
      />
    );
  }

  if (!NativeApngView) {
    console.warn('ApngView: Native module not available');
    return null;
  }

  return (
    <NativeApngView
      ref={ref}
      source={resolvedSource}
      autoPlay={autoPlay}
      paused={paused}
      loopCount={loop ? 0 : loopCount}
      resizeMode={resizeMode}
      style={style}
      {...rest}
    />
  );
});

ApngView.displayName = 'ApngView';

export default ApngView;
