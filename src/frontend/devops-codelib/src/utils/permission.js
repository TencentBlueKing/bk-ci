import { handleNoPermission } from 'bk-permission'
import * as BKUI from 'bk-magic-vue'

// 权限动作
export const RESOURCE_ACTION = {
    CREATE: 'repertory_create',
    VIEW: 'repertory_view',
    EDIT: 'repertory_edit',
    DELETE: 'repertory_delete',
    USE: 'repertory_use'
}

export const handleCodelibNoPermission = (query, data) => {
    return handleNoPermission(
        BKUI,
        {
            resourceType: 'repertory',
            ...query
        },
        window.devops.$createElement,
        data
    )
}

export const RESOURCE_TYPE = 'repertory'
