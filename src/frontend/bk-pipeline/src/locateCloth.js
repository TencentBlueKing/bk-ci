export function hasLocateHighlight (container) {
    if (!container) return false
    if (container.locateHighlightActive) return true
    if ((container.elements || []).some(element => element.locateHighlightActive)) {
        return true
    }
    return (container.groupContainers || []).some(hasLocateHighlight)
}

export function findLocateHighlightTarget (container) {
    if (!container) return null

    if (container.locateHighlightActive) {
        const root = document.getElementById(String(container.id))
        return root?.querySelector('.container-title') || root || null
    }

    const activeElement = (container.elements || []).find(element => element.locateHighlightActive)
    if (activeElement) {
        return document.getElementById(String(activeElement.id))
    }

    for (const group of container.groupContainers || []) {
        const target = findLocateHighlightTarget(group)
        if (target) return target
    }

    return null
}
export function getLocateClothStyle (wrapEl, targetEl) {
    if (!wrapEl || !targetEl) return null

    const wrapRect = wrapEl.getBoundingClientRect()
    const targetRect = targetEl.getBoundingClientRect()
    const padding = 6

    return {
        top: `${targetRect.top - wrapRect.top - padding}px`,
        left: '0',
        width: '100%',
        height: `${targetRect.height + padding * 2}px`
    }
}
