<template>
    <bk-table
        ext-cls="list-table"
        v-bkloading="{ isLoading }"
        ref="tableRef"
        :data="data"
        :size="tableSize"
        :pagination="pagination"
        @page-change="handlePageChange"
        @page-limit-change="handlePageLimitChange"
        @sort-change="handleSort"
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
            sortable="custom"
        >
            <template slot-scope="{ row }">
                <div
                    class="template-name select-text"
                    @click="goEdit(row)"
                >
                    <img :src="row.logoUrl">
                    <span>{{ row.name }}</span>
                    <img
                        v-if="row.enablePac"
                        src="../../images/pacIcon.png"
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
        ></bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.type"
            :width="tableWidthMap.type"
            :label="$t('template.type')"
            prop="type"
            :filters="sourceFilters"
            :filter-method="sourceFilterMethod"
            :filter-multiple="false"
        ></bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.lastedVersion"
            :width="tableWidthMap.lastedVersion"
            :label="$t('template.lastedVersion')"
            prop="lastedVersion"
        ></bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.source"
            :width="tableWidthMap.source"
            :label="$t('template.source')"
            prop="source"
        ></bk-table-column>
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
        ></bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.updateTime"
            :width="tableWidthMap.updateTime"
            key="updateTime"
            :label="$t('template.lastModifiedDate')"
            sortable="custom"
            prop="updateTime"
            :formatter="formatTime"
        />
        <bk-table-column
            :label="$t('operate')"
            :min-width="100"
            :width="tableWidthMap.operate"
        >
            <div
                slot-scope="{ row }"
                class="template-operate"
            >
                <span
                    v-if="row.type === 'PIPELINE'"
                    @click="toInstanceList(row)"
                    :key="row.templateId"
                    :class="row.canEdit ? 'select-text' : 'not-create-permission'"
                    v-perm="{
                        permissionData: {
                            projectId: projectId,
                            resourceType: row.canView ? 'pipeline' : 'pipeline_template',
                            resourceCode: row.canView ? projectId : row.id,
                            action: row.canView ? RESOURCE_ACTION.CREATE : TEMPLATE_RESOURCE_ACTION.VIEW
                        }
                    }"
                >
                    {{ $t('template.instantiate') }}
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
    import ExtMenu from './extMenu'
    import TemplateEmpty from '@/components/common/exception'
    import {
        RESOURCE_ACTION,
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'
    import {
        TEMPLATE_TABLE_COLUMN_CACHE,
        CACHE_TEMPLATE_TABLE_WIDTH_MAP
    } from '@/store/constants'
    import { convertTime } from '@/utils/util'

    export default {
        components: {
            ExtMenu,
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
        data () {
            return {
                sourceFilters: [
                    { text: '类型1', value: 'CUSTOM' },
                    { text: '类型2', value: 'REPOSITORY' },
                    { text: '类型3', value: 'MARKET' },
                    { text: '类型4', value: 'YAML' }
                ],
                tableSize: 'medium',
                tableColumn: [],
                selectedTableColumn: [],
                tableWidthMap: {}
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            TEMPLATE_RESOURCE_ACTION () {
                return TEMPLATE_RESOURCE_ACTION
            },
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            allRenderColumnMap () {
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true
                    return result
                }, {})
            }
        },

        watch: {
            
        },
        mounted () {
            this.tableColumn = [
                {
                    id: 'name',
                    label: this.$t('template.name'),
                    disabled: true
                },
                {
                    id: 'desc',
                    label: this.$t('template.desc')
                },
                {
                    id: 'type',
                    label: this.$t('template.type')
                },
                {
                    id: 'lastedVersion',
                    label: this.$t('template.lastedVersion')
                },
                {
                    id: 'source',
                    label: this.$t('template.source')
                },
                {
                    id: 'debugPipelineCount',
                    label: this.$t('template.debugPipelineCount')
                },
                {
                    id: 'instancePipelineCount',
                    label: this.$t('template.instancePipelineCount')
                },
                {
                    id: 'updater',
                    label: this.$t('template.lastModifiedBy')
                },
                {
                    id: 'updateTime',
                    label: this.$t('template.lastModifiedDate')
                },
                {
                    id: 'operate',
                    label: this.$t('operate'),
                    disabled: true
                }
            ]
            const columnsCache = JSON.parse(localStorage.getItem(TEMPLATE_TABLE_COLUMN_CACHE))
            if (columnsCache) {
                this.selectedTableColumn = columnsCache.columns
                this.tableSize = columnsCache.size
            } else {
                this.selectedTableColumn = [
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
            this.tableWidthMap = JSON.parse(localStorage.getItem(CACHE_TEMPLATE_TABLE_WIDTH_MAP)) || {
                name: 220,
                desc: 200,
                type: 100,
                lastedVersion: 80,
                source: '',
                debugPipelineCount: 80,
                instancePipelineCount: '',
                updater: '',
                updateTime: 100,
                operate: 100
            }
        },
        methods: {
            handlePageLimitChange (limit) {
                this.$emit('limit-change', limit)
            },
            handlePageChange (page) {
                this.$emit('page-change', page)
            },
            clearFilter () {
                this.$emit('clear')
            },
            formatTime (row, cell, value) {
                return convertTime(value)
            },
            goEdit (row) {
                // this.$router.push({
                //     name: 'pipelinesEdit',
                //     params: {
                //         projectId: row.projectId,
                //         pipelineId: row.pipelineId
                //     }
                // })
            },
            handleSettingChange ({ fields, size }) {
                console.log(1111, fields, size)
                this.selectedTableColumn = fields
                this.tableSize = size
                localStorage.setItem(TEMPLATE_TABLE_COLUMN_CACHE, JSON.stringify({
                    columns: fields,
                    size
                }))
            },
            handelHeaderDragend (newWidth, oldWidth, column) {
                this.tableWidthMap[column.property] = newWidth
                localStorage.setItem(CACHE_TEMPLATE_TABLE_WIDTH_MAP, JSON.stringify(this.tableWidthMap))
            },
            handleSort ({ prop, order }) {
                // const sortType = PIPELINE_SORT_FILED[prop]
                // if (sortType) {
                //     const collation = prop ? ORDER_ENUM[order] : ORDER_ENUM.descending
                //     localStorage.setItem('pipelineSortType', sortType)
                //     localStorage.setItem('pipelineSortCollation', collation)
                //     this.$router.replace({
                //         query: {
                //             ...this.$route.query,
                //             sortType,
                //             collation
                //         }
                //     })
                // }
            },
            sourceFilterMethod (value, row, column) {
                const property = column.property
                return row[property] === value
            }
        }
    }

</script>

<style lang="scss">
  @import '@/scss/conf.scss';
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
  }
  .template-operate {
    display: flex;
    align-items: center;
    height: 39px;
    span {
        margin-right: 15px;
    }
  }

  .select-text {
    color: #3A84FF;
    cursor: pointer;
  }
  .not-create-permission {
    cursor: not-allowed;
  }

</style>
