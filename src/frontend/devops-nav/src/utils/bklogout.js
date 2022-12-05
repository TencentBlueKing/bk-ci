/**
 * @file bklogout
 */

/* globals pt_logout */

const bklogout = {
    ret: -1,
    mainDomain: '',
    oaLogoutUrl: '//bklogin.woa.com/clear_cookie/',
    getCookie (b) {
        const a = document.cookie.match(new RegExp('(^| )' + b + '=([^;]*)(;|$)'))
        return !a ? '' : decodeURIComponent(a[2])
    },
    delCookie (a, b, c) {
        document.cookie = a
            + '=; expires=Mon, 26 Jul 1997 05:00:00 GMT; path=' + (c || '/') + '; ' + (b ? ('domain=' + b + ';') : '')
    },
    jsonp (b) {
        const a = document.createElement('script')
        a.setAttribute('src', b)
        document.getElementsByTagName('head')[0].appendChild(a)
        a.onerror = () => {
            a.onerror = null
            bklogout.set_ret(-1, '')
        }
    },
    init () {
        const a = location.hostname.match(/\w*\.(com|cn)$/)
        bklogout.mainDomain = a ? a[0] : ''
    },
    getLogoutUrl (b) {
        const g = bklogout.getCookie('bk_ticket')
        let e = ''
        const h = b ? 'oa.com' : bklogout.mainDomain
        e = '//bklogin.' + h + '/logout?'
        e += ('bk_ticket=' + encodeURIComponent(g) + '&deep_logout=1')
        return e
    },
    pt_callback (status) {
        // let a = a || bklogout.mainDomain
        const a = bklogout.mainDomain
        if (status !== 2) {
            bklogout.delCookie('uin', a)
            bklogout.delCookie('skey', a)
        }
    },
    clearCookie (a) {
        // let a = a || bklogout.mainDomain
        a = a || bklogout.mainDomain
        bklogout.delCookie('bk_ticket', a)
        bklogout.delCookie('TCOA', a)
        bklogout.delCookie('TCOA_TICKET', a)
        if (window.pt_logout) {
            window.pt_logout.logout(bklogout.pt_callback)
        }
    },
    set_ret (d, b) {
        try {
            if (d !== 0) {
                bklogout.clearCookie(b)
            }
            if (typeof bklogout.callback === 'function') {
                bklogout.callback(2)
            }
        } catch (c) {
            console.error(c)
        }
    },
    logout (f, b) {
        bklogout.init()
        // 清除第三方登录票据
        const a = bklogout.mainDomain
        if (a !== 'oa.com') {
            bklogout.jsonp(bklogout.oaLogoutUrl)
        } else {
            bklogout.delCookie('TCOA', a)
            bklogout.delCookie('TCOA_TICKET', a)
            if (window.pt_logout) {
                window.pt_logout.logout(bklogout.pt_callback)
            }
        }
        const bkTicket = bklogout.getCookie('bk_ticket')
        const oaTicket = bklogout.getCookie('TCOA_TICKET')
        const qqTicket = bklogout.getCookie('uin')
        if (typeof f === 'function') {
            bklogout.callback = f
        }
        const d = bklogout.getLogoutUrl(b)
        if (!bkTicket && !oaTicket && !qqTicket) {
            if (typeof f === 'function') {
                f(2)
            }
        } else {
            bklogout.jsonp(d)
        }
    }
}

export default bklogout
