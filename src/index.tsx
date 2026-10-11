import { AnimatedImageView } from './AnimatedImageView';
import type { AnimatedImageViewProps, AnimatedImageSource } from './AnimatedImageView';

export {
  AnimatedImageView,
  type AnimatedImageViewProps,
  type AnimatedImageViewRef,
  type AnimatedImageSource,
  type AnimatedImageResizeMode,
  type AnimatedImageLoadEvent,
  type AnimatedImageErrorEvent,
} from './AnimatedImageView';

/** @deprecated Renamed to `AnimatedImageView`, since it plays AVIF as well. */
export const ApngView: typeof AnimatedImageView = AnimatedImageView;
/** @deprecated Renamed to `AnimatedImageViewProps`. */
export type ApngViewProps = AnimatedImageViewProps;
/** @deprecated Renamed to `AnimatedImageSource`. */
export type ApngSource = AnimatedImageSource;

export { default } from './AnimatedImageView';
