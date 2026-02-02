/**
 * NoEnablePermission Component
 * Displays when permission management is not enabled for a resource
 */

import { Button, Exception } from 'bkui-vue';
import { computed, defineComponent, type PropType, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { enablePermission } from '../api';
import { getNoEnablePermissionTitle } from '../constants';
import type { ResourceType } from '../types';
import styles from './index.module.css';

export default defineComponent({
  name: 'NoEnablePermission',

  props: {
    /**
     * Whether the user has permission to enable permission management
     */
    hasPermission: {
      type: Boolean,
      default: false,
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
  },

  emits: ['open-manage'],

  setup(props, { emit }) {
    const { t } = useI18n();

    const isOpenManageLoading = ref(false);

    /**
     * Title text based on resource type
     */
    const title = computed(() =>
      getNoEnablePermissionTitle(props.resourceType, (key) =>
        t(`flow.permission.${key}`),
      ),
    );

    /**
     * Handle enable permission management
     */
    const handleOpenManage = async () => {
      isOpenManageLoading.value = true;
      try {
        await enablePermission(
          props.projectCode,
          props.resourceType,
          props.resourceCode,
          props.ajaxPrefix,
        );
        emit('open-manage');
      } finally {
        isOpenManageLoading.value = false;
      }
    };

    return () => (
      <article class={styles.noEnablePermission}>
        <div class={styles.contentWrapper}>
          <Exception type="403" scene="part" class={styles.exceptionPart}>
            <p class={styles.titleText}>{title.value}</p>
            <Button
              class={styles.enableBtn}
              theme="primary"
              disabled={!props.hasPermission}
              loading={isOpenManageLoading.value}
              onClick={handleOpenManage}
            >
              {t('flow.permission.enablePermissionManagement')}
            </Button>
          </Exception>
        </div>
      </article>
    );
  },
});
