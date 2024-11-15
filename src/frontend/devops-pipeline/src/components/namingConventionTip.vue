<template>
    <div style="width: 100%;">
        <h3>{{ $t('grammaticalDifferences') }}</h3>
        <table class="custom-table">
            <thead>
                <tr>
                    <th>{{ $t('differenceItem') }}</th>
                    <th>{{ $t('CLASSIC') }}</th>
                    <th>{{ $t('CONSTRAINED') }}</th>
                </tr>
            </thead>
            <tbody>
                <tr
                    v-for="(row, index) in namingConventionData"
                    :key="index"
                >
                    <td>
                        <div class="label-column">
                            <p>{{ row.difference }}</p>
                            <p v-if="row.differenceTip">{{ row.differenceTip }}</p>
                        </div>
                    </td>
                    <td style="width: 290px;">
                        <div class="label-column">
                            <div v-if="!row.classicKey">
                                <p>{{ row.classic }}</p>
                                <p>{{ row.classicExample }}</p>
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
                    </td>
                    <td>
                        <div class="label-column">
                            <p>{{ row.constrainedMode }}</p>
                            <p>{{ row.constrainedExample }}</p>
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
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
.custom-table {
  width: 100%;
  border-collapse: collapse;
    th, td {
        padding-left: 15px;
        padding-right: 15px;
    }
    tr {
        background-color: #fff;
        border-bottom: 1px solid #dfe0e5;
        height: 43px;
    }
    td {
        color: #575961;
        font-size: 12px;
    }
    th {
        background-color: #fafbfd;
        text-align: left;
        height: 43px;
        overflow: hidden;
        white-space: nowrap;
        word-wrap: normal;
        text-overflow: ellipsis;
        font-weight: 400;
        color: #313238
    }
    .label-column {
        word-break: keep-all;
        overflow-wrap: break-word;
    }
    .classicKey {
        font-weight: 700;
    }
}
</style>
