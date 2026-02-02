/**
 * Permission API Service
 * API calls for permission management components
 */

import { del, get, post, put } from '@/utils/http';
import type {
  ApplyToJoinGroupRequest,
  GroupListResponse,
  GroupMemberInfo,
  GroupPolicy,
  RenewalGroupMemberRequest,
} from './types';

/**
 * Default API prefix for permission related endpoints
 */
const DEFAULT_API_PREFIX = '/auth/api/user/auth';

/**
 * Build the full API URL with optional custom prefix
 * @param path - API path
 * @param ajaxPrefix - Optional custom prefix
 * @returns Full API URL
 */
const buildUrl = (path: string, ajaxPrefix?: string): string => {
  const prefix = ajaxPrefix || '';
  return `${prefix}${DEFAULT_API_PREFIX}${path}`;
};

/**
 * Check if the user has manager permission for the resource
 * @param projectCode - Project code
 * @param resourceType - Resource type
 * @param resourceCode - Resource code
 * @param ajaxPrefix - Optional custom API prefix
 * @returns Promise<boolean>
 */
export const hasManagerPermission = (
  projectCode: string,
  resourceType: string,
  resourceCode: string,
  ajaxPrefix?: string,
): Promise<boolean> => {
  return get(
    buildUrl(
      `/resource/${projectCode}/${resourceType}/${resourceCode}/hasManagerPermission`,
      ajaxPrefix,
    ),
  );
};

/**
 * Check if permission management is enabled for the resource
 * @param projectCode - Project code
 * @param resourceType - Resource type
 * @param resourceCode - Resource code
 * @param ajaxPrefix - Optional custom API prefix
 * @returns Promise<boolean>
 */
export const isEnablePermission = (
  projectCode: string,
  resourceType: string,
  resourceCode: string,
  ajaxPrefix?: string,
): Promise<boolean> => {
  return get(
    buildUrl(
      `/resource/${projectCode}/${resourceType}/${resourceCode}/isEnablePermission`,
      ajaxPrefix,
    ),
  );
};

/**
 * Enable permission management for the resource
 * @param projectCode - Project code
 * @param resourceType - Resource type
 * @param resourceCode - Resource code
 * @param ajaxPrefix - Optional custom API prefix
 */
export const enablePermission = (
  projectCode: string,
  resourceType: string,
  resourceCode: string,
  ajaxPrefix?: string,
): Promise<void> => {
  return put(
    buildUrl(
      `/resource/${projectCode}/${resourceType}/${resourceCode}/enable`,
      ajaxPrefix,
    ),
  );
};

/**
 * Disable permission management for the resource
 * @param projectCode - Project code
 * @param resourceType - Resource type
 * @param resourceCode - Resource code
 * @param ajaxPrefix - Optional custom API prefix
 */
export const disablePermission = (
  projectCode: string,
  resourceType: string,
  resourceCode: string,
  ajaxPrefix?: string,
): Promise<void> => {
  return put(
    buildUrl(
      `/resource/${projectCode}/${resourceType}/${resourceCode}/disable`,
      ajaxPrefix,
    ),
  );
};

/**
 * Get user group list for the resource
 * @param projectCode - Project code
 * @param resourceType - Resource type
 * @param resourceCode - Resource code
 * @param page - Page number
 * @param pageSize - Page size
 * @param ajaxPrefix - Optional custom API prefix
 * @returns Promise<GroupListResponse>
 */
export const getGroupList = (
  projectCode: string,
  resourceType: string,
  resourceCode: string,
  page: number,
  pageSize: number,
  ajaxPrefix?: string,
): Promise<GroupListResponse> => {
  return get(
    buildUrl(
      `/resource/${projectCode}/${resourceType}/${resourceCode}/listGroup?page=${page}&pageSize=${pageSize}`,
      ajaxPrefix,
    ),
  );
};

/**
 * Get group member list for the resource
 * @param projectCode - Project code
 * @param resourceType - Resource type
 * @param resourceCode - Resource code
 * @param ajaxPrefix - Optional custom API prefix
 * @returns Promise<GroupMemberInfo[]>
 */
