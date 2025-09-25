<template>
    <section class="reference-list-main">
        <bk-alert
            type="info"
            :title="$t('publicVar.referenceTips')"
        />
        <div class="reference-table-wrapper">
            <p class="title">
                {{ $t('publicVar.referenceTitle') }}
            </p>
            <bk-table
                :data="renderData"
                :pagination="pagination"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange"
            >
                <bk-table-column
                    :label="$t('publicVar.pipelineTemplateName')"
                    prop="referName"
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
                />
                <bk-table-column
                    :label="$t('publicVar.referenceNum')"
                    prop="actualRefCount"
                />
                <bk-table-column
                    :label="$t('publicVar.lastModifiedBy')"
                    prop="modifier"
                />
                <bk-table-column
                    :label="$t('publicVar.lastModifiedDate')"
                    prop="updateTime"
                />
            </bk-table>
        </div>
    </section>
</template>

<script setup>
    import { ref, computed, onMounted } from 'vue'
    import { convertTime } from '@/utils/util'
    import UseInstance from '@/hook/useInstance'
    const { proxy } = UseInstance()
    const props = defineProps({
        groupData: Object
    })
    const referenceList = ref([])
    const pagination = ref({
        current: 1,
        limit: 20,
        count: 0
    })
    const renderData = computed(() => {
        return referenceList.value.map(i => {
            return {
                ...i,
                updateTime: convertTime(i.updateTime)
            }
        })
    })
    async function fetchReferenceList () {
        try {
            const res = await proxy.$store.dispatch('publicVar/getReferenceList', {
                groupName: props.groupData?.groupName,
                params: {
                    page: pagination.value.current,
                    pageSize: pagination.value.limit
                }
            })
            referenceList.value = res.records.map(i => ({
                ...i,
                referTypeName: i.referType === 'PIPELINE' ? proxy.$t('pipeline') : proxy.$t('template')
            }))
            pagination.value.count = res.count
        } catch (e) {
            proxy.$bkMessage({
                theme: 'error',
                message: e.message || e
            })
        }
    }
    function handleToPipeline (row) {
        if (!row?.referUrl) return
        window.open(row.referUrl, '_blank')
    }
    function handlePageChange (page) {
        pagination.value.current = page
        fetchReferenceList()
    }
    function handlePageLimitChange (limit) {
        pagination.value.current = 1
        pagination.value.limit = limit
        fetchReferenceList()
    }
    onMounted(() => {
        fetchReferenceList()
    })
</script>

<style lang="scss">
.reference-list-main {
    .reference-table-wrapper {
        padding: 20px;
        .title {
            font-weight: 700;
            font-size: 14px;
            color: #63656E;
            margin-bottom: 20px;
        }
    }
}
</style>