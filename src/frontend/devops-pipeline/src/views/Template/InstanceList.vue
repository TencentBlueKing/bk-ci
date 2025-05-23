<template>
    <div
        class="biz-container pipeline-subpages instance-manage-wrapper"
        v-bkloading="{
            isLoading: isLoading
        }"
    >
        <div
            class="sub-view-port"
            v-if="showContent && showInstanceList"
        >
            <section class="info-header">
                <bk-popover
                    class="instance-handle-row"
                    placement="right"
                    :disabled="!disabledBatchBtn"
                    :width="300"
                >
                    <bk-button
                        class="batch-update"
                        :disabled="disabledBatchBtn"
                        @click="batchUpdateInstance()"
                    >
                        <span>{{ $t('template.batchUpdateInstance') }}</span>
                    </bk-button>
                    <div slot="content">
                        <p>{{ $t('template.batchUpdateTip1') }}</p>
                        <p>{{ $t('template.batchUpdateTip2') }}</p>
                    </div>
                </bk-popover>
                <search-select
                    class="search-pipeline-input"
                    v-model="searchValue"
                    :data="searchList"
                    clearable
                    :placeholder="filterTips"
                    :show-condition="false"
                    @change="handleChange"
                />
            </section>
            <section class="instance-table">
                <bk-table
                    :data="instanceList"
                    size="small"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="pageLimitChange"
                    @select="selectItem"
                    @select-all="selectItem"
                >
                    <bk-table-column
                        type="selection"
                        width="60"
                        align="center"
                        :selectable="isUpdating"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('template.pipelineInstanceName')"
                        prop="pipelineName"
                    >
                        <template slot-scope="{ row }">
                            <span
                                class="pipeline-name"
                                :title="row.pipelineName"
                                @click="toPipelineHistory(row.pipelineId)"
                            >
                                {{ row.pipelineName }}
                            </span>
                            <pac-tag
                                v-if="row.enabledPac"
                            />
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('template.currentVision')"
                        prop="pipelineVersionName"
                        :width="300"
                    >
                        <template slot-scope="{ row }">
                            <div class="version-wrapper">
                                {{ row.pipelineVersionName }}
                                <span
                                    class="template-version"
                                    v-if="[TEMPLATE_INSTANCE_PIPELINE_STATUS.PENDING_UPDATE, TEMPLATE_INSTANCE_PIPELINE_STATUS.UPDATED].includes(row.status)"
                                >
                                    {{ $t('template.from') }} {{ row.fromTemplateVersionName }}
                                </span>

                                <template v-if="row.status === TEMPLATE_INSTANCE_PIPELINE_STATUS.PENDING_UPDATE">
                                    <logo
                                        class="update-icon"
                                        name="update"
                                        :size="12"
                                    />
                                </template>

                                <template
                                    v-if="row.status === TEMPLATE_INSTANCE_PIPELINE_STATUS.UPDATING"
                                >
                                    <span class="template-version">
                                        {{ $t('template.Upgrading') }}
                                        <bk-loading
                                            class="loading-icon"
                                            theme="primary"
                                            mode="spin"
                                            size="mini"
                                            is-loading
                                        />
                                    </span>
                                </template>

                                <template
                                    v-if="row.status === TEMPLATE_INSTANCE_PIPELINE_STATUS.FAILED"
                                >
                                    <span class="template-version">
                                        {{ $t('template.UpgradeFailed') }}
                                        <logo
                                            v-bk-tooltips="{
                                                content: row.instanceErrorInfo
                                            }"
                                            class="status-failed-icon"
                                            name="failed-circle-fill"
                                            :size="14"
                                        />
                                    </span>
                                </template>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('template.newestVersion')"
                        :width="140"
                    >
                        <template>
                            <span>{{ currentVersionName }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('template.codeRepo')"
                        prop="repoAliasName"
                    >
                        <template slot-scope="{ row }">
                            {{ row.repoAliasName || '--' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('template.lastModifiedBy')"
                        prop="updater"
                        :width="150"
                    >
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('lastUpdateTime')"
                        prop="updateTime"
                        :width="250"
                    >
                        <template slot-scope="{ row }">
                            <span>{{ localConvertTime(row.updateTime) }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('operate')"
                    >
                        <template slot-scope="{ row }">
                            <bk-button
                                class="mr10"
                                theme="primary"
                                text
                                :disabled="!row.canEdit || !!row.pullRequestUrl"
                                @click="updateInstance(row)"
                            >
                                {{ $t('template.updateInstance') }}
                            </bk-button>
                            <bk-button
                                v-if="row.pullRequestUrl"
                                class="mr10"
                                theme="primary"
                                text
                                @click="HandleMR(row)"
                            >
                                {{ $t('template.handleMR') }}
                            </bk-button>
                            <bk-button
                                class="mr10"
                                theme="primary"
                                text
                                @click="copyAsTemplateInstance(row)"
                                :disabled="!row.canEdit"
                            >
                                {{ $t('copy') }}
                            </bk-button>
                            <version-diff-entry
                                v-if="row.version !== currentVersion"
                                :version="row.version"
                                :latest-version="currentVersion"
                                :pipeline-id="row.pipelineId"
                                type="templateInstance"
                            />
                        </template>
                    </bk-table-column>
                </bk-table>
            </section>
        </div>
        <empty-tips
            v-if="showContent && !showInstanceList"
            :title="emptyTipsConfig.title"
            :desc="emptyTipsConfig.desc"
            :btns="emptyTipsConfig.btns"
        >
        </empty-tips>
    </div>
</template>

<script setup>
    import { computed, ref, watch } from 'vue'
    import { convertTime } from '@/utils/util'
    import {
        SET_INSTANCE_LIST,
        TEMPLATE_INSTANCE_PIPELINE_STATUS
    } from '@/store/modules/templates/constants'
    import PacTag from '@/components/PacTag'
    import Logo from '@/components/Logo'
    import emptyTips from '@/components/pipelineList/imgEmptyTips'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry'
    import UseInstance from '@/hook/useInstance'
    import SearchSelect from '@blueking/search-select'

    const { proxy, showTips, t } = UseInstance()
    const isLoading = ref(false)
    const showContent = ref(false)
    const searchValue = ref([])
    const instanceList = ref([])
    const selectItemList = ref([])
    const pagination = ref({
        current: 1,
        count: 0,
        limit: 10
    })

    const emptyTipsConfig = ref({
        title: t('template.instanceEmptyTitle'),
        desc: t('template.instanceEmptyDesc'),
        btns: [
            {
                theme: 'primary',
                size: 'normal',
                handler: () => createInstance(templateId.value, 'create'),
                text: t('template.addInstance')
            }
        ]
    })
    const showInstanceList = computed(() => showContent.value && (instanceList.value.length || searchValue.value.length))
    const projectId = computed(() => proxy.$route.params.projectId)
    const templateId = computed(() => proxy.$route.params.templateId)
    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)
    const currentVersion = computed(() => pipelineInfo.value?.releaseVersion)
    const currentVersionName = computed(() => pipelineInfo.value?.releaseVersionName)
    const searchParams = computed(() => searchValue.value.reduce((acc, filter) => {
        acc[filter.id] = filter.values.map(val => val.id).join(',')
        return acc
    }, {}))
    const disabledBatchBtn = computed(() => {
        const enabledSet = new Set(selectItemList.value.map(item => item.enabledPac))
        if (enabledSet.size > 1) return true

        const repoValues = selectItemList.value.map(item => item.repoAliasName).filter(Boolean)
        return !(repoValues.length === 0 || new Set(repoValues).size === 1) || !selectItemList.value.length
    })

    const searchList = computed(() => {
        const list = [
            {
                name: proxy.$t('template.pipelineInstanceName'),
                id: 'pipelineName',
                default: true
            },
            {
                name: proxy.$t('template.lastModifiedBy'),
                id: 'updater'
            },
            {
                name: proxy.$t('status'),
                id: 'status',
                children: [
                    {
                        name: proxy.$t('template.instanceStatus.pendingUpdate'),
                        id: TEMPLATE_INSTANCE_PIPELINE_STATUS.PENDING_UPDATE
                    },
                    {
                        name: proxy.$t('template.instanceStatus.updating'),
                        id: TEMPLATE_INSTANCE_PIPELINE_STATUS.UPDATING
                    },
                    {
                        name: proxy.$t('template.instanceStatus.failed'),
                        id: TEMPLATE_INSTANCE_PIPELINE_STATUS.FAILED
                    }
                ]
            },
            {
                name: proxy.$t('versionNum'),
                id: 'templateVersion',
                remoteMethod:
                    async (search) => {
                        const res = await proxy.$store.dispatch('pipelines/requestTemplateVersionList', {
                            projectId: projectId.value,
                            templateId: templateId.value,
                            versionName: search
                        })
                        return res.records.map(item => ({
                            name: item.versionName,
                            id: item.version
                        }))
                    }
            },
            {
                name: proxy.$t('template.codeRepo'),
                id: 'repoHashId',
                remoteMethod:
                    async (search) => {
                        const res = await proxy.$store.dispatch('common/getPACRepoList', {
                            projectId: projectId.value,
                            enabledPac: true,
                            scmType: 'CODE_GIT',
                            permission: 'USE',
                            aliasName: search,
                            page: 1,
                            pageSize: 50
                        })
                        return res.records.map(item => ({
                            name: item.aliasName,
                            id: item.repositoryHashId
                        }))
                    }
            }
        ]
        return list.filter((data) => {
            return !searchValue.value.find(val => val.id === data.id)
        })
    })
    const filterTips = computed(() => searchList.value.map(item => item.name).join(' / '))
    watch(() => searchValue.value, () => {
        requestInstanceList()
    }, {
        immediate: true
    })

    async function requestInstanceList () {
        isLoading.value = true

        try {
            const postData = {
                projectId: projectId.value,
                templateId: templateId.value,
                page: pagination.value.current,
                pageSize: pagination.value.limit,
                ...searchParams.value
            }
            const res = await proxy.$store.dispatch('templates/requestInstanceList', postData)
            instanceList.value = res.records
            pagination.value.count = res.count
        } catch (err) {
            showTips({
                message: err.message || err,
                theme: 'error'
            })
        } finally {
            isLoading.value = false
            showContent.value = true
        }
    }
    async function handlePageChange (page) {
        if (page !== pagination.value.current) {
            pagination.value.current = page
            await requestInstanceList()
        }
    }
    async function pageLimitChange (limit) {
        if (limit !== pagination.value.limit) {
            pagination.value.current = 1
            pagination.value.limit = limit
            await requestInstanceList()
        }
    }
    function selectItem (items) {
        selectItemList.value = items.filter(i => i.canEdit)
        return items.filter(i => i.canEdit)
    }
    function isUpdating (row) {
        return row.status !== 'UPDATING' && row.canEdit
    }
    function localConvertTime (timestamp) {
        return convertTime(timestamp)
    }
    function createInstance (templateId, type = 'create') {
        const route = {
            name: 'instanceEntry',
            params: {
                projectId: projectId.value,
                templateId,
                version: pipelineInfo.value?.releaseVersion,
                type
            }
        }
        proxy.$router.push(route)
    }
    function updateInstance (row) {
        if (row.canEdit) {
            proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, [row])
            createInstance(row.templateId, 'upgrade')
        }
    }
    function HandleMR (row) {
        window.open(row.pullRequestUrl, '_blank')
    }
    function copyAsTemplateInstance (row) {
        const route = {
            name: 'instanceEntry',
            params: {
                version: currentVersion.value,
                instanceName: (row.pipelineName + '_copy').substring(0, 128)
            }
        }
        proxy.$router.push(route)
    }
    function toPipelineHistory (pipelineId) {
        const url = `${WEB_URL_PREFIX}/pipeline/${projectId.value}/${pipelineId}/history`
        window.open(url, '_blank')
    }
    function batchUpdateInstance () {
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, selectItemList.value)
        proxy.$router.push({
            name: 'instanceEntry',
            params: {
                ...proxy.$route.params,
                version: pipelineInfo.value?.releaseVersion,
                type: 'upgrade'
            }
        })
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .pipeline-subpages {
        min-height: 100%;
    }
    .instance-manage-wrapper {
        flex-direction: column;
        .instance-header {
            .bk-button-normal {
                margin-top: -6px;
                padding: 0 10px;
                font-size: 12px;
            }
        }
        .sub-view-port {
            padding: 20px;
            height: calc(100% - 60px);
            overflow: auto;
        }
        .info-header {
            display: flex;
            justify-content: space-between;
        }
        .bk-form-control {
            display: inline-table;
            width: 200px;
        }
        .search-pipeline-input {
            width: 680px;
            background: white;
            ::placeholder {
                color: #c4c6cc;
            }
        }
        .instance-handle-row {
            display: flex;
            margin-bottom: 20px;
            button {
                margin-right: 4px;
                padding: 0 11px;
                font-size: 12px;
            }
        }
        .instance-handle-row-right {
            .right-icon {
                top: 16px;
            }
        }
        .instance-table {
            .pipeline-name {
                color: $primaryColor;
                cursor: pointer;
                margin-right: 5px;
            }
            .disabled-checkbox {
                position: absolute;
                left: -60px;
                top:  0;
            }
            .status-card {
                max-width: 120px;
                font-size: 12px;
                text-align: center;
                color: #fff;
                background-color: #39E084;
            }
            .need-update {
                background-color: #F6B026;
            }
            .update-failed {
                background-color: $dangerColor;
            }
            .updating {
                background-color: $primaryColor;
            }
            .update-btn,
            .compared-btn {
                display: inline-block;
                margin-right: 20px;
                cursor: pointer;
            }
            .template-version {
                color: #979BA5;
                margin: 0 5px;
            }
            .version-wrapper {
                display: table;
                align-items: center;
            }
            .update-icon {
                position: relative;
                top: 2px;
            }
            .status-failed-icon {
                cursor: pointer;
            }
            .loading-icon {
                display: ruby;
                .bk-spin-loading {
                    margin: auto;
                    padding-left: 12px;
                }
            }
        }
        .batch-update {
            padding: 0 11px;
            font-size: 12px;
        }
    }
</style>
