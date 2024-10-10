<template>
    <div style="width: 100%;">
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
            >
                <template slot-scope="props">
                    <div class="label-column">
                        <p>{{ props.row.difference }}</p>
                        <p>{{ props.row.differenceTip }}</p>
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('traditionalStyle')"
                prop="classic"
                :width="290"
            >
                <template slot-scope="props">
                    <div class="label-column">
                        <div v-if="!props.row.classicKey">
                            <p>{{ props.row.classic }}</p>
                            <p>{{ props.row.classicExample }}</p>
                        </div>
                        <div v-else>
                            <i18n
                                tag="p"
                                path="traditionalFunctionFormat"
                            >
                                <span class="classicKey">{{ $t('customExpressionsSatisfyRun') }}</span>
                            </i18n>
                        </div>
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
</template>

<script>
    export default {
        name: 'nameing-convention-tip',
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
                        difference: this.$t('expressionFunction'),
                        differenceTip: this.$t('expressionFunctionTip'),
                        classic: this.$t('traditionalFunctionFormat'),
                        classicKey: true,
                        constrainedMode: this.$t('constraintFunctionFormat'),
                        constrainedExample: this.$t('constraintFunctionFormatExample')
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
            }
        }
    }
</script>

<style lang="scss" scoped>
p {
  word-break: keep-all;
  overflow-wrap: break-word;
}
.classicKey {
  font-weight: 700;
}
</style>
