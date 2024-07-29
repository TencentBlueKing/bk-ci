import { getPlatformConfig, setShortcutIcon } from '@blueking/platform-config'

const state = () => ({
    platformInfo: {
        name: '蓝盾',
        nameEn: 'BLUEKING CI',
        brandName: '腾讯蓝鲸智云',
        brandNameEn: 'BlueKing',
        favicon: `${window.PUBLIC_URL_PREFIX}/static/favicon.ico`,
        version: window.BK_CI_VERSION
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
        document.title = `${i18n.name || name} | ${i18n.brandName || brandName}`
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
