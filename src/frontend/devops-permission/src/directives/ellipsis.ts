import { DirectiveBinding, ObjectDirective } from 'vue';
import $bkPopover from 'bkui-vue/lib/plugin-popover';

const resolveOptions = (el: HTMLElement, binding: DirectiveBinding) => {
  const options: any = {
    content: '',
    target: el,
  };
  if (typeof binding.value === 'object') {
    Object.assign(options, binding.value);
  } else {
    options.content = binding.value;
  }

  return options;
};

export const createInstance = (el: HTMLElement, binding: any) => {
  let instance = null;
  let createTimer = null;
  let hidePopTimer = null;
  const options = resolveOptions(el, binding);
  const { disabled } = options;
  if (disabled || instance) {
    return;
  }

  const handleContentEnter = () => {
    hidePopTimer && clearTimeout(hidePopTimer);
    hidePopTimer = null;
  };

  const handleContentLeave = () => {
    if (createTimer) {
      clearTimeout(createTimer);
    }
    instance?.hide();
    instance?.close();
    instance = null;
  };

  const handleMouseEnter = () => {
    createTimer && clearTimeout(createTimer);
    createTimer = setTimeout(() => {
      if (el.clientWidth < el.scrollWidth) {
        const targetOptions = resolveOptions(el, binding);
        targetOptions.content = targetOptions.content || el.innerHTML;
        Object.assign(targetOptions, {
          onContentMouseenter: handleContentEnter,
          onContentMouseleave: handleContentLeave,
        });
        instance = $bkPopover(targetOptions);

        setTimeout(() => {
          instance.show();
        });
      }
    }, 100);
  };

  const handleMouseLeave = () => {
    hidePopTimer = setTimeout(() => {
      if (createTimer) {
        clearTimeout(createTimer);
      }
      instance?.hide();
      instance?.close();
      instance = null;
    }, 120);
  };

  el.addEventListener('mouseenter', handleMouseEnter);
  el.addEventListener('mouseleave', handleMouseLeave);

  Object.assign(binding, {
    __cached: {
      handleMouseEnter,
      handleMouseLeave,
    },
  });

  const destroyInstance = (element?: HTMLElement) => {
    handleMouseLeave();
    (element ?? el)?.removeEventListener('mouseenter', handleMouseEnter);
    (element ?? el)?.removeEventListener('mouseleave', handleMouseLeave);
  };

  return {
    destroyInstance,
    instance,
  };
};

const ellipsis: ObjectDirective = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    createInstance(el, binding);
  },
  beforeUnmount(el: HTMLElement, binding: any) {
    if (binding.__cached) {
      const { handleMouseEnter, handleMouseLeave } = binding.__cached;
      el.removeEventListener('mouseenter', handleMouseEnter);
      el.removeEventListener('mouseleave', handleMouseLeave);
      binding.__cached = null;
    }
  },
};

export default ellipsis;
