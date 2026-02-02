/**
 * GroupAside Component
 * Sidebar for user group list management
 */

import { Button, Dialog, Message, Popover } from 'bkui-vue'
import { bkLoading as vBkLoading } from 'bkui-vue/lib/directives'
import {
    computed,
    defineComponent,
    onMounted,
    onUnmounted,
    type PropType,
    reactive,
    ref,
} from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { SvgIcon } from '@/components/SvgIcon'
import { disablePermission, getGroupList, syncGroupMember, syncGroupPermissions } from '../api'
import {
    DEFAULT_PAGE_SIZE,
    getCloseManageTips,
    getGroupCountFields,
    getResourceTypeName,
    IAM_MESSAGE_TYPE,
    RESOURCE_TYPES,
} from '../constants'
import type { GroupInfo, IAMMessageData, ResourceType } from '../types'
import styles from './index.module.css'

export default defineComponent({
    name: 'GroupAside',

    directives: {
        bkLoading: vBkLoading,
    },

    props: {
        /**
         * Active group index
         */
        activeIndex: {
            type: Number,
            default: 0,
        },
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
         * Resource name
         */
        resourceName: {
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
         * Whether to show create group button
         */
        showCreateGroup: {
            type: Boolean,
            default: true,
        },
        /**
         * API prefix
         */
        ajaxPrefix: {
            type: String,
            default: '',
        },
    },

    emits: ['chooseGroup', 'createGroup', 'closeManage', 'changeGroupDetailTab'],

    setup(props, { emit }) {
        const { t } = useI18n()
        const route = useRoute()
        const router = useRouter()

        const page = ref(1)
        const activeTab = ref('')
        const groupList = ref<GroupInfo[]>([])
        const hasLoadEnd = ref(false)
        const isLoading = ref(false)
        const isClosing = ref(false)
        const curGroupIndex = ref(-1)
        const tabName = ref('member')

        const closeObj = reactive({
            isShow: false,
            isLoading: false,
        })

        /**
         * Group count fields based on resource type
         */
        const groupCountField = computed(() => getGroupCountFields(props.resourceType))

        /**
         * Resource type display name
         */
        const resourceTypeName = computed(() =>
            getResourceTypeName(props.resourceType, (key) => t(`flow.permission.${key}`)),
        )

        /**
         * Close manage tips text
         */
        const closeManageTips = computed(() =>
            getCloseManageTips(props.resourceType, (key) => t(`flow.permission.${key}`)),
        )

        /**
         * Fetch group list data
         */
        const handleGetData = async (pageSize: number = DEFAULT_PAGE_SIZE) => {
            // Only show loading on first page
            if (page.value === 1) {
                isLoading.value = true
            }
            try {
                const data = await getGroupList(
                    props.projectCode,
                    props.resourceType,
                    props.resourceCode,
                    page.value,
                    pageSize,
                    props.ajaxPrefix,
                )

                hasLoadEnd.value = !data.hasNext
                groupList.value.push(...data.records)

                // Select first group on first page load
                if (page.value === 1 && groupList.value.length > 0) {
                    const queryGroupId = route.query?.groupId
                    const chooseGroup =
                        groupList.value.find((group) => String(group.groupId) === String(queryGroupId)) ||
                        groupList.value[0]
                    handleChooseGroup(chooseGroup)
                }

                page.value += 1
            } catch (error) {
                console.error('Failed to fetch group list:', error)
            } finally {
                isLoading.value = false
            }
        }

        /**
         * Refresh the entire list
         */
        const refreshList = async () => {
            groupList.value = []
            hasLoadEnd.value = false
            page.value = 1
            return handleGetData(100)
        }

        /**
         * Handle group selection
         */
        const handleChooseGroup = (group?: GroupInfo) => {
            if (!group?.groupId) {
                return
            }
            router.replace({
                query: {
                    ...route.query,
                    groupId: group.groupId,
                },
            })
            activeTab.value = group.groupId
            curGroupIndex.value = groupList.value.findIndex((item) => item.groupId === group.groupId)
            emit('chooseGroup', group)
        }

        /**
         * Handle create group
         */
        const handleCreateGroup = () => {
            activeTab.value = ''
            emit('createGroup')
        }

        /**
         * Show close manage dialog
         */
        const showCloseManageDialog = () => {
            closeObj.isShow = true
        }

        /**
         * Hide close manage dialog
         */
        const handleHiddenCloseManage = () => {
            closeObj.isShow = false
        }

        /**
         * Handle close permission management
         */
        const handleCloseManage = async () => {
            isClosing.value = true
            try {
                await disablePermission(
                    props.projectCode,
                    props.resourceType,
                    props.resourceCode,
                    props.ajaxPrefix,
                )
                handleHiddenCloseManage()
                emit('closeManage')
            } finally {
                isClosing.value = false
            }
        }

        /**
         * Sync group permissions with IAM
         */
        const handleSyncGroupPermissions = async (groupId: string) => {
            try {
                await syncGroupPermissions(props.projectCode, groupId, props.ajaxPrefix)
            } catch (error) {
                const err = error as { message?: string }
                Message({
                    theme: 'error',
                    message: err.message || String(error),
                })
            }
        }

        /**
         * Sync group members with IAM
         */
        const handleSyncGroupIAM = async (groupId: string) => {
            try {
                await syncGroupMember(props.projectCode, groupId, props.ajaxPrefix)
            } catch (error) {
                const err = error as { message?: string }
                Message({
                    theme: 'error',
                    message: err.message || String(error),
                })
            }
        }

        /**
         * Handle IAM iframe message
         */
        const handleMessage = (event: MessageEvent) => {
            const messageData = event.data as IAMMessageData
            if (messageData?.type !== IAM_MESSAGE_TYPE) return

            const currentGroup = groupList.value[curGroupIndex.value]

            switch (messageData.code) {
                case 'create_user_group_submit':
                    refreshList().then(() => {
                        const group =
                            groupList.value.find((g) => g.groupId === messageData?.data?.id) || groupList.value[0]
                        handleChooseGroup(group)
                    })
                    break

                case 'create_user_group_cancel':
                    if (groupList.value.length > 0) {
                        handleChooseGroup(groupList.value[0])
                    }
                    break

                case 'add_user_confirm':
                case 'add_template_confirm':
                    if (currentGroup && messageData.data) {
                        currentGroup.departmentCount += messageData.data.departments?.length || 0
                        currentGroup.userCount += messageData.data.users?.length || 0
                        if (currentGroup.templateCount !== undefined) {
                            currentGroup.templateCount += messageData.data.templates?.length || 0
                        }
                        handleSyncGroupIAM(currentGroup.groupId)
                    }
                    break

                case 'remove_user_confirm':
                case 'remove_template_confirm':
                    if (currentGroup && messageData.data?.members) {
                        const departments = messageData.data.members.filter((i) => i.type === 'department')
                        const users = messageData.data.members.filter((i) => i.type === 'user')
                        const templates = messageData.data.members.filter((i) => i.type === 'template')
                        currentGroup.departmentCount -= departments.length
                        currentGroup.userCount -= users.length
                        if (currentGroup.templateCount !== undefined) {
                            currentGroup.templateCount -= templates.length
                        }
                        handleSyncGroupIAM(currentGroup.groupId)
                    }
                    break

                case 'change_group_detail_tab':
                    if (messageData.data?.tab) {
                        tabName.value = messageData.data.tab
                        emit('changeGroupDetailTab', messageData.data.tab)
                    }
                    break

                case 'submit_add_group_perm':
                case 'submit_delete_group_perm':
                case 'submit_edit_group_perm':
                    if (messageData.data?.id) {
                        handleSyncGroupPermissions(messageData.data.id)
                    }
                    break

                case 'renewal_user_confirm':
                case 'renewal_template_confirm':
                    if (currentGroup) {
                        handleSyncGroupIAM(currentGroup.groupId)
                    }
                    break
            }
        }

        /**
         * Get icon name based on count field
         */
        const getIconName = (field: string) => {
            const iconMap: Record<string, string> = {
                userCount: 'user',
                templateCount: 'usergroup',
                departmentCount: 'organization',
            }
            return iconMap[field] || ''
        }

        onMounted(() => {
            window.addEventListener('message', handleMessage)
            handleGetData()
        })

        onUnmounted(() => {
            window.removeEventListener('message', handleMessage)
        })

        return () => (
            <article class={styles.groupAside} v-bkLoading={{ loading: isLoading.value }}>
                <span class={styles.groupTitle}>{t('flow.permission.permissionRoles')}</span>

                <div class={styles.groupList}>
                    {groupList.value.map((group) => (
                        <div
                            key={group.groupId}
                            class={[styles.groupItem, activeTab.value === group.groupId && styles.groupActive]}
                            onClick={() => handleChooseGroup(group)}
                        >
                            <span class={styles.groupName} title={group.name}>
                                {group.name}
                            </span>
                            <div class={styles.numContainer}>
                                {groupCountField.value.map((item) => (
                                    <div key={item} class={styles.numBox}>
                                        <SvgIcon
                                            name={getIconName(item)}
                                            size={14}
                                            class={[
                                                styles.groupIcon,
                                                activeTab.value === group.groupId && styles.activeIcon,
                                            ]}
                                        />
                                        <div class={styles.groupNum}>{group[item as keyof GroupInfo]}</div>
                                    </div>
                                ))}
                            </div>
                            {props.resourceType === RESOURCE_TYPES.PROJECT && (
                                <Popover
                                    class={styles.groupMoreOption}
                                    placement="bottom"
                                    theme="light"
                                    arrow={false}
                                    offset={15}
                                    v-slots={{
                                        content: () => (
                                            <Button
                                                class={styles.deleteBtn}
                                                disabled={[1, 2].includes(group.id || 0)}
                                                text
                                            >
                                                {t('flow.permission.delete')}
                                            </Button>
                                        ),
                                    }}
                                >
                                    <i class={styles.moreIcon} onClick={(e: Event) => e.stopPropagation()}>
                                        ⋮
                                    </i>
                                </Popover>
                            )}
                        </div>
                    ))}
                </div>

                <div class={styles.lineSplit} />

                {props.showCreateGroup && (
                    <div
                        class={[styles.groupItem, activeTab.value === '' && styles.groupActive]}
                        onClick={handleCreateGroup}
                    >
                        <span class={styles.addGroupBtn}>
                            <i class={styles.addIcon}>+</i>
                            {t('flow.permission.createNewUserGroup')}
                        </span>
                    </div>
                )}

                <div class={styles.closeBtn}>
                    <Button onClick={showCloseManageDialog} loading={isClosing.value}>
                        {t('flow.permission.closePermissionManagement')}
                    </Button>
                </div>

                {/* Close manage confirmation dialog */}
                <Dialog
                    isShow={closeObj.isShow}
                    dialogType="show"
                    width={500}
                    quickClose={false}
                    closeIcon={false}
                    onClosed={handleHiddenCloseManage}
                >
                    <div class={styles.closeDialogHeader}>
                        <i class={styles.warningIconLarge}>⚠</i>
                        <p class={styles.closeTitle}>
                            {t('flow.permission.confirmClosePermission', {
                                name: props.resourceName,
                            })}
                        </p>
                    </div>

                    <div class={styles.closeTips}>
                        <p>
                            {t('flow.permission.closePermissionActions', {
                                resourceType: resourceTypeName.value,
                            })}
                        </p>
                        <p>
                            <i class={styles.warningIcon}>⚠</i>
                            {closeManageTips.value || t('flow.permission.removeEditorsExecutorsViewers')}
                        </p>
                        <p>
                            <i class={styles.warningIcon}>⚠</i>
                            {t('flow.permission.deleteInheritedPermissions')}
                        </p>
                        <p>
                            <i class={styles.warningIcon}>⚠</i>
                            {t('flow.permission.deleteGroupInfoAndPermissions')}
                        </p>
                    </div>

                    <div class={styles.confirmClose}>
                        <span class={styles.confirmText}>{t('flow.permission.cannotRecoverWarning')}</span>
                    </div>

                    <div class={styles.optionBtns}>
                        <Button
                            class={styles.closeBtnDanger}
                            theme="danger"
                            onClick={handleCloseManage}
                            loading={isClosing.value}
                        >
                            {t('flow.permission.closePermissionManagement')}
                        </Button>
                        <Button class={styles.cancelBtn} onClick={handleHiddenCloseManage}>
                            {t('flow.permission.cancel')}
                        </Button>
                    </div>
                </Dialog>
            </article>
        )
    },
})
