/**
 * PermissionMain Component
 * Main entry component for permission management
 * Handles permission status detection and renders appropriate sub-components
 */

import { bkLoading as vBkLoading } from 'bkui-vue/lib/directives';
import {
    computed,
    defineComponent,
    nextTick,
    onMounted,
    type PropType,
    ref,
    watch,
} from 'vue';
import { hasManagerPermission, isEnablePermission } from '../api';
import { ERROR_CODES } from '../constants';
import NoEnablePermission from '../NoEnablePermission';
import NoPermission from '../NoPermission';
import PermissionManage from '../PermissionManage';
import type { ResourceType } from '../types';
import styles from './index.module.css';

export default defineComponent({
    name: 'PermissionMain',

    directives: {
        bkLoading: vBkLoading,
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
         * Resource name
         */
        resourceName: {
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

    setup(props) {
        const isEnablePermissionValue = ref(false);
        const hasPermission = ref(false);
        const isLoading = ref(true);
        const isApprover = ref(false);
        const errorCode = ref<number | string>('');

        /**
         * Computed permission resource key for watching changes
         */
        const permissionResource = computed(() => {
            return `${props.projectCode}/${props.resourceType}/${props.resourceCode}`;
        });

        /**
         * Initialize permission status
         * Fetches both hasManagerPermission and isEnablePermission
         */
        const initStatus = async () => {
            isLoading.value = true;
            isApprover.value = false;
            errorCode.value = '';

            const commonPrefix = props.ajaxPrefix;

            try {
                const [hasManagerRes, isEnableRes] = await Promise.all([
                    hasManagerPermission(
                        props.projectCode,
                        props.resourceType,
                        props.resourceCode,
                        commonPrefix,
                    ),
                    isEnablePermission(
                        props.projectCode,
                        props.resourceType,
                        props.resourceCode,
                        commonPrefix,
                    ),
                ]);
                isEnablePermissionValue.value = isEnableRes ?? false;
                hasPermission.value = hasManagerRes ?? false;
            } catch (err) {
                const error = err as { code?: typeof ERROR_CODES[keyof typeof ERROR_CODES] };
                const code = error.code;

                if (
                    code &&
                    [
                        ERROR_CODES.NOT_FOUND,
                        ERROR_CODES.FORBIDDEN,
                        ERROR_CODES.SPECIAL_ERROR,
                    ].includes(code)
                ) {
                    isApprover.value = true;
                    errorCode.value = code;
                }
            } finally {
                isLoading.value = false;
            }
        };

        /**
         * Handle close manage event
         * Refresh permission status
         */
        const handleCloseManage = () => {
            initStatus();
        };

        /**
         * Handle open manage event
         * Refresh permission status
         */
        const handleOpenManage = () => {
            initStatus();
        };

        // Watch for resource changes and reinitialize status
        watch(
            permissionResource,
            () => {
                nextTick(() => {
                    initStatus();
                });
            },
        );

        // Initialize on mount
        onMounted(() => {
            initStatus();
        });

        return () => (
            <article
                class={styles.permissionWrapper}
                v-bkLoading={{ loading: isLoading.value }}
            >
                {isEnablePermissionValue.value && !isLoading.value && (
                    <>
                        {hasPermission.value ? (
                            <PermissionManage
                                showCreateGroup={props.showCreateGroup}
                                resourceType={props.resourceType}
                                resourceCode={props.resourceCode}
                                resourceName={props.resourceName}
                                projectCode={props.projectCode}
                                ajaxPrefix={props.ajaxPrefix}
                                onClose-manage={handleCloseManage}
                            />
                        ) : (
                            <NoPermission
                                resourceType={props.resourceType}
                                resourceCode={props.resourceCode}
                                projectCode={props.projectCode}
                                ajaxPrefix={props.ajaxPrefix}
                            />
                        )}
                    </>
                )}

                {!isEnablePermissionValue.value && !isLoading.value && (
                    <>
                        {isApprover.value ? (
                            <NoPermission
                                resourceType={props.resourceType}
                                resourceCode={props.resourceCode}
                                projectCode={props.projectCode}
                                ajaxPrefix={props.ajaxPrefix}
                                errorCode={errorCode.value}
                            />
                        ) : (
                            <NoEnablePermission
                                resourceType={props.resourceType}
                                resourceCode={props.resourceCode}
                                projectCode={props.projectCode}
                                ajaxPrefix={props.ajaxPrefix}
                                hasPermission={hasPermission.value}
                                onOpen-manage={handleOpenManage}
                            />
                        )}
                    </>
                )}
            </article>
        );
    },
});
