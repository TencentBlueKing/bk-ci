import BkUserDisplayName from '@blueking/bk-user-display-name'
import fetch from '../http/fetch'

const userApiPrefix = `${window.BK_APIGW_USER_WEB_URL}/api/v3/open-web/tenant/users/-`
export default class TenantSingleton {
    static instance: any
    static tenantId: string
    static apiBaseUrl: string
    
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
        try {
            console.log(keyword, 123)
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
        apiBaseUrl: string
    }> {
        try {
            const data : {
                tenantId: string,
                apiBaseUrl: string
            } = await fetch.get?.('/project/api/user/users/tenantInfoForDisplay')

            BkUserDisplayName.configure({
                tenantId: data.tenantId,
                apiBaseUrl: data.apiBaseUrl,
                emptyText: 'unkown_user'
            })
            TenantSingleton.tenantId = data.tenantId
            TenantSingleton.apiBaseUrl = data.apiBaseUrl
            return data
        } catch (error) {
            console.error(error)
            return {
                tenantId: '',
                apiBaseUrl: ''
            }
        }
    }
}
