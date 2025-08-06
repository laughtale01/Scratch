# Flyout Zoom Controls Troubleshooting Guide

## Current Status

The flyout zoom controls feature has been implemented but is temporarily disabled due to crashes. This document provides instructions for enabling and troubleshooting the feature.

## How to Enable/Disable

1. Open `/src/containers/blocks.jsx`
2. Find the line `const ENABLE_FLYOUT_ZOOM = false;` (around line 119)
3. Change to `true` to enable, `false` to disable
4. Rebuild the project with `npm run build`

## Implementation Files

- **Main Implementation**: `/src/containers/flyout-zoom-safe.js`
  - Safe version with extensive error handling
  - Uses Blockly's native ZoomControls class
  - Positions controls in bottom-right of flyout

- **Container Integration**: `/src/containers/blocks.jsx`
  - Initializes flyout zoom controls in `componentDidMount`
  - Handles cleanup in `componentWillUnmount`
  - Updates position on resize events

- **Styles**: `/src/components/blocks/blocks.css`
  - Ensures zoom controls are fully opaque
  - Fixes SVG opacity inheritance issues
  - Adds flyout-specific styling

## Known Issues

1. **Crash on Load**: When enabled, may cause "Scratch has crashed" error
   - Solution: Set `ENABLE_FLYOUT_ZOOM = false`

2. **Position Outside Flyout**: Controls may appear outside the visible flyout area
   - Check flyout metrics calculation in `flyout-zoom-safe.js`
   - Verify flyout width (should be ~248px)

3. **Opacity Issues**: Controls appear faded/disabled
   - CSS overrides are in place but may need adjustment
   - Check if SVG files are loading correctly

## Debugging Steps

1. **Enable Console Logging**:
   - Open browser developer tools (F12)
   - Look for messages starting with "Flyout zoom controls:"
   - Check for any error messages

2. **Check SVG Files**:
   - Verify zoom SVG files exist in `build/static/blocks-media/default/`
   - Files needed: `zoom-in.svg`, `zoom-out.svg`, `zoom-reset.svg`

3. **Test Without Zoom Controls**:
   - Set `ENABLE_FLYOUT_ZOOM = false`
   - Rebuild and verify Scratch works normally

4. **Incremental Testing**:
   - Try the debug version: Change import to `flyout-zoom-debug.js`
   - This version logs detailed metrics information

## Alternative Approaches

If the current implementation continues to cause issues:

1. **Custom React Component**: Create a React-based zoom control component instead of using Blockly's SVG approach

2. **HTML Overlay**: Position HTML buttons over the flyout area instead of SVG elements

3. **Keyboard Shortcuts**: Add keyboard shortcuts for flyout zoom (e.g., Ctrl+Alt+Plus/Minus)

4. **Menu Option**: Add zoom controls to the context menu or settings

## Build and Test

```bash
# Build the project
npm run build

# Or run development server
npm start

# Access at:
# - Built version: file:///D:/minecraft_collaboration_project/scratch-gui/build/index.html
# - Dev server: http://localhost:8601
```

## Next Steps

1. Investigate the exact cause of the crash by checking browser console
2. Consider implementing a React-based alternative if SVG approach is unstable
3. Add unit tests for the zoom control functionality
4. Document the final working solution in CLAUDE.md

## Related Files for Reference

- Main workspace zoom implementation: `node_modules/scratch-blocks/core/zoom_controls.js`
- Flyout implementation: `node_modules/scratch-blocks/core/flyout_base.js`
- Workspace metrics: `node_modules/scratch-blocks/core/workspace_svg.js`