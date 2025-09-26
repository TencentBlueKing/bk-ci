import request from '../http/fetch';
// @ts-ignore
const userApiPrefix = `${window.BK_APIGW_USER_WEB_URL}/api/v3/open-web/tenant/users/-`
export default class TenantSingleton {
    static instance: any
    
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
            const res = await request.get?.(`${userApiPrefix}/search/?keyword=${keyword || 'a'}`)
            return TenantSingleton.formatData(res)
        } catch (e) {
            return []
        }
    }
    static async  fetchTenantDisplayNames (uids) {
        try {
            const res = await request.get?.(`${userApiPrefix}/lookup/?lookups=${uids}&lookup_fields=bk_username`)
            return TenantSingleton.formatData(res)
        } catch (error) {
            console.error(error)
            return []
        }
    }

    static async init (): Promise<{
        tenantId: string,
        apiBaseUrl: string
    }> {
        try {
            const data : {
                tenantId: string,
                apiBaseUrl: string
            } = await request.get?.('/project/api/user/users/tenantInfoForDisplay')

            
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
