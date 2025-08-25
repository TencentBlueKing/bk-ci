<template>
    <section class="reference-history-main">
        <bk-table
            :data="renderData"
            :pagination="pagination"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
        >
            <bk-table-column
                :label="$t('publicVar.releaseTime')"
                prop="pubTime"
                width="200"
                show-overflow-tooltip
            />
            <bk-table-column
                :label="$t('publicVar.publisher')"
                prop="publisher"
                width="160"
                show-overflow-tooltip
            />
            <bk-table-column
                :label="$t('publicVar.publishContent')"
                prop="publishContent"
                show-overflow-tooltip
            >
                <template slot-scope="{ row }">
                    <template v-if="row.isCreated">
                        {{
                            row.publishContent.type === VARIABLE
                                ? $t('publicVar.newVariableTips', [row.publishContent.varName])
                                : $t('publicVar.newConstTips', [row.publishContent.varName])
                        }}
                    </template>
                    <template v-else-if="row.isDeleted">
                        {{
                            row.publishContent.type === VARIABLE
                                ? $t('publicVar.deleteVariableTips', [row.publishContent.varName])
                                : $t('publicVar.deleteConstTips', [row.publishContent.varName])
                        }}
                    </template>
                    <template v-else-if="row.isUpdated">
                        <bk-popover
                            placement="top"
                            max-width="300"
                            ext-cls="publish-content-popover"
                        >
                            <div>
                                <span>
                                    {{
                                        row.publishContent.type === VARIABLE
                                            ? $t('publicVar.updateVariableTips', [row.publishContent.varName])
                                            : $t('publicVar.updateConstTips', [row.publishContent.varName])
                                    }}
                                </span>
                                <span>
                                    {{ $t('publicVar.updateDesc', [row.publishContent.desc]) }}
                                </span>
                                <span>
                                    {{ $t('publicVar.updateDefault', [row.publishContent.defaultValue]) }}
                                </span>
                            </div>
                            <div slot="content">
                                <p>
                                    {{
                                        row.publishContent.type === VARIABLE
                                            ? $t('publicVar.updateVariableTips', [row.publishContent.varName])
                                            : $t('publicVar.updateConstTips', [row.publishContent.varName])
                                    }}
                                </p>
                                <p>
                                    {{ $t('publicVar.updateDesc', [row.publishContent.desc]) }}
                                </p>
                                <p>
                                    {{ $t('publicVar.updateDefault', [row.publishContent.defaultValue]) }}
                                </p>
                            </div>
                        </bk-popover>
                    </template>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('publicVar.versionDesc')"
                prop="desc"
                show-overflow-tooltip
            />
        </bk-table>
    </section>
</template>

<script setup>
    import { ref, computed, onMounted } from 'vue'
    import { convertTime } from '@/utils/util'
    import {
        OPERATE_TYPE,
        CONSTANT,
        VARIABLE
    } from '@/store/modules/publicVar/constants'
    import UseInstance from '@/hook/useInstance'
    const { proxy } = UseInstance()
    const props = defineProps({
        groupData: Object
    })
    const releaseHistoryList = ref([])
    const pagination = ref({
        current: 1,
        limit: 20,
        count: 0
    })
    const renderData = computed(() => {
        return releaseHistoryList.value.map(i => {
            const publishContent = JSON.parse(i.content)
            return {
                ...i,
                pubTime: convertTime(i.pubTime),
                publishContent,
                isCreated: publishContent.operate === OPERATE_TYPE.CREATE,
                isUpdated: publishContent.operate === OPERATE_TYPE.UPDATE,
                isDeleted: publishContent.operate === OPERATE_TYPE.DELETE,
            }
        })
    })
    async function fetchReleaseHistory () {
        try {
            const res = await proxy.$store.dispatch('publicVar/getReferenceHistory', {
                groupName: props.groupData?.groupName,
                page: pagination.value.current,
                pageSize: pagination.value.limit
            })
            releaseHistoryList.value = res.records
            pagination.value.count = res.count
        } catch (e) {
            proxy.$bkMessage({
                theme: 'error',
                message: e.message || e
            })
        }
    }
    function handlePageChange (page) {
        pagination.value.current = page
        fetchReleaseHistory()
    }
    function handlePageLimitChange (limit) {
        pagination.value.current = 1
        pagination.value.limit = limit
        fetchReleaseHistory()
    }
    onMounted(() => {
        fetchReleaseHistory()
    })
</script>

<style lang="scss">
.reference-history-main {
    padding: 20px;
}
.publish-content-popover {
    .bk-tooltip-content {
        word-break: break-all;
    }
}
</style>