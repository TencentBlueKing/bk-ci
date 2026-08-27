import BkUserDisplayName from '@blueking/bk-user-display-name'
import Vue from 'vue'
import {
    applyBkUserDisplayName,
    applyTenantDisplayInfo,
    DEFAULT_USER_TIME_ZONE,
    getTenantUserApiPrefix,
    hasTenantId
} from '../../../common-lib/time'
import request from './request'
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
            } = await request.get('project/api/user/users/tenantInfoForDisplay')

            const tenantInfo = applyTenantDisplayInfo(data)
            if (hasTenantId(tenantInfo)) {
                request.defaults.headers.common['X-Bk-Tenant-Id'] = tenantInfo.tenantId
            }
            applyBkUserDisplayName(Vue, BkUserDisplayName, tenantInfo)
            return tenantInfo
        } catch (error) {
            console.error(error)
            const tenantInfo = applyTenantDisplayInfo({
                tenantId: '',
                apiBaseUrl: '',
                timeZone: DEFAULT_USER_TIME_ZONE
            })
            applyBkUserDisplayName(Vue, BkUserDisplayName, tenantInfo)
            return tenantInfo
        }
    }
}
