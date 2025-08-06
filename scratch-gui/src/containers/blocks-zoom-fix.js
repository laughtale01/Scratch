// Utility to create custom zoom controls with inline SVG that don't have opacity issues

export const createCustomZoomControls = (ScratchBlocks, flyoutWorkspace) => {
    // Create zoom control container
    const zoomGroup = ScratchBlocks.utils.createSvgElement('g', {
        'class': 'blocklyZoom',
        'opacity': '1'
    }, null);
    
    // Helper to create inline SVG zoom buttons
    const createZoomButton = (y, type, handler) => {
        // Create button group
        const buttonGroup = ScratchBlocks.utils.createSvgElement('g', {
            'transform': `translate(0, ${y})`,
            'style': 'cursor: pointer;'
        }, zoomGroup);
        
        // Create background circle
        const bgCircle = ScratchBlocks.utils.createSvgElement('circle', {
            'cx': 18,
            'cy': 18,
            'r': 18,
            'fill': '#ffffff',
            'stroke': '#e0e0e0',
            'stroke-width': '1'
        }, buttonGroup);
        
        // Create icon based on type
        if (type === 'in') {
            // Plus sign for zoom in
            const plus = ScratchBlocks.utils.createSvgElement('g', {
                'fill': '#575e75',
                'stroke': '#575e75',
                'stroke-width': '2',
                'stroke-linecap': 'round'
            }, buttonGroup);
            
            // Horizontal line
            ScratchBlocks.utils.createSvgElement('line', {
                'x1': 13,
                'y1': 18,
                'x2': 23,
                'y2': 18
            }, plus);
            
            // Vertical line
            ScratchBlocks.utils.createSvgElement('line', {
                'x1': 18,
                'y1': 13,
                'x2': 18,
                'y2': 23
            }, plus);
        } else if (type === 'out') {
            // Minus sign for zoom out
            ScratchBlocks.utils.createSvgElement('line', {
                'x1': 13,
                'y1': 18,
                'x2': 23,
                'y2': 18,
                'stroke': '#575e75',
                'stroke-width': '2',
                'stroke-linecap': 'round'
            }, buttonGroup);
        } else if (type === 'reset') {
            // Circle for reset
            ScratchBlocks.utils.createSvgElement('circle', {
                'cx': 18,
                'cy': 18,
                'r': 5,
                'fill': 'none',
                'stroke': '#575e75',
                'stroke-width': '2'
            }, buttonGroup);
        }
        
        // Add hover effect
        buttonGroup.addEventListener('mouseenter', () => {
            bgCircle.setAttribute('fill', '#f0f0f0');
        });
        
        buttonGroup.addEventListener('mouseleave', () => {
            bgCircle.setAttribute('fill', '#ffffff');
        });
        
        // Add click handler
        const clickHandler = (e) => {
            e.stopPropagation();
            e.preventDefault();
            
            // Visual feedback
            bgCircle.setAttribute('fill', '#e0e0e0');
            setTimeout(() => {
                bgCircle.setAttribute('fill', '#ffffff');
            }, 100);
            
            handler();
        };
        
        // Bind events
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
    createZoomButton(0, 'in', () => {
        if (flyoutWorkspace) {
            flyoutWorkspace.zoomCenter(1);
        }
    });
    
    createZoomButton(44, 'out', () => {
        if (flyoutWorkspace) {
            flyoutWorkspace.zoomCenter(-1);
        }
    });
    
    createZoomButton(88, 'reset', () => {
        if (flyoutWorkspace) {
            const startScale = flyoutWorkspace.options?.zoomOptions?.startScale || 1;
            flyoutWorkspace.setScale(startScale);
            flyoutWorkspace.scrollCenter();
        }
    });
    
    return zoomGroup;
};