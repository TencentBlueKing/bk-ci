/**
 * IamIframe Component
 * Embeds IAM (Identity and Access Management) iframe for user group management
 */

import { computed, defineComponent, type PropType, watch } from 'vue';
import { IAM_CONFIG } from '../constants';
import styles from './index.module.css';

export default defineComponent({
  name: 'IamIframe',

  props: {
    /**
     * Path to the IAM page
     */
    path: {
      type: String,
      default: '',
    },
    /**
     * Additional query parameters for the iframe URL
     */
    query: {
      type: Object as PropType<Record<string, string>>,
      default: () => ({}),
    },
  },

  setup(props) {
    /**
     * Compute the full iframe URL
     * Combines BK_IAM_URL_PREFIX with path and query parameters
     */
    const iframeUrl = computed(() => {
      if (!props.path) {
        return '';
      }

      try {
        // Get IAM URL prefix from global window config
        const iamUrlPrefix = (
          (window as unknown as { BK_IAM_URL_PREFIX?: string })
            .BK_IAM_URL_PREFIX || ''
        ).replace(/\/+$/, ''); // Remove trailing slashes

        const pathWithoutLeadingSlash = props.path.replace(/^\/+/, ''); // Remove leading slashes
        const url = new URL(`${iamUrlPrefix}/${pathWithoutLeadingSlash}`);

        // Add common query parameters
        url.searchParams.append('system_id', IAM_CONFIG.SYSTEM_ID);
        url.searchParams.append('source', IAM_CONFIG.SOURCE);

        // Add custom query parameters
        if (props.query) {
          Object.entries(props.query).forEach(([key, value]) => {
            if (value !== undefined && value !== null) {
              url.searchParams.append(key, String(value));
            }
          });
        }

        return url.href;
      } catch (error) {
        console.error('Failed to construct IAM iframe URL:', error);
        return '';
      }
    });

    // Watch for path/query changes to log URL updates (for debugging)
    watch(
      () => [props.path, props.query],
      () => {
        if (iframeUrl.value) {
          console.log('IAM iframe URL updated:', iframeUrl.value);
        }
      },
      { deep: true },
    );

    return () => (
      <iframe
        class={styles.iamIframe}
        src={iframeUrl.value}
        frameborder="0"
        width="100%"
        height="100%"
        title="IAM Management"
      />
    );
  },
});
