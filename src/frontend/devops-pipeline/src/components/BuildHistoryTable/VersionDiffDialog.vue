<template>
    <bk-dialog
        v-model="visible"
        header-position="left"
        :title="$t('pipelineVersionChnaged', [buildNum])"
        :width="640"
        @confirm="handleClose"
        :before-close="handleClose"
    >
        <div
            class="build-pipeline-version-diff-dialog"
            v-bkloading="{ isLoading }"
        >
            <h3>
                {{ $t('templateDiffs') }}
            </h3>
            <p class="version-diff-area">
                {{ buildVersionDiffState?.prevVersionName }}
                <span class="bk-icon icon-arrows-right-shape"></span>
                {{ buildVersionDiffState?.currVersionName }}

                
                <VersionDiffEntry
                    class="check-diff-btn"
                    :version="buildVersionDiffState?.prevVersion"
                    :latest-version="buildVersionDiffState?.currVersion"
                >
                    {{ $t("checkDiff") }}
                </VersionDiffEntry>
            </p>
            <template v-if="Array.isArray(buildVersionDiffState?.buildVersionDiffs) && buildVersionDiffState.buildVersionDiffs.length > 0">
                <h3>
                    {{ $t('templateVersionDiff') }}
                </h3>
                <p class="ref-template-diff-table-title">
                    {{ $t('refTemplateDiffTitle') }}
                </p>
                <bk-table
                    :data="buildVersionDiffState.buildVersionDiffs"
                >
                    <bk-table-column
                        :label="$t('refTemplate')"
                        prop="templateName"
                    >
                        <template #default="{ row }">
                            <div class="build-diff-template-version-cell">
                                <p @click="goTemplate(row)">
                                    {{ row.templateName }}
                                </p>
                                <span class="build-diff-template-version-name">
                                    {{ row.templateVersionName }}
                                </span>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('preRef')"
                        prop="prevTemplateVersionRef"
                    />
                    <bk-table-column
                        :label="$t('currentRef')"
                        prop="currTemplateVersionRef"
                    />
                    <bk-table-column
                        :label="$t('operation')"
                    >
                        <template #default="{ row }">
                            <VersionDiffEntry
                                force-template
                                :template-id="row.templateId"
                                :version="row.prevTemplateVersion"
                                :latest-version="row.currTemplateVersion"
                            >
                                {{ $t("checkDiff") }}
                            </VersionDiffEntry>
                        </template>
                    </bk-table-column>
                </bk-table>
            </template>
        </div>
    </bk-dialog>
</template>


<script>
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry.vue'
    import UseInstance from '@/hook/useInstance'
    import { defineComponent, ref, watch } from 'vue'
    

    export default defineComponent({
        name: 'VersionDiffDialog',
        props: {
            buildNum: {
                type: String,
                required: true,
            },
            buildId: {
                type: String,
                required: true,
            },
            visible: {
                type: Boolean,
                required: false,
            },
        },
        components: {
            VersionDiffEntry,
        },
        emits: ['update:visible'],
        setup (props, { emit }) {
            const isLoading = ref(false)
            const buildVersionDiffState = ref()
            const { proxy } = UseInstance()

            watch(() => props.visible, (visible) => {
                if (!visible) {
                    return
                }
                fetchBuildVersionDiff()
            }, { immediate: true })

            function handleClose () {
                emit('update:visible', false)
            }

            function goTemplate ({ projectId, templateId, }) {
                // todo
                proxy.$router.push({
                    name: 'TemplateOverview',
                    params: {
                        projectId,
                        templateId,
                    },
                })
            }

            async function fetchBuildVersionDiff () {
                try {
                    isLoading.value = true
                    const { projectId, pipelineId } = proxy.$route.params
                    const res = await proxy.$ajax.post(`/process/api/user/builds/${projectId}/${pipelineId}/${props.buildId}/buildVersionDiff`)
                    buildVersionDiffState.value = res.data
                } catch (error) {
                    buildVersionDiffState.value = {
                        prevVersion: 1025348,
                        prevVersionName: 'V8(P8.T7.5)',
                        currVersion: 1025347,
                        currVersionName: 'V5(P2.T3.3)',
                        templateId: '8236b3567f7643379e5f367cdd3330fd',
                        buildVersionDiffs: [
                            {
                                projectId: 'lockie',
                                templateId: '8236b3567f7643379e5f367cdd3330fd',
                                templateName: 'stage 模板 1',
                                templateVersionName: 'dev',
                                prevTemplateVersion: 1025348,
                                currTemplateVersion: 1025347,
                                prevTemplateVersionRef: 'a58c06f',
                                currTemplateVersionRef: 'a2e4b6'
                            },
                            {
                                projectId: 'lockie',
                                templateId: '8236b3567f7643379e5f367cdd3330fd',
                                templateName: 'stage 模板 2',
                                templateVersionName: 'latest',
                                prevTemplateVersion: 1025348,
                                currTemplateVersion: 1025347,
                                prevTemplateVersionRef: 'v9(P8.T7.5)',
                                currTemplateVersionRef: 'V10(P2.T3.3)'
                            },
                            {
                                projectId: 'lockie',
                                templateId: '8236b3567f7643379e5f367cdd3330fd',
                                templateName: 'job 模板 1',
                                templateVersionName: '/refs/head/test',
                                prevTemplateVersion: 1025348,
                                currTemplateVersion: 1025347,
                                prevTemplateVersionRef: 'c6d8e9',
                                currTemplateVersionRef: '3a5d7f'
                            }
                        ]
                    }
                } finally {
                    isLoading.value = false
                }
            }

            return {
                buildVersionDiffState,
                isLoading,
                goTemplate,
                handleClose,
            }
        },
    })
</script>

<style lang="scss">
    @import '@/scss/conf';
    @import '@/scss/mixins/ellipsis';
    .build-pipeline-version-diff-dialog {
        > h3 {
            font-size: 14px;
            color: #4D4F56;
            line-height: 22px;
            margin: 24px 0 8px 0;
            display: flex;
            align-items: center;
            &:before {
                content: '';
                display: inline-block;
                width: 4px;
                height: 16px;
                background: #3A84FF;
                border-radius: 0 2px 2px 0;
                margin-right: 8px;
            }
        }
        .version-diff-area {
            display: flex;
            background: #F5F7FA;
            border-radius: 2px;
            align-items: center;
            padding: 16px 24px;
            font-size: 12px;
            .icon-arrows-right-shape {
                margin: 0 18px;
                color: #F59500;
            }
            .check-diff-btn {
                margin-left: auto;
                justify-self: flex-end;
            }
        }
        .ref-template-diff-table-title {
            margin-bottom: 8px;
            color: #4D4F56;
            font-size: 12px;
            line-height: 20px;
        }
        .build-diff-template-version-cell {
            display: flex;
            flex-direction: column;
            > p {
                color: $primaryColor;
                cursor: pointer;
                @include ellipsis();
            }
            .build-diff-template-version-name {
                color: #979BA5;
                font-size: 12px;
                line-height: 20px;
                @include ellipsis();
            }
        }
    }
</style>