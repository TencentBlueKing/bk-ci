/**
 * Permission utilities — delegates to bk-permission.
 */

import { handleNoPermissionV3 } from 'bk-permission'
import * as BKUI from 'bkui-vue'
import { h } from 'vue'
import { RESOURCE_TYPES } from '@/components/Permission/constants'

export interface PermissionParams {
  projectId: string
  resourceType: string
  resourceCode: string
  action?: string
}

/**
 * Trigger the bk-permission "apply for permission" dialog.
 */
export function handleFlowNoPermission(permissionData: PermissionParams) {
  handleNoPermissionV3(BKUI, permissionData, h)
}

/**
 * Shortcut for creative_stream resource type.
 */
export function handleFlowResourceNoPermission(query: Omit<PermissionParams, 'resourceType'>) {
  return handleFlowNoPermission({
    resourceType: RESOURCE_TYPES.CREATIVE_STREAM,
    ...query,
  })
}
