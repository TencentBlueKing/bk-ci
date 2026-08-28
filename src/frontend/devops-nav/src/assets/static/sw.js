/* eslint-disable no-restricted-globals */
// Service Worker that prepends PUBLIC_URL_PREFIX to same-origin <img> requests
// missing the deployment subpath. The prefix is read from this script's own
// registration URL (e.g. sw.js?prefix=%2Fbk-ci) so we don't need any
// deploy-time placeholder substitution inside this file.

const params = new URL(self.location.href).searchParams
const PUBLIC_URL_PREFIX = params.get('prefix') || ''

self.addEventListener('install', () => {
    self.skipWaiting()
})

self.addEventListener('activate', (event) => {
    event.waitUntil(self.clients.claim())
})

self.addEventListener('fetch', (event) => {
    const req = event.request
    if (req.method !== 'GET' || req.destination !== 'image') return
    if (!PUBLIC_URL_PREFIX) return

    let url
    try {
        url = new URL(req.url)
    } catch (e) {
        return
    }

    if (url.origin !== self.location.origin) return

    const path = url.pathname
    if (!path.startsWith('/')) return
    if (path === PUBLIC_URL_PREFIX || path.startsWith(PUBLIC_URL_PREFIX + '/')) return

    url.pathname = PUBLIC_URL_PREFIX + path
    event.respondWith(fetch(new Request(url.toString(), req)))
})
