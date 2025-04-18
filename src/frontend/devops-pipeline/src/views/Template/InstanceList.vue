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
                <div
                    class="instance-handle-row"
                    v-bk-tooltips="updateConfig"
                >
                    <bk-button
                        class="batch-update"
                        :disabled="!selectItemList.length"
                        @click="batchUpdateInstance()"
                    >
                        <span>{{ $t('template.batchUpdateInstance') }}</span>
                    </bk-button>
                </div>
                <div id="update-html">
                    <p>{{ $t('template.batchUpdateTip1') }}</p>
                    <p>{{ $t('template.batchUpdateTip2') }}</p>
                </div>
                <!-- <bk-input
                    :placeholder="$t('search')"
                    ext-cls="instance-handle-row-right"
                    :clearable="true"
                    right-icon="icon-search"
                    v-model="searchKey"
                    @enter="query"
                    @clear="query"
                >
                </bk-input> -->
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
                        <template slot-scope="props">
                            <span
                                class="pipeline-name"
                                :title="props.row.pipelineName"
                                @click="toPipelineHistory(props.row.pipelineId)"
                            >{{ props.row.pipelineName }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('template.currentVision')"
                        prop="versionName"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('template.newestVersion')"
                    >
                        <template>
                            <span>{{ currentVersionName }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('template.codeRepo')"
                        prop="codeRepo"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('template.lastModifiedBy')"
                        prop="updater"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('lastUpdateTime')"
                        prop="updateTime"
                    >
                        <template slot-scope="props">
                            <span>{{ localConvertTime(props.row.updateTime) }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('operate')"
                        width="250"
                    >
                        <template slot-scope="props">
                            <bk-button
                                class="mr10"
                                theme="primary"
                                text
                                :disabled="!props.row.hasPermission"
                                @click="updateInstance(props.row)"
                            >
                                {{ $t('template.updateInstance') }}
                            </bk-button>
                            <bk-button
                                class="mr10"
                                theme="primary"
                                text
                                :disabled="!props.row.hasPermission"
                                @click="HandleMR(props.row)"
                            >
                                {{ $t('template.handleMR') }}
                            </bk-button>
                            <bk-button
                                class="mr10"
                                theme="primary"
                                text
                                @click="copyAsTemplateInstance(props.row)"
                                :disabled="!props.row.hasPermission"
                            >
                                {{ $t('copy') }}
                            </bk-button>
                            <bk-button
                                theme="primary"
                                text
                                @click="toCompared(props.row)"
                            >
                                {{ $t('template.diff') }}
                            </bk-button>
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
        <instance-compared
            :show-compared-instance="showComparedInstance"
            :loading="dialogLoading"
            :instance-version="instanceVersion"
            :cur-version="currentVersion"
            :version-list="versionList"
            :cur-params-list="curParamsList"
            :target-params-list="targetParamsList"
            :cur-stages="curStages"
            :target-stages="targetStages"
            @cancel="cancelHandler"
            :selected-version="selectedVersion"
        />
    </div>
</template>

<script setup>
    import emptyTips from '@/components/pipelineList/imgEmptyTips'
    import instanceCompared from '@/components/Template/instance-compared.vue'
    import UseInstance from '@/hook/useInstance'
    import { SET_INSTANCE_LIST } from '@/store/modules/templates/constants'
    import { convertTime } from '@/utils/util'
    import { computed, onMounted, ref, watch } from 'vue'

    const { proxy, showTips, t } = UseInstance()
    const isInit = ref(false)
    const isLoading = ref(false)
    const searchable = ref(false)
    const showContent = ref(false)
    const dialogLoading = ref(false)
    const showComparedInstance = ref(false)
    const instanceVersion = ref('')
    const searchValue = ref([])
    const instanceList = ref([])
    const selectItemList = ref([])
    const versionList = ref([])
    const curStages = ref([])
    const targetStages = ref([])
    const targetParamsList = ref([])
    const curParamsList = ref([])
    const pagination = ref({
        current: 1,
        count: 0,
        limit: 10
    })
    const updateConfig = ref({
        allowHTML: true,
        width: 290,
        placement: 'top-middle',
        content: '#update-html'
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
    const curComparedPipeline = ref('')

    const showInstanceList = computed(() => showContent.value && (instanceList.value.length || searchable.value))
    const projectId = computed(() => proxy.$route.params.projectId)
    const templateId = computed(() => proxy.$route.params.templateId)
    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)

    const currentVersion = computed(() => pipelineInfo.value?.releaseVersion)
    const currentVersionName = computed(() => pipelineInfo.value?.releaseVersionName)

    const searchParams = computed(() => searchValue.value.reduce((acc, filter) => {
        acc[filter.id] = filter.values.map(val => val.id).join(',')
        return acc
    }, {}))

    watch(() => instanceVersion.value, (newVal) => {
        if (newVal && !isInit.value) {
            requestVersionCompare(newVal)
        }
    })

    onMounted(() => {
        requestInstanceList()
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
        selectItemList.value = items.filter(i => i.hasPermission)
        return items.filter(i => i.hasPermission)
    }
    function isUpdating (row) {
        return row.status !== 'UPDATING' && row.hasPermission
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
        if (row.hasPermission) {
            proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, [row])
            createInstance(row.templateId, 'upgrade')
        }
    }
    async function requestVersionCompare (versionId) {
        dialogLoading.value = true

        try {
            const res = await proxy.$store.dispatch('pipelines/requestVersionCompare', {
                projectId: projectId.value,
                templateId: templateId.value,
                version: versionId
                // templateId: curComparedPipeline.value
            })

            versionList.value = res.versions ?? []
            const curVersion = versionList.find(val => {
                return val.version === parseInt(versionId)
            })
            instanceVersion.value = curVersion?.version

            const curData = res.origin
            const targetData = res.target

            curStages.value = curData.model.stages
            targetStages.value = targetData.model.stages

            curParamsList.value.splice(0, curParamsList.value.length)
            targetParamsList.value.splice(0, targetParamsList.value.length)

            curData.params && curData.params.forEach((item) => {
                const temp = {
                    key: item.id,
                    value: item.defaultValue
                }
                curParamsList.value.push(temp)
            })

            targetData.params && targetData.params.forEach((item) => {
                const temp = {
                    key: item.id,
                    value: item.defaultValue
                }
                targetParamsList.value.push(temp)
            })
        } catch (err) {
            showTips({
                message: err.message || err,
                theme: 'error'
            })
        } finally {
            dialogLoading.value = false
        }
    }
    function HandleMR (row) {
        // TO DO
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
    function toCompared (row) {
        showComparedInstance.value = true
        isInit.value = true
        curComparedPipeline.value = row.pipelineId
        requestVersionCompare(row.version)
    }
    function cancelHandler () {
        showComparedInstance.value = false
    }
    function selectedVersion (data) {
        isInit.value = false
        instanceVersion.value = data
    }
    function toPipelineHistory () {
        // to do..
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
        }
        .batch-update {
            padding: 0 11px;
            font-size: 12px;
        }
    }
</style>
