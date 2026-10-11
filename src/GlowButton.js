import React, { useMemo, useState } from 'react';
import { Pressable, View } from 'react-native';

import { ApngView } from './ApngView';
import { DEFAULT_GLOW_MARGIN, glowSheetLayout, rowForState, rowOffset } from './glowSheet';

/**
 * A button whose glow is a pre-rendered animation rather than a live shader.
 *
 * Takes one APNG holding every interaction state stacked in rows — what
 * react-native-animated-glow's studio exports when more than one state is chosen — and shows the
 * row matching what the pointer is doing. Hover and press therefore cost nothing at all: no
 * second file to fetch, no second decoder, and no Skia on the consuming side.
 *
 * The button occupies its CONTENT box in layout, and the glow spills outside it. That is the
 * whole point of the margin in the exported frame, and why the visible window is the padded row
 * rather than the content: clipping to the content box would cut the glow off at the button's
 * edge. Neighbouring rows cannot leak in, because each was captured clamped to its own canvas.
 */
export const GlowButton = ({
    source,
    /**
     * The rows the file holds, in order — normally `['default', 'hover', 'press']`, and whatever
     * subset was exported otherwise. The file cannot say this itself: three rows of 290px look
     * exactly like one row of 870.
     */
    states = ['default'],
    /** The button's own size, excluding the glow margin. */
    content,
    /** Padding baked into each row for the glow to bleed into. */
    margin = DEFAULT_GLOW_MARGIN,
    /**
     * Identifies the file's CONTENTS, not its location.
     *
     * `ApngView` caches by URI by default, which is wrong for a file that gets rewritten at the
     * same path — a second export of the same states would go on showing the first. Anything that
     * changes with the bytes will do; the glow studio's preview slot hands one over.
     */
    cacheKey,
    /** Pins the shown row, ignoring the pointer. Useful for previews and for tests. */
    activeState,
    onPress,
    disabled = false,
    paused = false,
    /** Applied to the content box, so a caller can add a border radius or a background. */
    style,
    children,
    ...rest
}) => {
    const [hovered, setHovered] = useState(false);
    const [pressed, setPressed] = useState(false);

    const layout = useMemo(
        () => glowSheetLayout(content, states, margin),
        [content, states, margin],
    );

    // Derived from the two booleans rather than tracked as one value: releasing a press while
    // still hovering has to land on hover, and a single "pointer state" gets that wrong
    // depending on which handler ran last.
    const pointerState = pressed ? 'press' : hovered ? 'hover' : 'default';
    const row = rowForState(states, activeState ?? (disabled ? 'default' : pointerState));

    return (
        <Pressable
            accessibilityRole="button"
            disabled={disabled}
            onPress={onPress}
            onPressIn={() => setPressed(true)}
            onPressOut={() => setPressed(false)}
            onHoverIn={() => setHovered(true)}
            onHoverOut={() => setHovered(false)}
            style={[
                {
                    width: layout.content.width,
                    height: layout.content.height,
                    alignItems: 'center',
                    justifyContent: 'center',
                },
                style,
            ]}
            {...rest}
        >
            {/*
              The window onto one row. Absolutely positioned and inset by the margin so the row
              lands centred on the content box, with the bleed hanging outside it — the button
              itself stays the size it claims to be in layout.

              `overflow: 'hidden'` is what hides the other states, and is required explicitly on
              Android, where children draw outside their parent by default.
            */}
            <View
                pointerEvents="none"
                style={{
                    position: 'absolute',
                    left: -layout.margin,
                    top: -layout.margin,
                    width: layout.frame.width,
                    height: layout.frame.height,
                    overflow: 'hidden',
                }}
            >
                <ApngView
                    source={source}
                    cacheKey={cacheKey}
                    paused={paused}
                    loop
                    // `stretch` with both dimensions given, rather than `contain`: the sheet is
                    // drawn at exactly the size the arithmetic says, so there is nothing to fit
                    // and nothing to letterbox. `contain` would silently shrink a sheet whose
                    // aspect ratio disagreed, moving every row off its offset.
                    resizeMode="stretch"
                    style={{
                        position: 'absolute',
                        left: 0,
                        // Slid by layout rather than by transform, so the parent's clip applies
                        // to it on every platform without depending on draw-time ordering.
                        top: rowOffset(layout, row),
                        width: layout.sheet.width,
                        height: layout.sheet.height,
                    }}
                />
            </View>

            {/*
              The label, over the glow. Later siblings paint on top in React Native, and the
              window above is `pointerEvents: 'none'`, so the press still belongs to the button.
            */}
            {children}
        </Pressable>
    );
};

GlowButton.displayName = 'GlowButton';

export default GlowButton;
