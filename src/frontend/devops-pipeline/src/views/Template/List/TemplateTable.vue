<template>
    <bk-table
        v-bkloading="{ isLoading }"
        :data="templateList"
        :size="tableSize"
        :max-height="maxHeight"
        ext-cls="template-table"
        :pagination="pagination"
        @page-change="handlePageChange"
        @page-limit-change="handlePageLimitChange"
        @header-dragend="handelHeaderDragend"
    >
        <TemplateEmpty
            slot="empty"
            type="search-empty"
            @clear="clearFilter"
        />

        <bk-table-column
            v-for="col in selectedTableColumn"
            :label="col.label"
            :key="col.id"
            :prop="col.id"
            :width="col.width"
            :min-width="col.minWidth"
            :show-overflow-tooltip="col.showOverflowTooltip"
            :sortable="col.sortable"
            :formatter="col.formatter"
            :fixed="col.id === 'operate' ? 'right' : false"
        >
            <template slot-scope="{ row }">
                <div
                    v-if="col.id === 'name'"
                    :class="['template-name', {
                        'select-text': row.canView
                    }]"
                    v-perm="row.viewPerm"
                    @click="goTemplateOverview(row.overviewParams)"
                >
                    <span
                        class="template-name-area"
                    >
                        {{ row.name }}
                    </span>
                    <bk-tag
                        v-if="row.latestVersionStatus === VERSION_STATUS_ENUM.COMMITTING"
                        theme="success"
                        class="draft-tag"
                    >
                        {{ $t('draft') }}
                    </bk-tag>
                    <PacTag
                        v-if="row.enablePac"
                        class="pac-code-icon"
                    />
                </div>
                <div
                    class="source-name"
                    v-else-if="col.id === 'source'"
                >
                    <span>{{ row.sourceName }}</span>
                    <bk-popover
                        placement="top"
                        v-if="row.upgradeFlag || row.publishFlag || row.storeFlag"
                    >
                        <bk-badge
                            class="store-source-flag"
                            dot
                            theme="danger"
                            :visible="row.upgradeFlag || row.publishFlag"
                        >
                            <Logo
                                size="12"
                                :name="row.storeFlag ? 'is-store' : 'template-upgrade'"
                            />
                        </bk-badge>
                        <div slot="content">
                            <span>{{ row.sourceTooltip?.content }}</span>
                            <span

                                class="text-link"
                                @click="row.sourceTooltip?.handler()"
                            >
                                {{ row.sourceTooltip?.actionLabel }}
                            </span>
                        </div>
                    </bk-popover>
                </div>

                <bk-button
                    v-else-if="col.id === 'instancePipelineCount'"
                    text
                    :disabled="row.instancePipelineCount <= 0 || !row.canView"
                    v-perm="row.viewPerm"
                    @click="goTemplateOverview(row.overviewParams)"
                >
                    {{ row.instancePipelineCount }}
                </bk-button>

                <div
                    v-else-if="col.id === 'operate'"
                    class="template-operate"
                >
                    <bk-button
                        v-if="row.type === 'PIPELINE'"
                        text
                        :disabled="row.latestVersionStatus === 'COMMITTING'"
                        @click="goInstanceEntry(row)"
                        v-perm="row.viewPerm"
                    >
                        {{ $t('template.instantiate') }}
                    </bk-button>
                    <ext-menu
                        type="template"
                        :data="row"
                        :config="row.templateActions"
                    />
                </div>
                <template
                    v-else
                >
                    <span>{{ row[col.id] || '--' }}</span>
                </template>
            </template>
        </bk-table-column>

        <bk-table-column
            type="setting"
        >
            <bk-table-setting-content
                :fields="tableColumns"
                :selected="selectedTableColumn"
                :size="tableSize"
                @setting-change="handleSettingChange"
            />
        </bk-table-column>
    </bk-table>
</template>

