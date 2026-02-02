/**
 * PermissionSettings Page
 * Flow permission management settings page
 */

import { PermissionMain } from '@/components/Permission';
import { useFlowInfo } from '@/hooks/useFlowInfo';
import { computed, defineComponent } from 'vue';
import { useRoute } from 'vue-router';
import styles from './permissionSettings.module.css';

export default defineComponent({
  name: 'PermissionSettings',

  setup() {
    const route = useRoute();
    const { flowInfo } = useFlowInfo();

    /**
     * Get project code from route params
     */
    const projectCode = computed(() => route.params.projectId as string);

    /**
     * Get pipeline/flow ID (resource code)
     */
    const resourceCode = computed(() => flowInfo.value?.pipelineId || '');

    /**
     * Get pipeline/flow name (resource name)
     */
    const resourceName = computed(() => flowInfo.value?.pipelineName || '');

    return () => (
      <div class={styles.permissionSettings}>
        {resourceCode.value && (
          <PermissionMain
            projectCode={projectCode.value}
            resourceType="creative_stream"
            resourceCode={resourceCode.value}
            resourceName={resourceName.value}
            showCreateGroup={false}
          />
        )}
      </div>
    );
  },
});