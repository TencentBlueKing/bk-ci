import * as BKUI from 'bkui-vue';
import {
    AngleDown
} from 'bkui-vue/lib/icon';
import {
    handleNoPermission
} from 'bk-permission';
import {
    h
} from 'vue';

export const handleProjectManageNoPermission = (query) => {
    return handleNoPermission(
        {
            AngleDown,
            ...BKUI
        },
        {
            resourceType: 'project',
            ...query
        },
        h
    );
};

// 流水线权限动作
export const RESOURCE_ACTION = {
    VIEW: 'project_view',
    EDIT: 'project_edit',
    ENABLE: 'project_enable',
    MANAGE: 'project_manage'
};

export const RESOURCE_TYPE = 'project';
