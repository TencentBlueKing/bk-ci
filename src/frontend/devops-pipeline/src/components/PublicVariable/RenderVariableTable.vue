<template>
    <section class="render-variable-table-main">
        <details open>
            <summary class="category-collapse-trigger">
                <bk-icon
                    type="right-shape"
                    class="icon-angle-right"
                />
                {{ data.title }}
            </summary>
            <div
                class="collapse-content"
            >
                <bk-table
                    class="variable-list-table"
                    :data="data.data"
                    max-height="350"
                    :row-class-name="rowClassName"
                >
                    <bk-table-column
                        :label="$t('publicVar.varId')"
                        prop="varName"
                    />
                    <bk-table-column
                        :label="$t('publicVar.varAlias')"
                        prop="alias"
                    >
                        <template slot-scope="{ row }">
                            {{ row.alias || '--' }}
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
                    >
                        <template slot-scope="{ row }">
                            {{ $t(`storeMap.${DEFAULT_PARAM[row.buildFormProperty.type]?.typeDesc}`) ?? row.type }}
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
                        :label="$t('publicVar.referenceStatus')"
                        prop="referCount"
                    >
                        <template slot-scope="{ row }">
                            <bk-button text>
                                {{ row.referCount || 0 }}
                            </bk-button>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        v-if="!readOnly"
                        :label="$t('publicVar.operation')"
                        prop="operation"
                    >
                        <template slot-scope="{ row }">
                            <bk-button
                                text
                                class="mr5"
                                @click="data.handleEditParam(data.key, row.varName)"
                            >
                                {{ $t('edit') }}
                            </bk-button>
                            <bk-button
                                text
                                class="mr5"
                                @click="data.handleCopyParam(data.key, row.buildFormProperty)"
                            >
                                {{ $t('copy') }}
                            </bk-button>
                            <bk-popconfirm
                                ref="removePopConfirmRef"
                                :popover-options="{ appendTo: 'parent' }"
                                :title="$t('newui.pipelineParam.removeTitle')"
                                :confirm-text="$t('newui.pipelineParam.remove')"
                                :cancel-text="$t('cancel')"
                                trigger="click"
                                width="200"
                                ext-cls="delete-param-popconfrim"
                                ext-popover-cls="delete-param-popconfrim-content"
                                @confirm="data.handleDeleteParam(row.varName)"
                            >
                                <bk-button
                                    text
                                    :disabled="!!row.referCount"
                                >
                                    {{ $t('delete') }}
                                </bk-button>
                            </bk-popconfirm>
                        </template>
                    </bk-table-column>
                    <empty-exception
                        v-if="!readOnly"
                        slot="empty"
                        :type="exceptionType"
                    >
                        <template
                            slot="sub-content"
                        >
                            <bk-button
                                text
                                class="empty-tips"
                                @click="data?.emptyBtnFn()"
                            >
                                {{ data.emptyBtnText }}
                            </bk-button>
                        </template>
                    </empty-exception>
                </bk-table>
            </div>
        </details>
    </section>
</template>

<script setup>
    import EmptyException from '@/components/common/exception'
    import {
        VARIABLE
    } from '@/store/modules/publicVar/constants'
    import {
        DEFAULT_PARAM
    } from '@/store/modules/atom/paramsConfig'
    const props = defineProps({
        isShow: Boolean,
        data: Object,
        readOnly: Boolean,
        newParamId: String
    })
    function rowClassName ({ row }) {
        if (row.varName === props.newParamId) return 'is-new'
        return ''
    }
</script>

<style lang="scss">
.render-variable-table-main {
    .category-collapse-trigger {
        display: flex;
        align-items: center;
        cursor: pointer;
        height: 32px;
        background: #EAEBF0;
        border-radius: 2px 2px 0 0;
        padding: 0 16px;
        font-size: 12px;
        color: #4D4F56;
        font-weight: 700;
        .icon-angle-right {
            font-size: 10px;
            margin-right: 5px;
            transform: rotate(90deg);
            color: #979BA5;
        }
    }
    details:not([open]) .collapse-content {
        display: none;
    }

    details:not([open]) .category-collapse-trigger .icon-angle-right {
        transform: rotate(0deg);
    }
    .variable-list-table {
        .is-new {
            background-color: #f2fff4 !important;
        }
        .bk-table-empty-text {
            padding: 15px 0 !important;
        }
    }
}
</style>