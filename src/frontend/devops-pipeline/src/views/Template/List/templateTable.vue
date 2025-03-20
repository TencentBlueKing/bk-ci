<template>
    <bk-table
        ext-cls="list-table"
        v-bkloading="{ isLoading }"
        ref="tableRef"
        :data="data"
        :size="tableSize"
        :max-height="718"
        :pagination="pagination"
        @page-change="handlePageChange"
        @page-limit-change="handlePageLimitChange"
        @header-dragend="handelHeaderDragend"
    >
        <TemplateEmpty
            v-if="!data.length"
            slot="empty"
            type="search-empty"
            @clear="clearFilter"
        />
        <bk-table-column
            :label="$t('template.name')"
            prop="name"
            :width="tableWidthMap.name"
            sortable
        >
            <template slot-scope="{ row }">
                <div
                    class="template-name select-text"
                    @click="goEdit(row)"
                >
                    <img :src="row.logoUrl">
                    <span :title="row.name">{{ row.name }}</span>
                    <img
                        v-if="row.enablePac"
                        src="../../../images/pacIcon.png"
                        class="pac-code-icon"
                    />
                </div>
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.desc"
            :width="tableWidthMap.desc"
            :label="$t('template.desc')"
            prop="desc"
        >
            <template slot-scope="{ row }">
                <span>{{ row.desc || '--' }}</span>
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.type"
            :width="tableWidthMap.type"
            :label="$t('template.type')"
            prop="type"
            :filters="sourceFilters"
            :filter-method="sourceFilterMethod"
            :filter-multiple="false"
        >
            <template slot-scope="{ row }">
                <span>{{ TEMPLATE_TYPE[row.type] || '--' }}</span>
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.lastedVersion"
            :width="tableWidthMap.lastedVersion"
            :label="$t('template.lastedVersion')"
            prop="lastedVersion"
        >
            <template slot-scope="{ row }">
                <span>{{ row.lastedVersion || '--' }}</span>
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.source"
            :width="tableWidthMap.source"
            :label="$t('template.source')"
            prop="sourceName"
        >
            <template slot-scope="{ row }">
                <div class="source-name">
                    <span>{{ row.sourceName }}</span>
                    <bk-badge
                        class="mr40"
                        dot
                        :theme="'danger'"
                        v-if="row.storeFlag"
                    >
                        <Logo
                            size="14"
                            name="is-store"
                        />
                    </bk-badge>
                </div>
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.debugPipelineCount"
            :width="tableWidthMap.debugPipelineCount"
            :label="$t('template.debugPipelineCount')"
            prop="debugPipelineCount"
        >
            <template slot-scope="{ row }">
                <span class="select-text">{{ row.debugPipelineCount }}</span>
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.instancePipelineCount"
            :width="tableWidthMap.instancePipelineCount"
            :label="$t('template.instancePipelineCount')"
            prop="instancePipelineCount"
        >
            <template slot-scope="{ row }">
                <span class="select-text">{{ row.debugPipelineCount }}</span>
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.updater"
            :width="tableWidthMap.updater"
            :label="$t('template.lastModifiedBy')"
            prop="updater"
        >
            <template slot-scope="{ row }">
                <span>{{ row.updater || '--' }}</span>
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.updateTime"
            :width="tableWidthMap.updateTime"
            key="updateTime"
            :label="$t('template.lastModifiedDate')"
            sortable
            prop="updateTime"
            :formatter="formatTime"
        />
        <bk-table-column
            :width="tableWidthMap.operate"
            :label="$t('operate')"
            :min-width="100"
        >
            <div
                slot-scope="{ row }"
                class="template-operate"
            >
                <span
                    @click="toInstanceList(row)"
                    :class="['action', row.canEdit ? 'create-permission' : 'not-create-permission']"
                >
                    <span v-if="row.type === 'PIPELINE'">
                        {{ $t('template.instantiate') }}
                    </span>
                </span>
                <ext-menu
                    type="template"
                    :data="row"
                    :config="row.templateActions"
                />
            </div>
        </bk-table-column>
        <bk-table-column
            type="setting"
        >
            <bk-table-setting-content
                :fields="tableColumn"
                :selected="selectedTableColumn"
                :size="tableSize"
                @setting-change="handleSettingChange"
            />
        </bk-table-column>
    </bk-table>
</template>

