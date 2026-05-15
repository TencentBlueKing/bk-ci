/**
 * Register a Service Worker that prepends PUBLIC_URL_PREFIX to same-origin
 * <img> requests missing the deployment subpath.
 *
 * The SW file is served from `<PREFIX>/console/static/sw.js`. We request
 * `scope: '/'` so it can intercept image requests anywhere on the origin
 * (not only under /console/). For this to work, the static server (nginx)
 * must respond with the header `Service-Worker-Allowed: /` for that URL.
 *
 * No-op when:
 * - Service Workers are unavailable.
 * - PUBLIC_URL_PREFIX is empty (e.g. local dev), so there's nothing to add.
 */
export function registerImgServiceWorker (): void {
    if (typeof navigator === 'undefined' || !('serviceWorker' in navigator)) return

    const prefix = window.PUBLIC_URL_PREFIX || ''
    if (!prefix) return

    const swUrl = `/console/static/sw.js?prefix=${encodeURIComponent(prefix)}`

    navigator.serviceWorker
        .register(swUrl, { scope: '/ms' })
        .catch((e) => {
            console.warn('[img-sw] register failed', e)
        })
}
