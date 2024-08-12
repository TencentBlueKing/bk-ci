import { getPlatformConfig, setShortcutIcon } from '@blueking/platform-config'
import createLocale from '../../../../locale'
const { i18n } = createLocale(require.context('@locale/nav/', false, /\.json$/), true)
const locale = i18n.locale.replace('_', '-')
const messages = i18n.messages[locale]

const state = () => ({
    platformInfo: {
        name: (messages && messages.bkci) || '蓝盾',
        nameEn: 'BK-CI',
        brandName: (messages && messages.tencentBlueKing) || '蓝鲸智云',
        brandNameEn: 'Tencent BlueKing',
        favicon: `${window.PUBLIC_URL_PREFIX}/static/favicon.ico`,
        version: window.BK_CI_VERSION,
        i18n: {}
    }
})
const getters = {
    platformInfo: state => state.platformInfo
}
const mutations = {
    setPlatformInfo (state, content) {
        state.platformInfo = content
    }
}

const actions = {
    async getPlatformPreData ({ state, commit }) {
        const config = { ...state.platformInfo }
        let resp
        const bkRepoUrl = window.BK_SHARED_RES_URL
        if (bkRepoUrl) {
            resp = await getPlatformConfig(`${bkRepoUrl}/bk_ci/base.js`, config)
        } else {
            resp = await getPlatformConfig(config)
        }
        const { i18n, name, brandName } = resp
        const currentPage = window.currentPage
        let platformTitle = `${i18n.name || name} | ${i18n.brandName || brandName}`
        document.title = currentPage
            ? `${currentPage.name} | ${platformTitle}`
            : platformTitle
        setShortcutIcon(resp.favicon)
        commit('setPlatformInfo', resp)
        return resp
    }
}

export default {
    state,
    getters,
    actions,
    mutations
}
