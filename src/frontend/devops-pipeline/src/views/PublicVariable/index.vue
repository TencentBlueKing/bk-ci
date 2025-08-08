<template>
    <section class="pipeline-public-var-main">
        <bk-alert
            class="public-var-tips"
            type="info"
            closable
            :title="$t('publicVar.publicVarTips')"
        />
        <div
            class="public-var-list-box"
        >
            <div class="header-wrapper">
                <div>
                    <bk-button
                        theme="primary"
                        icon="plus"
                        class="mr10"
                        @click="handleAddGroup"
                    >
                        {{ $t('publicVar.addParamGroup') }}
                    </bk-button>
                    <bk-button
                        @click="handleShowImportDialog"
                    >
                        {{ $t('publicVar.importParamGroup') }}
                    </bk-button>
                </div>
                <search-select
                    ref="searchSelect"
                    class="search-input"
                    v-model="searchValue"
                    :data="searchList"
                    clearable
                    :show-condition="false"
                    :placeholder="filterTips"
                >
                </search-select>
            </div>
            <bk-table
                class="public-list-table"
                :data="tableData"
            >
                <bk-table-column
                    :label="$t('publicVar.paramGroupId')"
                    width="250"
                    prop="groupId"
                >
                    <template slot-scope="{ row }">
                        <bk-button
                            text
                            @click="handleViewDetail"
                        >
                            {{ row.groupId }}
                        </bk-button>
                    </template>
                </bk-table-column>
                <bk-table-column
                    width="120"
                    :label="$t('publicVar.pipelineNum')"
                >
                    <template>
                        <bk-button
                            text
                        >
                            3
                        </bk-button>
                    </template>
                </bk-table-column>
                <template v-if="!showDetail">
                    <bk-table-column
                        :label="$t('publicVar.paramNum')"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('publicVar.paramGroupDesc')"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('publicVar.lastModifiedBy')"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('publicVar.lastModifiedDate')"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('publicVar.operation')"
                    >
                        <template slot-scope="{ row }">
                            <bk-button
                                text
                                @click="handleEditGroup(row)"
                            >
                                {{ $t('edit') }}
                            </bk-button>
                        </template>
                    </bk-table-column>
                </template>
            </bk-table>
            <param-group-detail
                :is-show.sync="showDetail"
                :title="detailTitle"
                :group-data="groupData"
            />
            <importParamGroupPopup
                :is-show.sync="showImportDialog"
            />
        </div>
    </section>
</template>

<script setup>
    import { ref, computed, onMounted } from 'vue'
    import UseInstance from '@/hook/useInstance'
    import SearchSelect from '@blueking/search-select'
    import ImportParamGroupPopup from './ImportParamGroupPopup.vue'
    import ParamGroupDetail from './ParamGroupDetail.vue'
    const { proxy } = UseInstance()
    const searchValue = ref([])
    const groupData = ref({})
    const showDetail = ref(false)
    const detailTitle = ref('')
    const showImportDialog = ref(false)
    const tableData = ref([
        {
            groupId: 'vav'
        }
    ])
    const searchList = computed(() => {
        return [
            {
                name: proxy.$t('publicVar.paramGroupName'),
                id: 'groupName'
            },
            {
                name: proxy.$t('publicVar.paramName'),
                id: 'name'
            },
            {
                name: proxy.$t('publicVar.paramType'),
                id: 'type'
            },
            {
                name: proxy.$t('publicVar.paramGroupDesc'),
                id: 'desc'
            }
        ]
    })
    const filterTips = computed(() => searchList.value.map(item => item.name).join('/'))

    function handleShowImportDialog () {
        showImportDialog.value = true
    }
    function handleViewDetail (row) {
        if (showDetail.value) return
        showDetail.value = true
        groupData.value = row
        detailTitle.value = ''
    }
    function handleAddGroup () {
        if (showDetail.value) return
        showDetail.value = true
        groupData.value = {}
        detailTitle.value = proxy.$t('publicVar.addParamGroup')
    }
    function handleEditGroup (row) {
        if (showDetail.value) return
        showDetail.value = true
        groupData.value = row
        detailTitle.value = proxy.$t('publicVar.editParamGroup')
    }
    onMounted(() => {})
</script>
<style lang="scss" scoped>
    .pipeline-public-var-main {
        width: 100%;
        .public-var-tips {
            width: 100%;
        }
        .public-var-list-box {
            padding: 20px;
        }
        .header-wrapper {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 20px;
            .search-input {
                width: 600px;
                background-color: white;
                ::placeholder {
                    color: #c4c6cc;
                }
            }
        }
    }
</style>
