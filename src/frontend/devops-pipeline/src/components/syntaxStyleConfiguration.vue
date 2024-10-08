<template>
    <div>
        <bk-popover
            theme="light"
            :width="892"
            placement="top-start"
        >
            <label class="label">{{ $t('namingConvention') }}</label>
            <div slot="content">
                <h3>{{ $t('grammaticalDifferences') }}</h3>
                <bk-table
                    :data="namingConventionData"
                    :outer-border="false"
                    row-auto-height
                    show-overflow-tooltip
                >
                    <bk-table-column
                        :label="$t('differenceItem')"
                        prop="difference"
                        :width="140"
                    />
                    <bk-table-column
                        :label="$t('traditionalStyle')"
                        prop="classic"
                        :width="290"
                    >
                        <template slot-scope="props">
                            <div class="label-column">
                                <p>{{ props.row.classic }}</p>
                                <p>{{ props.row.classicExample }}</p>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('constraintStyle')"
                        prop="constrainedMode"
                    >
                        <template slot-scope="props">
                            <div class="label-column">
                                <p>{{ props.row.constrainedMode }}</p>
                                <p>{{ props.row.constrainedExample }}</p>
                            </div>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
        </bk-popover>
        <bk-checkbox
            ext-cls="namingConvention-checkbox"
            v-model="inheritedDialect"
            @change="inheritedChange"
        >
            {{ $t('inheritedProject') }}
        </bk-checkbox>
        <bk-radio-group
            class="pipelinte-template-type-group"
            v-model="pipelineDialect"
            @change="pipelineDialectChange"
        >
            <bk-radio
                :value="'CLASSIC'"
                :disabled="isDialectDisabled"
            >
                <span class="radio-label">{{ $t('traditionalStyle') }}</span>
            </bk-radio>
            <bk-radio
                :value="'CONSTRAINED'"
                :disabled="isDialectDisabled"
            >
                <span class="radio-label">{{ $t('constraintStyle') }}</span>
            </bk-radio>
        </bk-radio-group>
    </div>
</template>

<script>

    export default {
        name: 'base-setting-tab',
        props: {
            inheritedDialect: Boolean,
            pipelineDialect: String
        },
        computed: {
            namingConventionData () {
                return [
                    {
                        difference: this.$t('expressionFormat'),
                        classic: this.$t('traditionalFormat'),
                        classicExample: this.$t('traditionalFormatExample'),
                        constrainedMode: this.$t('constraintFormat'),
                        constrainedExample: this.$t('constraintFormatExample')
                    },
                    {
                        difference: this.$t('variableValueTooLong'),
                        classic: this.$t('traditionalValueTooLongMode'),
                        constrainedMode: this.$t('constraintReadOnlyMode')
                    },
                    {
                        difference: this.$t('variableNotFound'),
                        classic: this.$t('traditionalNotFoundMode'),
                        constrainedMode: this.$t('constraintReadOnlyMode')
                    },
                    {
                        difference: this.$t('variableStandard'),
                        classic: this.$t('traditionalStandardMode'),
                        constrainedMode: this.$t('constraintStandardMode')
                    }
                ]
            },
            isDialectDisabled () {
                if (this.inheritedDialect) {
                    return true
                } else {
                    return false
                }
            }
        },
        methods: {
            inheritedChange (value) {
                this.isDialectDisabled = !this.isDialectDisabled
                this.$emit('inherited-change', value)
            },
            pipelineDialectChange (value) {
                this.$emit('pipeline-dialect-change', value)
            }
        }
    }
</script>

<style lang="scss" scoped>
.label {
    font-weight: bold;
    font-size: 12px;
    padding: 4px 0;
    color: #63656E;
    border-bottom: 1px dashed #63656E;
}

.label-column {
    padding: 4px 0;
}

.namingConvention-checkbox{
    position: absolute;
    top: 4px;
    margin-left: 16px;
    height: 24px;
    background: #F5F7FA;
    border-radius: 12px;
    padding: 4px 12px;
}

.pipelinte-template-type-group {
    margin-top: 10px;
}
</style>
