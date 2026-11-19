(function () {
    if (window.__urlPrefixInterceptorInstalled) {
        return
    }
    window.__urlPrefixInterceptorInstalled = true

    var SCHEME_RE = /^[a-z][a-z0-9+.-]*:/i
    var FULL_URL_RE = /^(?:https?:)?\/\//i

    function normalizePrefix (prefix) {
        if (typeof prefix !== 'string') {
            return ''
        }

        var normalized = prefix.trim()
        if (!normalized || normalized === '/' || normalized.indexOf('__') === 0) {
            return ''
        }

        if (normalized.charAt(0) !== '/') {
            normalized = '/' + normalized
        }

        return normalized.replace(/\/+$/, '')
    }

    function hasPrefix (path, prefix) {
        return path === prefix || path.indexOf(prefix + '/') === 0
    }

    function splitPath (url) {
        var hashIndex = url.indexOf('#')
        var queryIndex = url.indexOf('?')
        var endIndex = url.length

        if (hashIndex > -1) {
            endIndex = hashIndex
        }
        if (queryIndex > -1 && queryIndex < endIndex) {
            endIndex = queryIndex
        }

        return {
            path: url.slice(0, endIndex),
            suffix: url.slice(endIndex)
        }
    }

    function prefixSameOriginUrl (url) {
        var prefix = normalizePrefix(window.PUBLIC_URL_PREFIX)
        if (!prefix || typeof url !== 'string') {
            return url
        }

        var rawUrl = url.trim()
        if (
            !rawUrl
            || rawUrl.charAt(0) === '#'
            || rawUrl.charAt(0) === '?'
            || SCHEME_RE.test(rawUrl)
            || FULL_URL_RE.test(rawUrl)
        ) {
            return url
        }

        var parsed = splitPath(rawUrl)
        var path = parsed.path
        var suffix = parsed.suffix

        if (!path) {
            return url
        }

        if (path.indexOf('./') === 0 || path.indexOf('../') === 0) {
            try {
                var resolvedUrl = new URL(rawUrl, window.location.href)
                if (resolvedUrl.origin !== window.location.origin) {
                    return url
                }
                if (hasPrefix(resolvedUrl.pathname, prefix)) {
                    return url
                }
                resolvedUrl.pathname = prefix + resolvedUrl.pathname
                return resolvedUrl.pathname + resolvedUrl.search + resolvedUrl.hash
            } catch (e) {
                return url
            }
        }

        if (path.charAt(0) !== '/') {
            path = '/' + path
        }

        if (hasPrefix(path, prefix)) {
            return path + suffix
        }

        return prefix + path + suffix
    }

    function closestAnchor (target) {
        var node = target
        if (node && node.nodeType === 3) {
            node = node.parentNode
        }

        if (node && node.closest) {
            return node.closest('a[href]')
        }

        while (node && node !== document) {
            if (
                node.tagName
                && node.tagName.toLowerCase() === 'a'
                && node.getAttribute('href') !== null
            ) {
                return node
            }
            node = node.parentNode
        }

        return null
    }

    function openUrl (url, target) {
        var normalizedTarget = (target || '').toLowerCase()

        if (normalizedTarget === '_blank') {
            window.open(url, target)
            return
        }

        if (normalizedTarget === '_parent' && window.parent) {
            window.parent.location.href = url
            return
        }

        if (normalizedTarget === '_top' && window.top) {
            window.top.location.href = url
            return
        }

        if (target && normalizedTarget !== '_self') {
            window.open(url, target)
            return
        }

        window.location.href = url
    }

    function handleDocumentClick (event) {
        if (!event || event.defaultPrevented || event.button > 0) {
            return
        }

        var anchor = closestAnchor(event.target)

        if (!anchor || anchor.hasAttribute('download')) {
            return
        }

        var href = anchor.getAttribute('href')
        var prefixedHref = prefixSameOriginUrl(href)
        if (prefixedHref !== href) {
            event.preventDefault()
            event.stopPropagation()

            var target = anchor.getAttribute('target') || ''
            if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
                var normalizedTarget = target.toLowerCase()
                window.open(prefixedHref, !target || normalizedTarget === '_self' ? '_blank' : target)
                return
            }

            openUrl(prefixedHref, target)
        }
    }

    var rawOpen = window.open
    if (typeof rawOpen === 'function') {
        window.open = function (url, target, features) {
            return rawOpen.call(window, prefixSameOriginUrl(url), target, features)
        }
    }

    window.__prefixSameOriginUrl = prefixSameOriginUrl
    document.addEventListener('click', handleDocumentClick, true)
})()