<script setup>
    import TemplateEmpty from '@/components/common/exception'
    import Logo from '@/components/Logo'
    import PacTag from '@/components/PacTag.vue'
    import UseInstance from '@/hook/useInstance'
    import useTemplateActions from '@/hook/useTemplateActions'
    import {
        TEMPLATE_TABLE_COLUMN_CACHE
    } from '@/store/modules/templates/constants'
    import {
        RESOURCE_TYPE,
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'
    import { VERSION_STATUS_ENUM } from '@/utils/pipelineConst'
    import { computed, defineProps, onBeforeMount, ref } from 'vue'
    import ExtMenu from './extMenu'
    const { proxy, t } = UseInstance()
    const {
        goTemplateOverview
    } = useTemplateActions()
    const props = defineProps({
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
        },
        maxHeight: {
            type: [Number, String],
            default: 'auto'
        },
    })
    const emit = defineEmits(['limit-change', 'page-change', 'clear'])
    const tableSize = ref('small')
    const tableColumns = [
        {
            id: 'name',
            label: t('template.name'),
            minWidth: 220,
            disabled: true,
            sortable: true,
            showOverflowTooltip: true
        },
        {
            id: 'desc',
            label: t('template.desc'),
            width: 300,
            showOverflowTooltip: true
        },
        {
            id: 'typeName',
            label: t('template.type'),
            width: 100
        },
        {
            id: 'releasedVersionName',
            label: t('template.latestVersion'),
            width: 200
        },
        {
            id: 'source',
            label: t('template.source'),
            width: 120
        },
        {
            id: 'debugPipelineCount',
            label: t('template.debugPipelineCount'),
            width: 100
        },
        {
            id: 'instancePipelineCount',
            label: t('template.instancePipelineCount'),
            width: 60
        },
        {
            id: 'updater',
            label: t('template.lastModifiedBy'),
            width: 120
        },
        {
            id: 'updateTime',
            label: t('template.lastModifiedDate'),
            width: 200
        },
        {
            id: 'operate',
            disabled: true,
            label: t('operate'),
            width: 96
        }
    ]
    const tableColumnMap = tableColumns.reduce((acc, cur) => {
        acc[cur.id] = cur
        return acc
    }, {})
    const selectedTableColumn = ref([])
    const projectId = computed(() => proxy.$route.params.projectId)
    const templateList = computed(() => props.data.map(temp => {
        return {
            ...temp,
            viewPerm: {
                hasPermission: temp.canView,
                disablePermissionApi: true,
                permissionData: {
                    projectId: projectId.value,
                    resourceType: RESOURCE_TYPE.TEMPLATE,
                    resourceCode: temp.id,
                    action: TEMPLATE_RESOURCE_ACTION.VIEW
                }
            }
        }
    }))

    onBeforeMount(() => {
        try {
            const columnsCache = JSON.parse(localStorage.getItem(TEMPLATE_TABLE_COLUMN_CACHE))
            if (!columnsCache) {
                throw new Error('not cache')
            }
            selectedTableColumn.value = columnsCache.columns.map(col => Object.assign(tableColumnMap[col.id], {
                width: col.width ?? tableColumnMap[col.id].width
            }))
            tableSize.value = columnsCache.size
        } catch (error) {
            selectedTableColumn.value = tableColumns
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

    function goInstanceEntry (row) {
        proxy.$router.push({
            name: 'instanceEntry',
            params: {
                templateId: row.id,
                version: row.releasedVersion,
                type: 'create'
            }
        })
    }
    function handleSettingChange ({ fields, size }) {
        selectedTableColumn.value = fields
        tableSize.value = size
        cacheTableConf({ fields, size })
    }
    function handelHeaderDragend (newWidth, _, column) {
        if (tableColumnMap[column.property]) {
            tableColumnMap[column.property].width = newWidth

            cacheTableConf({
                fields: selectedTableColumn.value,
                size: tableSize.value
            })
        }
    }

    function cacheTableConf ({ fields, size }) {
        localStorage.setItem(TEMPLATE_TABLE_COLUMN_CACHE, JSON.stringify({
            columns: fields.map(field => ({
                id: field.id,
                width: field.width
            })),
            size
        }))
    }

    // function sourceFilterMethod (value, row, column) {
    //     const property = column.property
    //     return row[property] === value
    // }
</script>

<style lang="scss">
@import '@/scss/mixins/ellipsis';
@import '@/scss/conf';

.template-name {
    display: flex;
    align-items: center;
    grid-gap: 6px;
    .template-name-area {
        flex: 1;
        @include ellipsis(1);
    }
    .pac-code-icon {
        flex-shrink: 0;
    }

}
.source-name {
    display: flex;
    align-items: center;
    grid-gap: 6px;
    height: 36px;
    line-height: 1;
    .store-source-flag {
        font-size: 0;
    }
}
.template-operate {
    display: flex;
    align-items: center;
    height: 40px;
    grid-gap: 12px;
}

.template-table.bk-table-enable-row-transition .bk-table-body td {
    transition: none;
}

.select-text {
    color: $primaryColor;
    cursor: pointer;
}

</style>
