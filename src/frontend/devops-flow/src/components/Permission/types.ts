/**
 * Permission Component Types
 * TypeScript type definitions for permission management components
 */

/**
 * Resource type enum for permission management
 */
export type ResourceType =
  | 'project'
  | 'creative_stream'
  | 'creative_stream_group'
  | 'creative_stream_template'
  | 'environment';

/**
 * Member status in a user group
 */
export type MemberStatus = 'NOT_JOINED' | 'NORMAL' | 'EXPIRED';

/**
 * Apply dialog type
 */
export type ApplyDialogType = 'apply' | 'renewal';

/**
 * IAM message types from iframe postMessage
 */
export type IAMMessageCode =
  | 'create_user_group_submit'
  | 'create_user_group_cancel'
  | 'add_user_confirm'
  | 'add_template_confirm'
  | 'remove_user_confirm'
  | 'remove_template_confirm'
  | 'change_group_detail_tab'
  | 'submit_add_group_perm'
  | 'submit_delete_group_perm'
  | 'submit_edit_group_perm'
  | 'renewal_user_confirm'
  | 'renewal_template_confirm';

/**
 * Common resource info interface
 */
export interface ResourceInfo {
  projectCode: string;
  resourceType: ResourceType;
  resourceCode: string;
  resourceName?: string;
}

/**
 * Permission main component props
 */
export interface PermissionMainProps extends ResourceInfo {
  showCreateGroup?: boolean;
  ajaxPrefix?: string;
}

/**
 * User group basic info
 */
export interface GroupInfo {
  groupId: string;
  name: string;
  id?: number;
  userCount: number;
  departmentCount: number;
  templateCount?: number;
  managerId?: string;
}

/**
 * Group list API response
 */
export interface GroupListResponse {
  records: GroupInfo[];
  hasNext: boolean;
}

/**
 * Group member info for table display
 */
export interface GroupMemberInfo {
  groupId: string;
  groupName: string;
  createdTime: string;
  expiredDisplay: string;
  status: MemberStatus;
  directAdded: boolean;
}

/**
 * Group policy/permission info
 */
export interface GroupPolicy {
  actionName: string;
  permission: boolean;
}

/**
 * Apply dialog form data
 */
export interface ApplyFormData {
  expireTime: number;
  reason: string;
  englishName?: string;
}

/**
 * Apply dialog props
 */
export interface ApplyDialogProps {
  isShow: boolean;
  groupName: string;
  groupId: string;
  expiredDisplay?: string;
  title: string;
  type: ApplyDialogType;
  status?: MemberStatus;
  resourceType: ResourceType;
  projectCode: string;
  ajaxPrefix?: string;
}

/**
 * Apply to join group request body
 */
export interface ApplyToJoinGroupRequest {
  groupIds: string[];
  expiredAt: number;
  reason: string;
  applicant: string;
  projectCode: string;
}

/**
 * Renewal group member request body
 */
export interface RenewalGroupMemberRequest {
  expiredAt: number;
  projectId?: string;
  resourceType: ResourceType;
}

/**
 * Delete dialog state
 */
export interface DeleteDialogState {
  group: GroupInfo | Record<string, never>;
  isShow: boolean;
  isLoading: boolean;
}

/**
 * Close dialog state
 */
export interface CloseDialogState {
  isShow: boolean;
  isLoading: boolean;
}

/**
 * Logout/exit group dialog state
 */
export interface LogoutDialogState {
  loading: boolean;
  isShow: boolean;
  groupId: string;
  name: string;
}

/**
 * IAM message event data structure
 */
export interface IAMMessageData {
  type: string;
  code: IAMMessageCode;
  data?: {
    id?: string;
    departments?: unknown[];
    users?: unknown[];
    templates?: unknown[];
    members?: Array<{ type: string }>;
    tab?: string;
  };
}

/**
 * Time filter options for expiration selection
 */
export interface TimeFilterOptions {
  [key: number]: string;
}

/**
 * API error response
 */
export interface APIErrorResponse {
  code: number;
  message: string;
  httpStatus?: number;
}

/**
 * Permission check hook return type
 */
export interface UsePermissionStatusReturn {
  isLoading: boolean;
  isEnablePermission: boolean;
  hasPermission: boolean;
  isApprover: boolean;
  errorCode: number | string;
  initStatus: () => Promise<void>;
}

/**
 * Group aside component emits
 */
export interface GroupAsideEmits {
  (e: 'choose-group', group: GroupInfo): void;
  (e: 'create-group'): void;
  (e: 'close-manage'): void;
  (e: 'change-group-detail-tab', tab: string): void;
}

/**
 * Permission manage component emits
 */
export interface PermissionManageEmits {
  (e: 'close-manage'): void;
}

/**
 * No enable permission component emits
 */
export interface NoEnablePermissionEmits {
  (e: 'open-manage'): void;
}

/**
 * Apply dialog component emits
 */
export interface ApplyDialogEmits {
  (e: 'cancel'): void;
  (e: 'success'): void;
}

/**
 * v-perm directive binding value
 */
export interface PermDirectiveValue {
  hasPermission?: boolean;
  disablePermissionApi?: boolean;
  permissionData?: {
    projectCode: string;
    resourceType: ResourceType;
    resourceCode: string;
    action?: string;
  };
}
