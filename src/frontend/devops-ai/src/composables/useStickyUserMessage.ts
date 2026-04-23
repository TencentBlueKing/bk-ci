import { watch, nextTick, onBeforeUnmount, type Ref } from 'vue'

const STICKY_CLASS = 'sticky-user-question'
const STUCK_CLASS = 'is-stuck'
const SENTINEL_CLASS = 'sticky-sentinel'
const CONTAINER_SELECTOR = '.ai-message-container'
const GROUP_SELECTOR = '.message-group'
const USER_MSG_SELECTOR = '.ai-user-message'

function collectUserGroups(container: HTMLElement): HTMLElement[] {
  const out: HTMLElement[] = []
  for (const group of container.querySelectorAll<HTMLElement>(GROUP_SELECTOR)) {
    if (group.querySelector(USER_MSG_SELECTOR)) {
      out.push(group)
    }
  }
  return out
}

/** Offset of element top within scrollable content (px). */
function contentOffsetTop(el: HTMLElement, container: HTMLElement): number {
  const e = el.getBoundingClientRect()
  const c = container.getBoundingClientRect()
  return e.top - c.top + container.scrollTop
}

/**
 * Only one user turn is sticky at a time: the last user message whose start has
 * scrolled to/past the top — i.e. the "current" history turn while reading.
 * No stacking / no cumulative top.
 */
function applyActiveSticky(container: HTMLElement) {
  const userGroups = collectUserGroups(container)
  if (userGroups.length === 0) return

  const st = container.scrollTop
  let activeIndex = -1
  for (let i = 0; i < userGroups.length; i++) {
    const y = contentOffsetTop(userGroups[i], container)
    if (y <= st + 1) {
      activeIndex = i
    }
  }

  const cRect = container.getBoundingClientRect()

  for (let i = 0; i < userGroups.length; i++) {
    const g = userGroups[i]
    if (i === activeIndex) {
      g.classList.add(STICKY_CLASS)
      g.style.top = ''
      const gr = g.getBoundingClientRect()
      const stuck = gr.top <= cRect.top + 1 && gr.bottom > cRect.top
      g.classList.toggle(STUCK_CLASS, stuck)
    } else {
      g.classList.remove(STICKY_CLASS, STUCK_CLASS)
      g.style.top = ''
    }
  }
}

export function useStickyUserMessage(
  messages: Ref<unknown[]>,
  chatLoading?: Ref<boolean>,
) {
  let removeLayoutListeners: (() => void) | null = null
  let layoutRaf = 0

  function teardownLayout() {
    if (layoutRaf) {
      cancelAnimationFrame(layoutRaf)
      layoutRaf = 0
    }
    removeLayoutListeners?.()
    removeLayoutListeners = null
  }

  function scheduleApply(container: HTMLElement) {
    if (layoutRaf) cancelAnimationFrame(layoutRaf)
    layoutRaf = requestAnimationFrame(() => {
      layoutRaf = 0
      applyActiveSticky(container)
    })
  }

  function bindLayoutHandlers(container: HTMLElement) {
    teardownLayout()
    const run = () => scheduleApply(container)
    container.addEventListener('scroll', run, { passive: true })
    window.addEventListener('resize', run)
    const ro = new ResizeObserver(run)
    ro.observe(container)
    removeLayoutListeners = () => {
      container.removeEventListener('scroll', run)
      window.removeEventListener('resize', run)
      ro.disconnect()
    }
    scheduleApply(container)
    requestAnimationFrame(() => scheduleApply(container))
  }

  function cleanup() {
    teardownLayout()
    document.querySelectorAll(`.${SENTINEL_CLASS}`).forEach(el => el.remove())
    document.querySelectorAll(`.${STICKY_CLASS}`).forEach(el => {
      const h = el as HTMLElement
      h.style.top = ''
      h.classList.remove(STICKY_CLASS, STUCK_CLASS)
    })
  }

  function update() {
    cleanup()

    const container = document.querySelector(CONTAINER_SELECTOR) as HTMLElement | null
    if (!container) return

    if (collectUserGroups(container).length === 0) return

    bindLayoutHandlers(container)
  }

  watch(
    () =>
      [messages.value.length, chatLoading?.value ?? false] as [number, boolean],
    ([len, loading]) => {
      if (len === 0) {
        nextTick(cleanup)
        return
      }
      if (loading) return
      nextTick(update)
    },
    { immediate: true },
  )

  onBeforeUnmount(cleanup)
}
