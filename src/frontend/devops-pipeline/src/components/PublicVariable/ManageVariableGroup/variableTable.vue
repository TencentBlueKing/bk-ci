<template>
    <bk-table
        class="variable-list-table"
        :data="data.data"
        max-height="350"
    >
        <bk-table-column
            :label="data.title"
            prop="varName"
        >
            <template slot-scope="{ row }">
                <span
                    :class="['var-name', {
                        'is-required': row.buildFormProperty.valueNotEmpty
                    }]"
                >
                    {{ row?.varName }}
                    <span
                        v-if="row.buildFormProperty.readOnly"
                        class="read-only"
                    >
                        {{ $t('readonlyParams') }}
                    </span>
                </span>
            </template>
        </bk-table-column>
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
            width="100"
        >
            <template slot-scope="{ row }">
                {{ $t(`storeMap.${DEFAULT_PARAM[row.valueType]?.typeDesc}`) ?? row.type }}
            </template>
        </bk-table-column>
        <bk-table-column
            :label="$t('publicVar.defaultValue')"
            prop="defaultValue"
            width="120"
        >
            <template slot-scope="{ row }">
                {{ row.defaultValue || '--' }}
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="showRef"
            :label="$t('publicVar.beenCited')"
            width="100"
        >
            <template slot-scope="{ row }">
                <span
                    :style="{
                        color: row.isCited ? '#63656E' : '#C4C6CC'
                    }"
                >
                    {{ row.isCited ? $t('true') : $t('false') }}
                </span>
            </template>
        </bk-table-column>
    </bk-table>
</template>

<script setup>
    import {
        DEFAULT_PARAM
    } from '@/store/modules/atom/paramsConfig'
    const props = defineProps({
        data: Object,
        showRef: {
            type: Boolean,
            default: false
        }
    })
</script>

<style lang="scss" scoped>
    .variable-list-table {
        .var-name.is-required {
            padding-left: 8px;
            &::before {
                content: "* ";
                color: red;
                position: absolute;
                left: 14px;
                top: 14px;
            }
        }
        .read-only {
            display: inline-block;
            margin-right: 4px;
            flex-shrink: 0;
            font-size: 12px;
            color: #63656E;
            background: #F0F1F5;
            border-radius: 2px;
            margin: 0 4px 0 -2px;
            padding: 0 4px;
            transform: scale(0.83);
        }
    }
</style>
