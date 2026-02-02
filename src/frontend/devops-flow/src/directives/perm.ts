/**
 * v-perm Directive
 * Permission control directive for Vue 3
 * Disables and styles elements without permission
 */

import type { Directive, DirectiveBinding } from 'vue';
import type { PermDirectiveValue } from '../components/Permission/types';

/**
 * CSS styles for disabled state
 */
const DISABLED_STYLES = {
  cursor: 'not-allowed',
  opacity: '0.6',
  pointerEvents: 'none',
} as const;

/**
 * Tooltip element for showing permission message
 */
let tooltipEl: HTMLDivElement | null = null;

/**
 * Create tooltip element
 */
const createTooltip = (): HTMLDivElement => {
  if (tooltipEl) return tooltipEl;

  tooltipEl = document.createElement('div');
  tooltipEl.style.cssText = `
    position: fixed;
    padding: 8px 12px;
    background-color: rgba(0, 0, 0, 0.8);
    color: #fff;
    font-size: 12px;
    border-radius: 4px;
    z-index: 9999;
    pointer-events: none;
    white-space: nowrap;
    display: none;
  `;
  document.body.appendChild(tooltipEl);
  return tooltipEl;
};

/**
 * Show tooltip with message
 */
const showTooltip = (event: MouseEvent, message: string) => {
  const tooltip = createTooltip();
  tooltip.textContent = message;
  tooltip.style.display = 'block';
  tooltip.style.left = `${event.clientX + 10}px`;
  tooltip.style.top = `${event.clientY + 10}px`;
};

/**
 * Hide tooltip
 */
const hideTooltip = () => {
  if (tooltipEl) {
    tooltipEl.style.display = 'none';
  }
};

/**
 * Update mouse position for tooltip
 */
const updateTooltipPosition = (event: MouseEvent) => {
  if (tooltipEl && tooltipEl.style.display === 'block') {
    tooltipEl.style.left = `${event.clientX + 10}px`;
    tooltipEl.style.top = `${event.clientY + 10}px`;
  }
};

/**
 * Apply disabled styles to element
 */
const applyDisabledStyles = (el: HTMLElement) => {
  Object.entries(DISABLED_STYLES).forEach(([key, value]) => {
    el.style[key as keyof typeof DISABLED_STYLES] = value;
  });
  el.classList.add('perm-disabled');
};

/**
 * Remove disabled styles from element
 */
const removeDisabledStyles = (el: HTMLElement) => {
  Object.keys(DISABLED_STYLES).forEach((key) => {
    el.style[key as keyof typeof DISABLED_STYLES] = '';
  });
  el.classList.remove('perm-disabled');
};

/**
 * Store for original event handlers
 */
const originalHandlersMap = new WeakMap<
  HTMLElement,
  {
    onClick?: EventListener;
    onMouseMove?: EventListener;
    onMouseLeave?: EventListener;
  }
>();

/**
 * Setup permission check and apply styles
 */
const setupPermissionControl = (
  el: HTMLElement,
  binding: DirectiveBinding<PermDirectiveValue>,
) => {
  const value = binding.value;
  if (!value) return;

  const { hasPermission = true, disablePermissionApi = true } = value;

  // If has permission, ensure element is enabled
  if (hasPermission) {
    removeDisabledStyles(el);
    const handlers = originalHandlersMap.get(el);
    if (handlers) {
      if (handlers.onMouseMove) {
        el.removeEventListener('mousemove', handlers.onMouseMove);
      }
      if (handlers.onMouseLeave) {
        el.removeEventListener('mouseleave', handlers.onMouseLeave);
      }
      originalHandlersMap.delete(el);
    }
    return;
  }

  // No permission - apply disabled styles
  applyDisabledStyles(el);

  // Add tooltip handlers
  const message = '没有操作权限'; // No operation permission

  const handleMouseMove = (event: MouseEvent) => {
    showTooltip(event, message);
    updateTooltipPosition(event);
  };

  const handleMouseLeave = () => {
    hideTooltip();
  };

  // Store original handlers
  originalHandlersMap.set(el, {
    onMouseMove: handleMouseMove as EventListener,
    onMouseLeave: handleMouseLeave as EventListener,
  });

  el.addEventListener('mousemove', handleMouseMove);
  el.addEventListener('mouseleave', handleMouseLeave);

  // Prevent click events
  el.addEventListener('click', (event) => {
    if (!hasPermission) {
      event.preventDefault();
      event.stopPropagation();
    }
  }, true);
};

/**
 * Cleanup permission control
 */
const cleanupPermissionControl = (el: HTMLElement) => {
  const handlers = originalHandlersMap.get(el);
  if (handlers) {
    if (handlers.onMouseMove) {
      el.removeEventListener('mousemove', handlers.onMouseMove);
    }
    if (handlers.onMouseLeave) {
      el.removeEventListener('mouseleave', handlers.onMouseLeave);
    }
    originalHandlersMap.delete(el);
  }
  removeDisabledStyles(el);
  hideTooltip();
};

/**
 * v-perm directive definition
 *
 * Usage:
 * <button v-perm="{ hasPermission: false }">Click me</button>
 * <button v-perm="{ hasPermission: true, disablePermissionApi: true }">Click me</button>
 */
export const vPerm: Directive<HTMLElement, PermDirectiveValue> = {
  mounted(el, binding) {
    setupPermissionControl(el, binding);
  },

  updated(el, binding) {
    // Only update if value changed
    if (binding.value?.hasPermission !== binding.oldValue?.hasPermission) {
      setupPermissionControl(el, binding);
    }
  },

  unmounted(el) {
    cleanupPermissionControl(el);
  },
};

export default vPerm;
