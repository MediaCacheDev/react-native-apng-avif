/**
 * The arithmetic behind a multi-state glow sheet.
 *
 * A glow export can hold several interaction states in one animation, stacked one above another:
 * every frame of the file is a tall image whose rows are `default`, `hover`, `press` in that
 * order, each row being the button plus the margin its glow bleeds into. Showing one state means
 * showing one row and clipping the rest.
 *
 * Kept separate from the component, and free of React, because it is the half worth checking:
 * the layout is a contract with whatever produced the file, and getting a row offset wrong shows
 * hover when you meant press.
 */

/** The order rows are written in. Must match the producer's. */
export const STATE_ORDER = ['default', 'hover', 'press'];

/**
 * Padding around the button on every side, for the glow to spill into.
 *
 * Mirrors `GLOW_CANVAS_MARGIN` in react-native-animated-glow. A sheet captured with a different
 * margin must say so, or the label will sit off-centre by the difference.
 */
export const DEFAULT_GLOW_MARGIN = 100;

/**
 * Which row to show for a requested state, given the rows the file actually has.
 *
 * Falls back down the interaction order rather than to nothing: a sheet with no `press` row shows
 * `hover` when pressed, and one with neither shows `default`. A button whose glow simply stopped
 * responding would be worse than one that responds less.
 */
export const rowForState = (states, state) => {
    if (!Array.isArray(states) || states.length === 0) return 0;

    const wanted = STATE_ORDER.indexOf(state);
    // Unknown state: treat it as rest rather than guessing.
    if (wanted < 0) return 0;

    for (let index = wanted; index >= 0; index -= 1) {
        const found = states.indexOf(STATE_ORDER[index]);
        if (found >= 0) return found;
    }

    return 0;
};

/**
 * Measures everything the component needs from the button's own size.
 *
 * `content` is what a caller thinks in — the size of the button — and everything else follows
 * from it. The row is the content plus a margin on each side; the sheet is one row per state.
 */
export const glowSheetLayout = (content, states, margin = DEFAULT_GLOW_MARGIN) => {
    const rows = Math.max(1, Array.isArray(states) ? states.length : 1);
    const frame = {
        width: content.width + margin * 2,
        height: content.height + margin * 2,
    };

    return {
        rows,
        margin,
        content: { width: content.width, height: content.height },
        frame,
        /** The whole image, as it must be drawn for one row of it to be the right size. */
        sheet: { width: frame.width, height: frame.height * rows },
    };
};

/**
 * How far up to slide the sheet so that `row` is the one in the window.
 *
 * Negative by construction: row 0 sits at the top and every later row is above the window's top
 * edge until it is pulled down to it.
 */
export const rowOffset = (layout, row) => -layout.frame.height * Math.max(0, row);