export const getGroupMemberList = (
  projectCode: string,
  resourceType: string,
  resourceCode: string,
  ajaxPrefix?: string,
): Promise<GroupMemberInfo[]> => {
  return get(
    buildUrl(
      `/resource/${projectCode}/${resourceType}/${resourceCode}/groupMember`,
      ajaxPrefix,
    ),
  );
};

/**
 * Get group policies/permissions
 * @param projectCode - Project code
 * @param resourceType - Resource type
 * @param groupId - Group ID
 * @param ajaxPrefix - Optional custom API prefix
 * @returns Promise<GroupPolicy[]>
 */
export const getGroupPolicies = (
  projectCode: string,
  resourceType: string,
  groupId: string,
  ajaxPrefix?: string,
): Promise<GroupPolicy[]> => {
  return get(
    buildUrl(
      `/resource/group/${projectCode}/${resourceType}/${groupId}/groupPolicies`,
      ajaxPrefix,
    ),
  );
};

/**
 * Apply to join a user group
 * @param data - Apply request data
 * @param ajaxPrefix - Optional custom API prefix
 */
export const applyToJoinGroup = (
  data: ApplyToJoinGroupRequest,
  ajaxPrefix?: string,
): Promise<void> => {
  return post(buildUrl('/apply/applyToJoinGroup', ajaxPrefix), data);
};

/**
 * Renew group membership
 * @param projectCode - Project code
 * @param resourceType - Resource type
 * @param groupId - Group ID
 * @param data - Renewal request data
 * @param ajaxPrefix - Optional custom API prefix
 */
export const renewGroupMembership = (
  projectCode: string,
  resourceType: string,
  groupId: string,
  data: RenewalGroupMemberRequest,
  ajaxPrefix?: string,
): Promise<void> => {
  return put(
    buildUrl(
      `/resource/group/${projectCode}/${resourceType}/${groupId}/member/renewal`,
      ajaxPrefix,
    ),
    data,
  );
};

/**
 * Exit from a user group
 * @param projectCode - Project code
 * @param resourceType - Resource type
 * @param groupId - Group ID
 * @param ajaxPrefix - Optional custom API prefix
 */
export const exitGroup = (
  projectCode: string,
  resourceType: string,
  groupId: string,
  ajaxPrefix?: string,
): Promise<void> => {
  return del(
    buildUrl(
      `/resource/group/${projectCode}/${resourceType}/${groupId}/member`,
      ajaxPrefix,
    ),
  );
};

/**
 * Delete a user group
 * @param projectCode - Project code
 * @param resourceType - Resource type
 * @param groupId - Group ID
 * @param ajaxPrefix - Optional custom API prefix
 */
export const deleteGroup = (
  projectCode: string,
  resourceType: string,
  groupId: string,
  ajaxPrefix?: string,
): Promise<void> => {
  return del(
    buildUrl(
      `/resource/group/${projectCode}/${resourceType}/${groupId}`,
      ajaxPrefix,
    ),
  );
};

/**
 * Sync group permissions with IAM
 * @param projectCode - Project code
 * @param groupId - Group ID
 * @param ajaxPrefix - Optional custom API prefix
 */
export const syncGroupPermissions = (
  projectCode: string,
  groupId: string,
  ajaxPrefix?: string,
): Promise<void> => {
  return put(
    buildUrl(
      `/resource/group/sync/${projectCode}/${groupId}/syncGroupPermissions`,
      ajaxPrefix,
    ),
  );
};

/**
 * Sync group members with IAM
 * @param projectCode - Project code
 * @param groupId - Group ID
 * @param ajaxPrefix - Optional custom API prefix
 */
export const syncGroupMember = (
  projectCode: string,
  groupId: string,
  ajaxPrefix?: string,
): Promise<void> => {
  return put(
    buildUrl(
      `/resource/group/sync/${projectCode}/${groupId}/syncGroupMember`,
      ajaxPrefix,
    ),
  );
};
