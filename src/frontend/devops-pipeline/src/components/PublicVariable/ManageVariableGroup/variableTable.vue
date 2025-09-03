<template>
    <bk-table
        class="variable-list-table"
        :data="data.data"
        max-height="350"
    >
        <bk-table-column
            :label="data.title"
            prop="varName"
        />
        <bk-table-column
            :label="$t('desc')"
            prop="desc"
        >
            <template slot-scope="{ row }">
                {{ row.desc || '--' }}
            </template>
        </bk-table-column>
        <bk-table-column
            :label="$t('type')"
            prop="type"
        >
            <template slot-scope="{ row }">
                {{ row.type === VARIABLE ? $t('publicVar.params') : $t('publicVar.constant') }}
            </template>
        </bk-table-column>
        <bk-table-column
            :label="$t('publicVar.defaultValue')"
            prop="defaultValue"
        >
            <template slot-scope="{ row }">
                {{ row.defaultValue || '--' }}
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="showRef"
            :label="$t('publicVar.referenceStatus')"
            prop="referCount"
        >
            1
        </bk-table-column>
    </bk-table>
</template>

<script setup>
    import {
        VARIABLE
    } from '@/store/modules/publicVar/constants'
    const props = defineProps({
        data: Object,
        showRef: {
            type: Boolean,
            default: false
        }
    })
</script>
