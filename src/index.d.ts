import React from 'react';
import { ViewProps } from 'react-native';

export interface ApngSource {
  uri?: string;
  cacheKey?: string;
}

/**
 * ApngView - A React Native component for playing APNG and AVIF animations
 * 
 * Supported formats:
 * - APNG (Animated PNG)
 * - AVIF (AV1 Image File Format) - animated sequences
 * 
 * The component automatically detects the file type based on the file extension.
 */
export interface ApngViewProps extends ViewProps {
  /**
   * Source of the animation file (APNG or AVIF).
   * Can be a require() result, an object with uri, or a string URI.
   * 
   * Supported file extensions: .apng, .png, .avif
   */
  source: ApngSource | number | string;

  /**
   * Whether to start playing automatically.
   * @default true
   */
  autoPlay?: boolean;

  /**
   * Whether the animation is paused.
   * @default false
   */
  paused?: boolean;

  /**
   * Whether to loop the animation.
   * @default true
   */
  loop?: boolean;

  /**
   * Number of times to loop (0 = infinite).
   * Only used when loop is false.
   * @default 0
   */
  loopCount?: number;

  /**
   * How to resize the image.
   * @default 'contain'
   */
  resizeMode?: 'contain' | 'cover' | 'stretch' | 'center';

  /**
   * Optional cache key for the source.
   */
  cacheKey?: string;
}

export const ApngView: React.ForwardRefExoticComponent<
  ApngViewProps & React.RefAttributes<any>
>;

export default ApngView;

/** The interaction states a glow sheet can hold, in the order its rows are written. */
export type GlowSheetState = 'default' | 'hover' | 'press';

export const STATE_ORDER: GlowSheetState[];

/**
 * Padding around the button on every side for the glow to bleed into. Mirrors
 * `GLOW_CANVAS_MARGIN` in react-native-animated-glow.
 */
export const DEFAULT_GLOW_MARGIN: number;

export interface GlowSheetLayout {
  rows: number;
  margin: number;
  /** The button itself. */
  content: { width: number; height: number };
  /** One row: the content plus the margin on each side. */
  frame: { width: number; height: number };
  /** The whole image, one frame per state. */
  sheet: { width: number; height: number };
}

export function glowSheetLayout(
  content: { width: number; height: number },
  states: GlowSheetState[],
  margin?: number,
): GlowSheetLayout;

/**
 * Which row to show for a requested state, falling back down the interaction order when the
 * sheet does not hold it: a sheet without `press` shows `hover` when pressed.
 */
export function rowForState(states: GlowSheetState[], state: GlowSheetState): number;

/** How far up to slide the sheet so `row` fills the window. Negative. */
export function rowOffset(layout: GlowSheetLayout, row: number): number;

export interface GlowButtonProps extends ViewProps {
  /** The stacked animation. Same source shapes as `ApngView`. */
  source: ApngSource | number | string;

  /**
   * The rows the file holds, in order. The file cannot report this itself — three rows of 290px
   * look exactly like one row of 870 — so it has to be stated.
   *
   * @default ['default']
   */
  states?: GlowSheetState[];

  /** The button's own size, excluding the glow margin. */
  content: { width: number; height: number };

  /**
   * Padding baked into each row of the file.
   *
   * @default DEFAULT_GLOW_MARGIN
   */
  margin?: number;

  /**
   * Identifies the file's CONTENTS rather than its location, for a file that may be rewritten at
   * the same path. Without it a second export of the same states keeps showing the first.
   */
  cacheKey?: string;

  /** Pins the row shown, ignoring the pointer. */
  activeState?: GlowSheetState;

  onPress?: () => void;
  disabled?: boolean;
  paused?: boolean;
  children?: React.ReactNode;
}

/**
 * A button whose glow is a pre-rendered animation: one APNG holding every interaction state as a
 * row, with the row matching the pointer shown and the rest clipped away.
 *
 * The button occupies its content box in layout and the glow spills outside it, exactly as a live
 * glow does.
 */
export const GlowButton: React.FC<GlowButtonProps>;