<script>
    import { onMounted, ref, computed, toRefs } from '@vue/composition-api'
    import ExtMenu from './extMenu'
    import Logo from '@/components/Logo'
    import TemplateEmpty from '@/components/common/exception'
    import UseInstance from '@/hook/useInstance'
    import {
        TEMPLATE_TABLE_COLUMN_CACHE,
        CACHE_TEMPLATE_TABLE_WIDTH_MAP
    } from '@/store/modules/templates/constants'
    import { convertTime } from '@/utils/util'

    export default {
        components: {
            ExtMenu,
            Logo,
            TemplateEmpty
        },
        props: {
            data: {
                type: Array,
                default: () => []
            },
            isLoading: {
                type: Boolean,
                default: false
            },
            pagination: {
                type: Object,
                default: () => ({
                    current: 1,
                    count: 6,
                    limit: 20
                })
            }
        },
        setup (props, { emit, root }) {
            if (!root) return
            const { proxy, i18n } = UseInstance()
            const { data, isLoading, pagination } = toRefs(props)

            const TEMPLATE_TRANSLATIONS = {
                PIPELINE: 'template.pipelineTemplate',
                STAGE: 'template.stageTemplate',
                JOB: 'template.jobTemplate',
                STEP: 'template.stepTemplate',
                ALL: 'template.allTemplate'
            }
            const tableSize = ref('medium')
            const tableColumn = ref([])
            const selectedTableColumn = ref([])
            const tableWidthMap = ref({})

            const sourceFilters = computed(() => ['PIPELINE', 'STAGE', 'JOB', 'STEP'].map(type => ({
                text: i18n.t(TEMPLATE_TRANSLATIONS[type]),
                value: type
            })))
            const TEMPLATE_TYPE = computed(() => {
                const types = {}
                Object.keys(TEMPLATE_TRANSLATIONS).forEach(type => {
                    types[type] = i18n.t(TEMPLATE_TRANSLATIONS[type])
                })
                return types
            })
            const allRenderColumnMap = computed(() => {
                return selectedTableColumn.value.reduce((result, item) => {
                    result[item.id] = true
                    return result
                }, {})
            })

            onMounted(() => {
                tableColumn.value = [
                    {
                        id: 'name',
                        label: i18n.t('template.name'),
                        disabled: true
                    },
                    {
                        id: 'desc',
                        label: i18n.t('template.desc')
                    },
                    {
                        id: 'type',
                        label: i18n.t('template.type')
                    },
                    {
                        id: 'lastedVersion',
                        label: i18n.t('template.lastedVersion')
                    },
                    {
                        id: 'source',
                        label: i18n.t('template.source')
                    },
                    {
                        id: 'debugPipelineCount',
                        label: i18n.t('template.debugPipelineCount')
                    },
                    {
                        id: 'instancePipelineCount',
                        label: i18n.t('template.instancePipelineCount')
                    },
                    {
                        id: 'updater',
                        label: i18n.t('template.lastModifiedBy')
                    },
                    {
                        id: 'updateTime',
                        label: i18n.t('template.lastModifiedDate')
                    },
                    {
                        id: 'operate',
                        label: i18n.t('operate'),
                        disabled: true
                    }
                ]
                const columnsCache = JSON.parse(localStorage.getItem(TEMPLATE_TABLE_COLUMN_CACHE))
                if (columnsCache) {
                    selectedTableColumn.value = columnsCache.columns
                    tableSize.value = columnsCache.size
                    console.log(columnsCache.size, '///', tableSize.value)
                } else {
                    selectedTableColumn.value = [
                        { id: 'name' },
                        { id: 'desc' },
                        { id: 'type' },
                        { id: 'lastedVersion' },
                        { id: 'source' },
                        { id: 'debugPipelineCount' },
                        { id: 'instancePipelineCount' },
                        { id: 'updater' },
                        { id: 'updateTime' },
                        { id: 'operate' }
                    ]
                }
                tableWidthMap.value = JSON.parse(localStorage.getItem(CACHE_TEMPLATE_TABLE_WIDTH_MAP)) || {
                    name: 220,
                    desc: 200,
                    type: 100,
                    lastedVersion: 80,
                    source: 80,
                    debugPipelineCount: 80,
                    instancePipelineCount: '',
                    updater: '',
                    updateTime: 100,
                    operate: 96
                }
            })

            function handlePageLimitChange (limit) {
                emit('limit-change', limit)
            }
            function handlePageChange (page) {
                emit('page-change', page)
            }
            function clearFilter () {
                emit('clear')
            }
            function formatTime (row, cell, value) {
                return convertTime(value)
            }
            function goEdit (row) {
                proxy.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        projectId: row.projectId,
                        pipelineId: row.pipelineId
                    }
                })
            }
            function handleSettingChange ({ fields, size }) {
                selectedTableColumn.value = fields
                tableSize.value = size
                localStorage.setItem(TEMPLATE_TABLE_COLUMN_CACHE, JSON.stringify({
                    columns: fields,
                    size
                }))
            }
            function handelHeaderDragend (newWidth, oldWidth, column) {
                tableWidthMap.value[column.property] = newWidth
                localStorage.setItem(CACHE_TEMPLATE_TABLE_WIDTH_MAP, JSON.stringify(tableWidthMap.value))
            }
            function sourceFilterMethod (value, row, column) {
                const property = column.property
                return row[property] === value
            }

            return {
                data,
                isLoading,
                pagination,
                tableSize,
                tableColumn,
                selectedTableColumn,
                tableWidthMap,
                sourceFilters,
                TEMPLATE_TYPE,
                allRenderColumnMap,
                handlePageLimitChange,
                handlePageChange,
                clearFilter,
                formatTime,
                goEdit,
                handleSettingChange,
                handelHeaderDragend,
                sourceFilterMethod
            }
        }
    }

</script>

<style lang="scss">
@import '@/scss/mixins/ellipsis';

.template-name {
    display: flex;
    align-items: center;
    img {
        vertical-align: middle;
        margin-right: 12px;
    }
    .pac-code-icon {
        margin: 0 0 0 12px;
    }
    span {
        @include ellipsis();
    }
}
.source-name {
    display: flex;
    align-items: center;
    span {
        flex-shrink: 0;
    }
}
.template-operate {
    display: flex;
    align-items: center;
    height: 40px;
    .action {
        display: inline-block;
        text-align: center;
        min-width: 62px;
    }
}

.select-text {
    color: #3A84FF;
    cursor: pointer;
}

.create-permission {
    cursor: pointer;
}

.not-create-permission {
    cursor: not-allowed;
}

</style>
