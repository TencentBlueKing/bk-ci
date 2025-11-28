<template>
    <section class="reference-history-main">
        <bk-table
            v-bkloading="{ isLoading }"
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
                    <span>{{ row.content }}</span>
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
    import { ref, computed, watch } from 'vue'
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
    const isLoading = ref(false)
    const pagination = ref({
        current: 1,
        limit: 20,
        count: 0
    })
    const renderData = computed(() => {
        return releaseHistoryList.value.map(i => {
            // const publishContent = JSON.parse(i.content)
            return {
                ...i,
                pubTime: convertTime(i.pubTime),
                // publishContent,
                // isCreated: publishContent.operate === OPERATE_TYPE.CREATE,
                // isUpdated: publishContent.operate === OPERATE_TYPE.UPDATE,
                // isDeleted: publishContent.operate === OPERATE_TYPE.DELETE,
            }
        })
    })
    watch(() => props.groupData.groupName, (newValue, oldValue) => {
        if (newValue !== oldValue) {
            fetchReleaseHistory()
        }
    }, {
        deep: true,
        immediate: true
    })
    async function fetchReleaseHistory () {
        try {
            isLoading.value = true
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
        } finally {
            isLoading.value = false
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