<template>
    <bk-dropdown-menu
        trigger="click"
        align="center"
    >
        <div
            slot="dropdown-trigger"
        >
            <i class="manage-icon manage-icon-more-fill"></i>
        </div>
        <div slot="dropdown-content">
            <ul
                class="bk-dropdown-list"
                slot="dropdown-content"
            >
                <li
                    v-for="(item, index) in actionConfMenus"
                    :class="['develop-txt', {
                        'develop-txt-disabled': item.disabled
                    }]"
                    :key="index"
                    @click="item.handler"
                    v-bk-tooltips="{
                        content: $t('noDraft'),
                        disabled: item.showTooltips
                    }"
                    v-perm="item.vPerm ? item.vPerm : {}"
                >
                    <template v-if="item.label">
                        {{ item.label }}
                    </template>
                    <template v-else>
                        <component
                            :is="item.component"
                            v-bind="item.componentProps"
                            :disabled="item.disabled"
                        />
                    </template>
                </li>
            </ul>
        </div>
    </bk-dropdown-menu>
</template>

<script>
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry.vue'
    import { RESOURCE_ACTION, RESOURCE_TYPE } from '@/utils/permission'
    import { computed, defineComponent } from 'vue'
    import useInstance from '../hook/useInstance'
    import useTemplateActions from '../hook/useTemplateActions'

    export default defineComponent({
        name: 'PipelineEditMoreAction',
        components: {
            VersionDiffEntry
        },
        props: {
            isTemplate: {
                type: Boolean,
                default: false
            },
            canDebug: {
                type: Boolean,
                default: false
            },
            uniqueId: {
                type: String,
                default: ''
            },
            projectId: {
                type: String,
                default: ''
            }
        },
        setup (props) {
            const { proxy, t } = useInstance()
            const { deleteTemplate } = useTemplateActions()
            const uniqueKey = computed(() => props.isTemplate ? 'templateId' : 'pipelineId')
            const activeVersion = computed(() => proxy.$store.state.atom?.pipelineInfo?.releaseVersion)
            const currentVersion = computed(() => proxy.$store.state.atom?.pipelineInfo?.version ?? '')
            const hasDraftPipeline = computed(() => proxy.$store.getters['atom/hasDraftPipeline'] ?? false)
            const isCommittingPipeline = computed(() => proxy.$store.getters['atom/isCommittingPipeline'] ?? false)
            const draftBaseVersionName = computed(() => proxy.$store.getters['atom/getDraftBaseVersionName'] ?? '--')
            const canExecute = computed(() => proxy.$store.state.atom?.pipelineInfo?.permissions?.canExecute ?? true)
            const pipelineSetting = computed(() => proxy.$store.state.atom?.pipelineSetting ?? {})
            const actionConfMenus = computed(() => {
                const { projectId } = proxy.$route.params
                return [
                    {
                        component: VersionDiffEntry,
                        componentProps: {
                            version: activeVersion.value,
                            latestVersion: currentVersion.value,
                            [uniqueKey.value]: props.uniqueId,
                            theme: 'normal',
                            size: 'small',
                            showButton: false
                        },
                        handler: () => {},
                        disabled: !hasDraftPipeline.value,
                        showTooltips: true
                    },
                    ...(props.isTemplate ? [] : [{
                        label: t('draftExecRecords'),
                        handler: goDraftDebugRecord,
                        disabled: !props.canDebug,
                        vPerm: {
                            hasPermission: canExecute.value,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId,
                                resourceType: props.isTemplate ? RESOURCE_TYPE.TEMPLATE : RESOURCE_TYPE.PIPELINE,
                                resourceCode: props.uniqueId,
                                action: props.isTemplate ? TEMPLATE_RESOURCE_ACTION.EXECUTE : RESOURCE_ACTION.EXECUTE
                            }
                        },
                        showTooltips: true
                    }]),
                    {
                        label: t('deleteDraft'),
                        handler: handelDelete,
                        disabled: !(hasDraftPipeline.value || isCommittingPipeline.value),
                        showTooltips: hasDraftPipeline.value || isCommittingPipeline.value
                    }
                ]
            })

            function goDraftDebugRecord () {
                if (props.canDebug) {
                    proxy.$router.push({
                        name: 'draftDebugRecord'
                    })
                }
            }

            /**
             * 删除草稿
             */
            async function handelDelete () {
                
                const params = {
                    projectId: props.projectId,
                    [uniqueKey.value]: [props.uniqueId]
                }
                const commonConfig = {
                    title: t('sureDeleteDraft'),
                    okText: t('delete'),
                    cancelText: t('cancel'),
                    theme: 'danger',
                    width: 470,
                    confirmLoading: true
                }
                if (isCommittingPipeline.value) {
                    proxy.$bkInfo({
                        ...commonConfig,
                        subTitle: t(props.isTemplate ? 'deleteDraftTemplate' : 'deleteDraftPipeline'),
                        confirmFn: () => deleteConfirm(params)
                    })
                } else if (hasDraftPipeline.value) {
                    proxy.$bkInfo({
                        ...commonConfig,
                        subHeader: createSubHeader(pipelineSetting.value.pipelineName, draftBaseVersionName.value),
                        confirmFn: () => deleteDraftConfirm(params)
                    })
                }
            }

            async function deleteDraftConfirm (params) {
                try {
                    const action = props.isTemplate ? 'templates/deleteTempalteVersion' : 'pipelines/deletePipelineVersion'
                    await proxy.$store.dispatch(action, {
                        ...params,
                        version: currentVersion.value,
                    })
                    
                    proxy.$showTips({
                        message: t('delete') + t('version') + t('success'),
                        theme: 'success'
                    })

                    proxy.$router.push({
                        name: 'PipelineManageList'
                    })
                } catch (err) {
                    proxy.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            }

            function createSubHeader (name, versionName) {
                const h = proxy.$createElement
                return h('div', { class: 'draft-delete' }, [
                    h('p', {
                        class: 'text-overflow',
                        directives: [
                            {
                                name: 'bk-tooltips',
                                value: name
                            }
                        ]
                    }, [
                        h('span', { class: 'label' }, `${t(props.isTemplate ? 'templateName' : 'pipeline')} ：`),
                        h('span', name)
                    ]),
                    h('p', [
                        h('span', { class: 'label' }, `${t('draft')} ：`),
                        h('span', `${t('baseOn', [versionName])} `)
                    ])
                ])
            }

            async function deleteConfirm (params) {
                try {
                    if (props.isTemplate) {
                        await deleteTemplate(params)
                    } else {
                        const { data } = await proxy.$store.dispatch('pipelines/patchDeletePipelines', {
                            projectId: props.projectId,
                            pipelineIds: [props.uniqueId]
                        })
                        const hasErr = Object.keys(data)[0] !== props.uniqueId
                        if (hasErr) {
                            throw Error(t('deleteFail'))
                        }
                    }
                    proxy.$showTips({
                        message: t('delete') + t('version') + t('success'),
                        theme: 'success'
                    })

                    proxy.$router.push({
                        name: 'PipelineManageList'
                    })
                } catch (err) {
                    proxy.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            }

            return {
                actionConfMenus
            }
        }
    })
</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    .manage-icon-more-fill {
        font-size: 20px;
        padding: 3px;

        &:hover,
        &.active {
            background-color: #dddee6;
            color: #3a85ff;
            border-radius: 50%;
        }
    }
    .bk-dropdown-list {
        .develop-txt {
            display: block;
            height: 32px;
            line-height: 33px;
            padding: 0 16px;
            white-space: nowrap;
            font-size: 12px;
            cursor: pointer;
            &:hover {
                background-color: #f0f1f5;
            }
            &.develop-txt-disabled {
                cursor: not-allowed;
                color: #c4c6cc;
            }
        }
    }
    .draft-delete {
        text-align: center;
        color: #43444a;

        p {
            margin-bottom: 14px;
            max-width: 370px;
        }
        .label {
            color: #76777f;
        }
        .text-overflow {
            @include ellipsis();
        }
    }
</style>