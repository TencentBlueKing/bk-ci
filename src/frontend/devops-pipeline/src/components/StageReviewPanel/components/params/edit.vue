<template>
    <section class="param-edit-home">
        <bk-button text title="primary" @click="toggleShowParamForm()" :disabled="disabled" class="params-opt">
            <i class="bk-icon icon-plus-circle"></i>{{ $t('stageReview.createVariables') }}
        </bk-button>

        <bk-table :data="copyReviewParams">
            <bk-table-column :label="$t('stageReview.variableName')" prop="key" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('stageReview.alias')" prop="chineseName" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('stageReview.type')" prop="valueType" :formatter="typeFormatter"></bk-table-column>
            <bk-table-column :label="$t('stageReview.defaultValue')" prop="value" show-overflow-tooltip :formatter="valFormatter"></bk-table-column>
            <bk-table-column :label="$t('stageReview.required')" prop="required" :formatter="requireFormatter"></bk-table-column>
            <bk-table-column :label="$t('stageReview.options')" prop="options" show-overflow-tooltip :formatter="jsonFormatter"></bk-table-column>
            <bk-table-column :label="$t('stageReview.operation')" width="120">
                <template slot-scope="props">
                    <bk-button class="mr10" theme="primary" text @click="toggleShowParamForm(props.row, props.$index)" :disabled="disabled">{{ $t('edit') }}</bk-button>
                    <bk-button class="mr10" theme="primary" text @click="removeParam(props.$index)" :disabled="disabled">{{ $t('delete') }}</bk-button>
                </template>
            </bk-table-column>
        </bk-table>

        <param-form
            :show="paramFormData.isShow"
            :param="paramFormData.form"
            @confirm="confirm"
            @cancel="cancel"
        ></param-form>
    </section>
</template>

<script>
    import ParamForm from './form'
    import { CHECK_PARAM_LIST } from '@/store/modules/atom/paramsConfig'

    const paramsMap = CHECK_PARAM_LIST.reduce((acc, cur) => {
        acc[cur.id] = global.pipelineVue.$t(`storeMap.${cur.name}`)
        return acc
    }, {})

    export default {
        components: {
            ParamForm
        },

        props: {
            disabled: Boolean,
            reviewParams: Array
        },

        data () {
            return {
                copyReviewParams: JSON.parse(JSON.stringify(this.reviewParams)),
                paramFormData: {
                    isShow: false,
                    form: {}
                }
            }
        },

        methods: {
            confirm (row) {
                if (this.paramFormData.index >= 0) {
                    this.copyReviewParams.splice(this.paramFormData.index, 1, row)
                } else {
                    this.copyReviewParams.push(row)
                }

                this.toggleShowParamForm()
                this.triggleChange()
            },

            cancel () {
                this.toggleShowParamForm()
            },

            toggleShowParamForm (row = {}, index = -1) {
                this.paramFormData.isShow = !this.paramFormData.isShow
                this.paramFormData.form = row
                this.paramFormData.index = index
            },

            removeParam (index) {
                const confirmFn = () => {
                    this.copyReviewParams.splice(index, 1)
                    this.triggleChange()
                }
                this.$bkInfo({
                    theme: 'error',
                    title: this.$t('stageReview.confirmDelete'),
                    confirmFn
                })
            },

            triggleChange () {
                this.$emit('change', 'reviewParams', this.copyReviewParams)
            },

            typeFormatter (row, column, cellValue, index) {
                return paramsMap[cellValue]
            },

            requireFormatter (row, column, cellValue, index) {
                const valMap = {
                    true: this.$t('true'),
                    false: this.$t('false')
                }
                return valMap[cellValue]
            },

            jsonFormatter (row, column, cellValue, index) {
                const valJson = cellValue && Array.isArray(cellValue) && cellValue.length ? JSON.stringify(cellValue) : '--'
                return valJson.replace(/"([^"]+)":/g, '$1:')
            },

            valFormatter (row, column, cellValue, index) {
                let res = cellValue || '--'
                if (Array.isArray(cellValue)) {
                    res = cellValue.length ? `[${cellValue.join(', ')}]` : '--'
                } else {
                    res = String(cellValue) || '--'
                }
                return res
            }
        }
    }
</script>

<style lang="scss" scoped>
    .param-edit-home {
        position: relative;
    }

    .params-opt {
        position: absolute;
        left: 85px;
        top: -31px;
        font-size: 12px;
        ::v-deep .bk-icon {
            top: 0;
            margin-right: 1px;
        }
    }

    ::v-deep .bk-table .cell {
        overflow: hidden;
    }
</style>
