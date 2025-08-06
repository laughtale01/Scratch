// Create zoom controls using the original Blockly design with embedded SVG to fix opacity

export const createOriginalZoomControls = (ScratchBlocks, flyoutWorkspace) => {
    // Create zoom control container
    const zoomGroup = ScratchBlocks.utils.createSvgElement('g', {
        'class': 'blocklyZoom'
    }, null);
    
    // SVG content for each button (extracted from original files)
    const zoomInSVG = `
        <circle fill="#231f20" opacity="0.15" cx="18" cy="18" r="18"/>
        <circle fill="#fff" cx="18" cy="18" r="16"/>
        <g opacity="1">
            <circle fill="none" stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" cx="18" cy="18" r="7"/>
            <line stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" x1="23" y1="23" x2="26" y2="26"/>
            <line stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" x1="16" y1="18" x2="20" y2="18"/>
            <line stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" x1="18" y1="16" x2="18" y2="20"/>
        </g>
    `;
    
    const zoomOutSVG = `
        <circle fill="#231f20" opacity="0.15" cx="18" cy="18" r="18"/>
        <circle fill="#fff" cx="18" cy="18" r="16"/>
        <g opacity="1">
            <circle fill="none" stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" cx="18" cy="18" r="7"/>
            <line stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" x1="23" y1="23" x2="26" y2="26"/>
            <line stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" x1="16" y1="18" x2="20" y2="18"/>
        </g>
    `;
    
    const zoomResetSVG = `
        <circle fill="#231f20" opacity="0.15" cx="18" cy="18" r="18"/>
        <circle fill="#fff" cx="18" cy="18" r="16"/>
        <g opacity="1">
            <path fill="none" stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M22.68,11.32a7,7,0,0,1,0,9.9l-4.95,4.95a7,7,0,0,1-9.9,0"/>
            <polyline fill="none" stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" points="19 11.13 22.87 11.13 22.87 15"/>
        </g>
    `;
    
    // Helper to create button with embedded SVG
    const createZoomButton = (y, svgContent, handler) => {
        // Create a group for the button
        const buttonGroup = ScratchBlocks.utils.createSvgElement('g', {
            'transform': `translate(0, ${y})`,
            'style': 'cursor: pointer;'
        }, zoomGroup);
        
        // Create SVG element
        const svgElement = ScratchBlocks.utils.createSvgElement('svg', {
            'width': 36,
            'height': 36,
            'viewBox': '0 0 36 36'
        }, buttonGroup);
        
        // Insert SVG content
        svgElement.innerHTML = svgContent;
        
        // Force full opacity on the icon group
        const iconGroup = svgElement.querySelector('g');
        if (iconGroup) {
            iconGroup.setAttribute('opacity', '1');
            iconGroup.style.opacity = '1';
        }
        
        // Add click handler
        const clickHandler = (e) => {
            e.stopPropagation();
            e.preventDefault();
            
            // Mark workspace as focused
            if (flyoutWorkspace && flyoutWorkspace.markFocused) {
                flyoutWorkspace.markFocused();
            }
            
            // Clear touch identifier
            if (ScratchBlocks.Touch && ScratchBlocks.Touch.clearTouchIdentifier) {
                ScratchBlocks.Touch.clearTouchIdentifier();
            }
            
            handler();
        };
        
        // Bind events using both methods
        buttonGroup.addEventListener('click', clickHandler);
        buttonGroup.addEventListener('mousedown', (e) => {
            e.stopPropagation();
            e.preventDefault();
        });
        
        // Also use Blockly's event binding
        ScratchBlocks.bindEventWithChecks_(buttonGroup, 'mousedown', null, clickHandler);
        
        return buttonGroup;
    };
    
    // Create the three zoom buttons
    createZoomButton(0, zoomInSVG, () => {
        if (flyoutWorkspace) {
            flyoutWorkspace.zoomCenter(1);
        }
    });
    
    createZoomButton(44, zoomOutSVG, () => {
        if (flyoutWorkspace) {
            flyoutWorkspace.zoomCenter(-1);
        }
    });
    
    createZoomButton(88, zoomResetSVG, () => {
        if (flyoutWorkspace) {
            const startScale = flyoutWorkspace.options?.zoomOptions?.startScale || 1;
            flyoutWorkspace.setScale(startScale);
            flyoutWorkspace.scrollCenter();
        }
    });
    
    return zoomGroup;
};