/**
 * PermissionManage Component
 * Main permission management view with GroupAside and IamIframe
 */

import { defineComponent, type PropType, ref } from 'vue'
import GroupAside from '../GroupAside'
import IamIframe from '../IamIframe'
import type { GroupInfo, ResourceType } from '../types'
import styles from './index.module.css'

export default defineComponent({
  name: 'PermissionManage',

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
     * Whether to show create group button
     */
    showCreateGroup: {
      type: Boolean,
      default: true,
    },
    /**
     * Resource name
     */
    resourceName: {
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

  emits: ['close-manage'],

  setup(props, { emit }) {
    const path = ref('')
    const tabName = ref('member')

    /**
     * Handle group detail tab change from IAM iframe
     */
    const handleChangeGroupDetailTab = (tab: string) => {
      if (tab) {
        tabName.value = tab
      }
    }

    /**
     * Handle group selection
     * Build iframe path with groupId, role_id and tab parameters
     */
    const handleChooseGroup = (group: GroupInfo) => {
      const roleId = group.managerId || ''
      const tab = tabName.value || 'member'
      path.value = `user-group-detail/${group.groupId}?role_id=${roleId}&tab=${tab}`
    }

    /**
     * Handle create group
     */
    const handleCreateGroup = () => {
      path.value = 'create-user-group'
    }

    /**
     * Handle close permission management
     */
    const handleCloseManage = () => {
      emit('close-manage')
    }

    return () => (
      <section class={styles.permissionManage}>
        <GroupAside
          showCreateGroup={props.showCreateGroup}
          resourceType={props.resourceType}
          resourceCode={props.resourceCode}
          resourceName={props.resourceName}
          projectCode={props.projectCode}
          ajaxPrefix={props.ajaxPrefix}
          onChooseGroup={handleChooseGroup}
          onCreateGroup={handleCreateGroup}
          onCloseManage={handleCloseManage}
          onChangeGroupDetailTab={handleChangeGroupDetailTab}
        />
        {path.value && <IamIframe path={path.value} />}
      </section>
    )
  },
})
