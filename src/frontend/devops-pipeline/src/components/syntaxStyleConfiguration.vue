<template>
    <div>
        <bk-popover
            v-if="isShowPopover"
            theme="light"
            :width="892"
            placement="top-start"
        >
            <label class="label">{{ $t('namingConvention') }}</label>
            <div slot="content">
                <NamingConventionTip />
            </div>
        </bk-popover>
        <bk-checkbox
            :ext-cls="{ 'namingConvention-checkbox': isShowPopover }"
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
                class="radio-label"
                :value="'CLASSIC'"
                :disabled="isDialectDisabled"
            >
                <span>{{ $t('CLASSIC') }}</span>
            </bk-radio>
            <bk-radio
                class="radio-label"
                :value="'CONSTRAINED'"
                :disabled="isDialectDisabled"
            >
                <span>{{ $t('CONSTRAINED') }}</span>
            </bk-radio>
        </bk-radio-group>
    </div>
</template>

<script>
    import NamingConventionTip from '@/components/namingConventionTip.vue'
    export default {
        name: 'base-setting-tab',
        components: {
            NamingConventionTip
        },
        props: {
            inheritedDialect: Boolean,
            pipelineDialect: String,
            isShowPopover: {
                type: Boolean,
                default: true
            }
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
    display: flex;

    .radio-label {
        margin-right: 10px;
    }
}
</style>
