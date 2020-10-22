const Vue = window.Vue
const vue = new Vue()
const prefix = 'store/api'

export default {
    getMemberView (params) {
        return vue.$ajax.get(`${prefix}/user/market/desk/store/member/view`, { params })
    },

    getMemberList (params) {
        return vue.$ajax.get(`${prefix}/user/market/desk/store/member/list`, { params })
    },

    requestDeleteMember (params) {
        return vue.$ajax.delete(`${prefix}/user/market/desk/store/member/delete`, { params })
    },

    requestAddMember (params) {
        return vue.$ajax.post(`${prefix}/user/market/desk/store/member/add`, params)
    },

    requestChangeProject (params) {
        return vue.$ajax.put(`${prefix}/user/market/desk/store/member/test/project/change?projectCode=${params.projectCode}&storeCode=${params.storeCode}&storeType=${params.storeType}&storeMember=${params.storeMember}`)
    }
}
