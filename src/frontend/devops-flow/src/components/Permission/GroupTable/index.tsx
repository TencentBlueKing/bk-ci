/**
 * GroupTable Component
 * Displays user group list with actions for apply, renewal, and exit
 */

import {
    Button,
    Checkbox,
    Dialog,
    Message,
    Sideslider,
    Table,
} from 'bkui-vue';
import { bkTooltips as vBkTooltips } from 'bkui-vue/lib/directives';
import {
    computed,
    defineComponent,
    onMounted,
    type PropType,
    reactive,
    ref,
} from 'vue';
import { useI18n } from 'vue-i18n';
import { exitGroup, getGroupMemberList, getGroupPolicies } from '../api';
import ApplyDialog from '../ApplyDialog';
import {
    APPLY_DIALOG_TYPES,
    getPermissionTitle,
    getStatusIconClass,
    getStatusText,
    MEMBER_STATUS,
} from '../constants';
import type {
    ApplyDialogType,
    GroupMemberInfo,
    GroupPolicy,
    MemberStatus,
    ResourceType,
} from '../types';
import styles from './index.module.css';

const { Column } = Table;

export default defineComponent({
  name: 'GroupTable',

  directives: {
    bkTooltips: vBkTooltips,
  },

  props: {
    /**
     * Resource type
     */
    resourceType: {
      type: String as PropType<ResourceType>,
      default: 'pipeline',
    },
    /**
     * Resource code
     */
    resourceCode: {
      type: String,
      default: '',
    },
    /**
     * Project code
     */
    projectCode: {
      type: String,
      default: '',
    },
    /**
     * API prefix
     */
    ajaxPrefix: {
      type: String,
      default: '',
    },
  },

  setup(props) {
    const { t } = useI18n();

    const isLoading = ref(false);
    const memberList = ref<GroupMemberInfo[]>([]);
    const showDetail = ref(false);
    const isDetailLoading = ref(false);
    const groupPolicies = ref<GroupPolicy[]>([]);
    const groupName = ref('');

    const logout = reactive({
      loading: false,
      isShow: false,
      groupId: '',
      name: '',
    });

    const apply = reactive<{
      isShow: boolean;
      groupName: string;
      groupId: string;
      status: MemberStatus | '';
      expiredDisplay: string;
      title: string;
      type: ApplyDialogType;
    }>({
      isShow: false,
      groupName: '',
      groupId: '',
      status: '',
      expiredDisplay: '',
      title: '',
      type: APPLY_DIALOG_TYPES.APPLY,
    });

    /**
     * Permission title based on resource type
     */
    const permissionTitle = computed(() =>
      getPermissionTitle(props.resourceType, (key) =>
        t(`flow.permission.${key}`),
      ),
    );

    /**
     * Fetch member list
     */
    const getMemberList = async () => {
      isLoading.value = true;
      try {
        const data = await getGroupMemberList(
          props.projectCode,
          props.resourceType,
          props.resourceCode,
          props.ajaxPrefix,
        );
        memberList.value = data;
      } catch (err) {
        const error = err as { message?: string };
        Message({
          theme: 'error',
          message: error.message || String(err),
        });
      } finally {
        isLoading.value = false;
      }
    };

    /**
     * View permission details
     */
    const handleViewDetail = async (row: GroupMemberInfo) => {
      groupName.value = row.groupName;
      showDetail.value = true;
      isDetailLoading.value = true;

      try {
        const data = await getGroupPolicies(
          props.projectCode,
          props.resourceType,
          row.groupId,
          props.ajaxPrefix,
        );
        groupPolicies.value = data;
      } catch (err) {
        const error = err as { message?: string };
        Message({
          theme: 'error',
          message: error.message || String(err),
        });
      } finally {
        isDetailLoading.value = false;
      }
    };

    /**
     * Format status text
     */
    const statusFormatter = (status: string) =>
      getStatusText(status, (key) => t(`flow.permission.${key}`));

    /**
     * Handle renewal action
     */
    const handleRenewal = (row: GroupMemberInfo) => {
      apply.isShow = true;
      apply.groupName = row.groupName;
      apply.groupId = row.groupId;
      apply.status = row.status;
      apply.expiredDisplay = row.expiredDisplay;
      apply.title = t('flow.permission.renewal');
      apply.type = APPLY_DIALOG_TYPES.RENEWAL;
    };

    /**
     * Handle apply to join action
     */
    const handleApply = (row: GroupMemberInfo) => {
      apply.isShow = true;
      apply.groupName = row.groupName;
      apply.groupId = row.groupId;
      apply.title = t('flow.permission.applyToJoin');
      apply.type = APPLY_DIALOG_TYPES.APPLY;
    };

    /**
     * Show logout confirmation
     */
    const handleShowLogout = (row: GroupMemberInfo) => {
      logout.isShow = true;
      logout.groupId = row.groupId;
      logout.name = row.groupName;
    };

    /**
     * Cancel logout
     */
    const handleCancelLogout = () => {
      logout.isShow = false;
    };

    /**
     * Confirm logout from group
     */
    const handleLogout = async () => {
      logout.loading = true;
      try {
        await exitGroup(
          props.projectCode,
          props.resourceType,
          logout.groupId,
          props.ajaxPrefix,
        );
        handleCancelLogout();
        getMemberList();
      } catch (err) {
        const error = err as { message?: string };
        Message({
          theme: 'error',
          message: error.message || String(err),
        });
      } finally {
        logout.loading = false;
      }
    };

    /**
     * Reset apply dialog state
     */
    const resetApply = () => {
      apply.isShow = false;
      apply.groupName = '';
      apply.groupId = '';
      apply.status = '';
      apply.expiredDisplay = '';
      apply.title = '';
      apply.type = APPLY_DIALOG_TYPES.APPLY;
    };

    /**
     * Handle apply success
     */
    const handleApplySuccess = () => {
      getMemberList();
    };

    onMounted(() => {
      getMemberList();
    });

    return () => (
      <article class={styles.groupTable}>
        <Table data={memberList.value} v-loading={isLoading.value}>
          <Column
            label={t('flow.permission.userGroup')}
            prop="groupName"
          />
          <Column label={t('flow.permission.addTime')} prop="createdTime">
            {{
              default: ({ row }: { row: GroupMemberInfo }) => (
                <span>{row.createdTime || '--'}</span>
              ),
            }}
          </Column>
          <Column label={t('flow.permission.validity')} prop="expiredDisplay">
            {{
              default: ({ row }: { row: GroupMemberInfo }) => (
                <span>
                  {row.expiredDisplay}
                  {row.status !== MEMBER_STATUS.EXPIRED &&
                    t('flow.permission.days')}
                </span>
              ),
            }}
          </Column>
          <Column label={t('flow.permission.status')} prop="status">
            {{
              default: ({ row }: { row: GroupMemberInfo }) => (
                <div class={styles.statusContent}>
                  <i
                    class={[
                      styles.statusIcon,
                      styles[getStatusIconClass(row.status)],
                    ]}
                  />
                  {statusFormatter(row.status)}
                </div>
              ),
            }}
          </Column>
          <Column label={t('flow.permission.actions')}>
            {{
              default: ({ row }: { row: GroupMemberInfo }) => (
                <div class={styles.actionBtns}>
                  <Button
                    class={styles.btn}
                    theme="primary"
                    text
                    onClick={() => handleViewDetail(row)}
                  >
                    {t('flow.permission.permissionDetails')}
                  </Button>

                  {row.status === MEMBER_STATUS.NOT_JOINED && (
                    <Button
                      class={styles.btn}
                      theme="primary"
                      text
                      onClick={() => handleApply(row)}
                    >
                      {t('flow.permission.applyToJoin')}
                    </Button>
                  )}

                  {([MEMBER_STATUS.EXPIRED, MEMBER_STATUS.NORMAL] as MemberStatus[]).includes(
                    row.status,
                  ) && (
                    <>
                      <span
                        v-bkTooltips={{
                          content: t('flow.permission.renewalContactAdmin'),
                          disabled: row.directAdded,
                        }}
                      >
                        <Button
                          class={styles.btn}
                          theme="primary"
                          text
                          disabled={!row.directAdded}
                          onClick={() => handleRenewal(row)}
                        >
                          {t('flow.permission.renewal')}
                        </Button>
                      </span>
                      <span
                        v-bkTooltips={{
                          content: t('flow.permission.exitContactAdmin'),
                          disabled: row.directAdded,
                        }}
                      >
                        <Button
                          class={styles.btn}
                          theme="primary"
                          text
                          disabled={!row.directAdded}
                          onClick={() => handleShowLogout(row)}
                        >
                          {t('flow.permission.exit')}
                        </Button>
                      </span>
                    </>
                  )}
                </div>
              ),
            }}
          </Column>
        </Table>

        {/* Permission details sideslider */}
        <Sideslider
          isShow={showDetail.value}
          onClosed={() => (showDetail.value = false)}
          quickClose
          width={640}
          v-slots={{
            header: () => (
              <div class={styles.detailTitle}>
                {t('flow.permission.permissionDetails')}
                <span class={styles.groupNameLabel}>{groupName.value}</span>
              </div>
            ),
          }}
        >
          <div class={styles.detailContent} v-loading={isDetailLoading.value}>
            <div class={styles.title}>{permissionTitle.value}</div>
            <div class={styles.content}>
              {groupPolicies.value.map((item, index) => (
                <Checkbox
                  key={index}
                  modelValue={item.permission}
                  disabled
                  class={styles.permissionItem}
                >
                  {item.actionName}
                </Checkbox>
              ))}
            </div>
          </div>
        </Sideslider>

        {/* Logout confirmation dialog */}
        <Dialog
          isShow={logout.isShow}
          title={t('flow.permission.confirmExitUserGroup')}
          onConfirm={handleLogout}
          onClosed={handleCancelLogout}
          loading={logout.loading}
        >
          {t('flow.permission.exitPermissionTip', { name: logout.name })}
        </Dialog>

        {/* Apply/Renewal dialog */}
        <ApplyDialog
          isShow={apply.isShow}
          groupName={apply.groupName}
          groupId={apply.groupId}
          status={apply.status as MemberStatus}
          expiredDisplay={apply.expiredDisplay}
          title={apply.title}
          type={apply.type}
          resourceType={props.resourceType}
          projectCode={props.projectCode}
          ajaxPrefix={props.ajaxPrefix}
          onCancel={resetApply}
          onSuccess={handleApplySuccess}
        />
      </article>
    );
  },
});
