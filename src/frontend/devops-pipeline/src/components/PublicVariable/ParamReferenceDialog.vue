<template>
    <bk-dialog
        v-model="isShow"
        :width="900"
        :mask-close="true"
        :auto-close="false"
        header-position="left"
        ext-cls="param-reference-dialog"
        @cancel="handleClose"
    >
        <template slot="header">
            <div class="reference-title">
                <span>{{ $t('publicVar.paramReferenceStatus') }}</span>
                <span class="param-name">{{ varName }}</span>
            </div>
        </template>
        <div class="param-reference-content">
            <bk-table
                v-bkloading="{ isLoading: loading }"
                class="reference-table"
                :data="tableData"
                :pagination="pagination"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange"
            >
                <bk-table-column
                    :label="$t('publicVar.pipelineTemplateName')"
                    prop="referName"
                    show-overflow-tooltip
                >
                    <template slot-scope="{ row }">
                        <bk-button
                            text
                            @click="handleToPipeline(row)"
                        >
                            {{ row.referName }}
                        </bk-button>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('type')"
                    prop="referTypeName"
                    width="120"
                />
                <bk-table-column
                    :label="$t('publicVar.lastModifiedBy')"
                    prop="modifier"
                    width="120"
                    show-overflow-tooltip
                />
                <bk-table-column
                    :label="$t('publicVar.lastModifiedDate')"
                    prop="updateTime"
                    width="180"
                />
                <template slot="empty">
                    <div class="empty-tips">
                        {{ $t('noData') }}
                    </div>
                </template>
            </bk-table>
        </div>
        
        <template slot="footer">
            <bk-button
                @click="handleClose"
            >
                {{ $t('close') }}
            </bk-button>
        </template>
    </bk-dialog>
</template>

<script setup>
    import { ref, computed, watch } from 'vue'
    import { convertTime } from '@/utils/util'
    import UseInstance from '@/hook/useInstance'
    
    const { proxy } = UseInstance()
    
    const props = defineProps({
        visible: {
            type: Boolean,
            default: false
        },
        varName: {
            type: String,
            default: ''
        },
        groupName: {
            type: String,
            default: ''
        }
    })
    
    const emit = defineEmits(['update:visible'])
    
    const isShow = computed({
        get: () => props.visible,
        set: (val) => emit('update:visible', val)
    })
    
    const loading = ref(false)
    const referenceList = ref([])
    const pagination = ref({
        current: 1,
        limit: 10,
        count: 0,
        showTotalCount: true
    })
    
    const tableData = computed(() => {
        return referenceList.value.map(i => ({
            ...i,
            updateTime: convertTime(i.updateTime),
            referTypeName: i.referType === 'PIPELINE' ? proxy.$t('pipeline') : proxy.$t('template.template')
        }))
    })
    
    // 获取变量引用列表
    async function fetchReferenceList () {
        if (!props.groupName || !props.varName) return
        
        loading.value = true
        try {
            const res = await proxy.$store.dispatch('publicVar/getReferenceList', {
                groupName: props.groupName,
                params: {
                    page: pagination.value.current,
                    pageSize: pagination.value.limit,
                    varName: props.varName // 传入变量名进行过滤
                }
            })
            
            referenceList.value = res.records || []
            pagination.value.count = res.count || 0
        } catch (e) {
            proxy.$bkMessage({
                theme: 'error',
                message: e.message || e
            })
        } finally {
            loading.value = false
        }
    }
    
    // 跳转到流水线/模板
    function handleToPipeline (row) {
        if (!row?.referUrl) return
        window.open(row.referUrl, '_blank')
    }
    
    // 分页切换
    function handlePageChange (page) {
        pagination.value.current = page
        fetchReferenceList()
    }
    
    // 每页数量切换
    function handlePageLimitChange (limit) {
        pagination.value.current = 1
        pagination.value.limit = limit
        fetchReferenceList()
    }
    
    // 关闭弹窗
    function handleClose () {
        isShow.value = false
        referenceList.value = []
        pagination.value.current = 1
    }
    
    // 监听弹窗显示状态
    watch(() => props.visible, (val) => {
        if (val) {
            pagination.value.current = 1
            fetchReferenceList()
        }
    })
</script>

<style lang="scss">
.param-reference-dialog {
    .param-name {
        color: #979BA5;
        font-size: 18px;
        font-weight: 500;
        &::before {
            content: '|';
            display: inline-block;
            height: 16px;
            color: #DCDEE5;
            margin: 0 10px;
        }
    }
    .param-reference-content {
        .reference-info {
            padding: 16px 0;
            margin-bottom: 16px;
            background: #F5F7FA;
            border-radius: 2px;
            padding-left: 16px;
            font-size: 14px;
            
            .info-label {
                color: #979BA5;
                margin-right: 8px;
            }
            
            .info-value {
                color: #313238;
                font-weight: 500;
            }
        }
        
        .reference-table {
            .empty-tips {
                padding: 40px 0;
                color: #979BA5;
                font-size: 14px;
            }
        }
    }
}
</style>
