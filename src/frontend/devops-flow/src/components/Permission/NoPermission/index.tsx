/**
 * NoPermission Component
 * Displays when the user has no management permission
 * Shows either PermissionTips or GroupTable based on resource type
 */

import { defineComponent, type PropType } from 'vue';
import { RESOURCE_TYPES } from '../constants';
import GroupTable from '../GroupTable';
import type { ResourceType } from '../types';
import styles from './index.module.css';

export default defineComponent({
  name: 'NoPermission',

  props: {
    /**
     * Title text
     */
    title: {
      type: String,
      default: '',
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
    /**
     * Error code from API
     */
    errorCode: {
      type: [String, Number],
      default: '',
    },
  },

  setup(props) {
    return () => {
      // For project type, show a different view (PermissionTips)
      // For other types, show the GroupTable
      if (props.resourceType === RESOURCE_TYPES.PROJECT) {
        // TODO: Implement PermissionTips component if needed
        return (
          <div class={styles.noPermission}>
            <div class={styles.noPermissionContent}>
              {/* Placeholder for PermissionTips */}
              <span>{props.title}</span>
            </div>
          </div>
        );
      }

      return (
        <div class={styles.noPermission}>
          <GroupTable
            resourceType={props.resourceType}
            resourceCode={props.resourceCode}
            projectCode={props.projectCode}
            ajaxPrefix={props.ajaxPrefix}
          />
        </div>
      );
    };
  },
});
