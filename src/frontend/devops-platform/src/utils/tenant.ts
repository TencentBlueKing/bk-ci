import BkUserDisplayName from '@blueking/bk-user-display-name'
import {
    applyTenantDisplayInfo,
    DEFAULT_USER_TIME_ZONE,
    getTenantUserApiPrefix,
    hasTenantUserApi
} from '../../../common-lib/time'
import fetch from '../http/fetch'
export default class TenantSingleton {
    static instance: any
    static tenantId: string
    static apiBaseUrl: string
    static timeZone: string = DEFAULT_USER_TIME_ZONE
    
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
        return res.map(item => ({
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
            const res = await fetch.get?.(`${userApiPrefix}/search/?keyword=${keyword || 'a'}`, null, {
                headers: {
                    'X-Bk-Tenant-Id': TenantSingleton.tenantId
                }
            })
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
            const res = await fetch.get?.(`${userApiPrefix}/lookup/?lookups=${uids}&lookup_fields=bk_username`, null, {
                headers: {
                    'X-Bk-Tenant-Id': TenantSingleton.tenantId
                }
            })
            return TenantSingleton.formatData(res)
        } catch (error) {
            console.error(error)
            return []
        }
    }

    async init (): Promise<{
        tenantId: string,
        apiBaseUrl: string,
        timeZone: string
    }> {
        try {
            const data : {
                tenantId: string,
                apiBaseUrl: string,
                timeZone?: string
            } = await fetch.get?.('/project/api/user/users/tenantInfoForDisplay')

            const tenantInfo = applyTenantDisplayInfo(data)
            if (hasTenantUserApi(tenantInfo)) {
                BkUserDisplayName.configure({
                    tenantId: tenantInfo.tenantId,
                    apiBaseUrl: tenantInfo.apiBaseUrl,
                    emptyText: 'unkown_user'
                })
            }
            TenantSingleton.tenantId = tenantInfo.tenantId
            TenantSingleton.apiBaseUrl = tenantInfo.apiBaseUrl
            TenantSingleton.timeZone = tenantInfo.timeZone
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
