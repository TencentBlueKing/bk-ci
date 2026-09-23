/**
 * Register a Service Worker that prepends PUBLIC_URL_PREFIX to same-origin
 * <img> requests missing the deployment subpath.
 *
 * The SW file is served from `<PREFIX>/console/static/sw.js`. We request
 * `scope: <PREFIX>/` so it can intercept same-origin requests from pages
 * under the deployed app prefix (for example `<PREFIX>/ms` and
 * `<PREFIX>/console`). For this to work, the static server (nginx)
 * must respond with the header `Service-Worker-Allowed` for that URL.
 *
 * No-op when:
 * - Service Workers are unavailable.
 * - PUBLIC_URL_PREFIX is empty (e.g. local dev), so there's nothing to add.
 */
export function registerImgServiceWorker (): void {
    if (typeof navigator === 'undefined' || !('serviceWorker' in navigator)) {
        console.info('[img-sw] skip register: serviceWorker is unavailable in current environment')
        return
    }

    const prefix = window.PUBLIC_URL_PREFIX || ''

    const swUrl = `${prefix}/console/static/sw.js?prefix=${encodeURIComponent(prefix)}`
    const scope = `${prefix}/`

    navigator.serviceWorker
        .register(swUrl, { scope })
        .then((registration) => {
            console.info('[img-sw] registered', {
                scope: registration.scope,
                scriptURL: swUrl
            })
        })
        .catch((e) => {
            console.warn('[img-sw] register failed', e)
        })
}
