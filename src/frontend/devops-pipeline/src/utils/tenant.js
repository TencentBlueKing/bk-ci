import BkUserDisplayName from '@blueking/bk-user-display-name'
import {
    applyTenantDisplayInfo,
    DEFAULT_USER_TIME_ZONE,
    getTenantUserApiPrefix,
    hasTenantId,
    hasTenantUserApi
} from '../../../common-lib/time'
import request from './request'
export default class TenantSingleton {
    static instance;
    apiBaseUrl = '';
    tenantId = '';
    timeZone = DEFAULT_USER_TIME_ZONE;
    
    constructor () {
        if (TenantSingleton.instance) {
            return TenantSingleton.instance
        }
        TenantSingleton.instance = this
        return this
    }

    static getInstance () {
        if (!TenantSingleton.instance) {
            TenantSingleton.instance = new TenantSingleton()
        }
        return TenantSingleton.instance
    }

    static formatData (res) {
        return res.data.map(item => ({
            id: item.bk_username,
            name: item.display_name
        }))
    }

    static async fetchTenantUsers (keyword = 'a') {
        const userApiPrefix = getTenantUserApiPrefix()
        if (!userApiPrefix) {
            return []
        }
        try {
            const res = await request.get(`${userApiPrefix}/search/?keyword=${keyword || 'a'}`)
            return TenantSingleton.formatData(res)
        } catch (e) {
            return []
        }
    }
    static async  fetchTenantDisplayNames (uids) {
        const userApiPrefix = getTenantUserApiPrefix()
        if (!userApiPrefix) {
            return []
        }
        try {
            const res = await request.get(`${userApiPrefix}/lookup/?lookups=${uids}&lookup_fields=bk_username`)
            return TenantSingleton.formatData(res)
        } catch (error) {
            console.error(error)
            return []
        }
    }


    async init () {
        try {
            const { data } = await request.get('project/api/user/users/tenantInfoForDisplay')
            const tenantInfo = applyTenantDisplayInfo(data)
            this.apiBaseUrl = tenantInfo.apiBaseUrl
            this.tenantId = tenantInfo.tenantId
            this.timeZone = tenantInfo.timeZone
            if (hasTenantId(tenantInfo)) {
                request.defaults.headers.common['X-Bk-Tenant-Id'] = tenantInfo.tenantId
            }
            if (hasTenantUserApi(tenantInfo)) {
                BkUserDisplayName.configure({
                    tenantId: tenantInfo.tenantId,
                    apiBaseUrl: tenantInfo.apiBaseUrl,
                    emptyText: 'unkown_user'
                })
            }
            return tenantInfo
        } catch (error) {
            console.error(error)
            return applyTenantDisplayInfo({
                tenantId: '',
                apiBaseUrl: '',
                timeZone: DEFAULT_USER_TIME_ZONE
            })
        }
    }
}
