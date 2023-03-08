import ajax from '../http/fetch/index';
import * as BKUI from 'bkui-vue';
import {
    AngleDown
} from 'bkui-vue/lib/icon';
import {
    handleNoPermissionV3
} from '../../../common-lib/permission/permission';
import {
    h
} from 'vue';

export const handleProjectManageNoPermission = (query) => {
    return handleNoPermissionV3(
        {
            AngleDown,
            ...BKUI
        },
        {
            resourceType: 'project',
            ...query
        },
        ajax,
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
