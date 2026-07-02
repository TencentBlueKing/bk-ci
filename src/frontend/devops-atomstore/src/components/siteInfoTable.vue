<template>
    <bk-table
        :data="siteInfos || []"
        class="site-info-table"
        :border="['row', 'col']"
        show-overflow-tooltip
    >
        <bk-table-column
            :label="$t('store.IP/域名')"
            prop="host"
        >
            <template #default="{ row, $index }">
                <bk-input
                    v-if="editable && editingIndex === $index"
                    v-model="row.host"
                />
                <span v-else>{{ row.host }}</span>
            </template>
        </bk-table-column>
        <bk-table-column
            :label="$t('store.端口列表')"
            prop="port"
        >
            <template #default="{ row, $index }">
                <bk-input
                    v-if="editable && editingIndex === $index"
                    v-model="row.port"
                />
                <span v-else>{{ row.port }}</span>
            </template>
        </bk-table-column>
        <bk-table-column
            :label="$t('store.目标网络行为')"
            prop="targetNetBehaviors"
        >
            <template #default="{ row, $index }">
                <bk-select
                    v-if="editable && editingIndex === $index"
                    v-model="row.targetNetBehaviors"
                    multiple
                >
                    <bk-option
                        v-for="item in targetNetBehaviorOptions"
                        :key="item"
                        :id="item"
                        :name="item"
                    />
                </bk-select>
                <div
                    v-else
                    :title="row.targetNetBehaviors"
                >
                    <bk-tag
                        v-for="behavior in row.targetNetBehaviors"
                        :key="behavior"
                        class="mr5"
                    >
                        {{ behavior }}
                    </bk-tag>
                </div>
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="editable"
            :label="$t('store.操作')"
            width="120"
        >
            <template #default="{ $index }">
                <div class="action-buttons">
                    <span
                        v-if="editingIndex === $index"
                        class="action-btn confirm-btn"
                        @click="handleConfirmEdit"
                    >
                        <i class="bk-icon icon-check-circle-shape" />
                    </span>
                    <template v-else>
                        <span
                            class="action-btn add-btn"
                            @click="handleInsertSite($index + 1)"
                        >
                            <i class="text-14 bk-icon icon-plus" />
                        </span>
                        <span
                            class="action-btn edit-btn"
                            @click="handleEditSite($index)"
                        >
                            <i class="text-14 bk-icon icon-edit2" />
                        </span>
                    </template>
                    <span
                        class="action-btn delete-btn"
                        @click="handleDeleteSite($index)"
                    >
                        <i class="bk-icon icon-minus" />
                    </span>
                </div>
            </template>
        </bk-table-column>
        <template #empty>
            <div
                v-if="editable"
                class="empty-wrapper"
            >
                <bk-button
                    theme="primary"
                    @click="handleAddSite"
                >
                    <i class="bk-icon icon-plus" />
                    {{ $t('store.添加') }}
                </bk-button>
            </div>
            <div
                v-else
                class="empty-wrapper"
            >
                {{ $t('store.暂无数据') }}
            </div>
        </template>
    </bk-table>
</template>

<script setup>
    import { ref, watch } from 'vue'
    import UseInstance from '@/hook/useInstance.js'

    const props = defineProps({
        siteInfos: {
            type: Array,
            default: () => []
        },
        editable: {
            type: Boolean,
            default: true
        }
    })

    const emit = defineEmits(['update:siteInfos'])

    const { proxy } = UseInstance()
    const { $t } = proxy

    const editingIndex = ref(-1)

    // 目标网络行为选项
    const targetNetBehaviorOptions = ['UPLOAD', 'DOWNLOAD', 'BOTH']

    // 监听 siteInfos 变化，重置编辑状态
    watch(() => props.siteInfos, () => {
        if (editingIndex.value >= (props.siteInfos?.length || 0)) {
            editingIndex.value = -1
        }
    })

    function handleAddSite () {
        const newSites = [...(props.siteInfos || [])]
        newSites.push({
            host: 'localhost',
            port: '8080',
            targetNetBehaviors: ['UPLOAD']
        })
        editingIndex.value = newSites.length - 1
        emit('update:siteInfos', newSites)
    }

    function handleInsertSite (index) {
        const newSites = [...(props.siteInfos || [])]
        newSites.splice(index, 0, {
            host: 'localhost',
            port: '8080',
            targetNetBehaviors: ['UPLOAD']
        })
        editingIndex.value = index
        emit('update:siteInfos', newSites)
    }

    function handleEditSite (index) {
        editingIndex.value = index
    }

    function handleConfirmEdit () {
        editingIndex.value = -1
    }

    function handleDeleteSite (index) {
        const newSites = [...(props.siteInfos || [])]
        newSites.splice(index, 1)
        if (editingIndex.value === index) {
            editingIndex.value = -1
        } else if (editingIndex.value > index) {
            editingIndex.value--
        }
        emit('update:siteInfos', newSites)
    }
</script>

<style lang="scss" scoped>
.site-info-table {
    .mr5 {
        margin-right: 5px;
    }
    
    .action-buttons {
        display: inline-flex;
        align-items: center;
        gap: 8px;
    }
    
    .action-btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 14px;
        height: 14px;
        border-radius: 50%;
        cursor: pointer;
        font-size: 8px;
        font-weight: bold;
        color: #fff;
        background: #c4c6cc;
        
        &:hover {
            opacity: 0.8;
        }
        
        &.confirm-btn {
            font-size: 16px;
            width: auto;
            height: auto;
            background: transparent;
            color: #c4c6cc;
            
            &:hover {
                color: #3a84ff;
            }
        }
    }
    
    .empty-wrapper {
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px 0;
    }

    .text-14 {
        font-size: 12px;
        vertical-align: middle;
    }

    .bk-icon {
        vertical-align: middle;
    }
}
</style>
