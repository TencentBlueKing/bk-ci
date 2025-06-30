<template>
    <div class="param-env-item">
        <div class="var-con">
            <div class="var-names-wrapper">
                <div
                    class="var-names"
                    :class="{ 'desc-param': desc }"
                    v-bk-tooltips="{ content: desc, disabled: !desc, allowHTML: true }"
                >
                    <span>{{ name }}</span>
                </div>
                <span
                    v-if="remark"
                    v-bk-tooltips="{ content: remark, placements: ['top'] }"
                >
                    <i class="bk-icon icon-info-circle"></i>
                </span>
            </div>
            <div
                class="var-operate"
                v-if="editable"
            >
                <i
                    v-bk-tooltips="{
                        content: disabledCopyTips,
                        placement: 'top-end',
                        delay: [300, 0],
                        disabled: !disabledCopyTips
                    }"
                    class="bk-icon icon-copy"
                    :class="{ 'disabled-copy': disabledCopy }"
                    @click.stop="handleCopy(bkVarWrapper(copyPrefix + name))"
                ></i>
            </div>
        </div>
    </div>
</template>

<script>
    import { bkVarWrapper, copyToClipboard } from '@/utils/util'
    export default {
        props: {
            name: {
                type: String,
                default: ''
            },
            desc: {
                type: String,
                default: ''
            },
            copyPrefix: {
                type: String,
                default: ''
            },
            editable: {
                type: Boolean,
                default: true
            },
            disabledCopy: {
                type: Boolean,
                default: false
            },
            disabledCopyTips: {
                type: String,
                default: ''
            },
            remark: {
                type: String,
                default: ''
            }
        },
        methods: {
            bkVarWrapper,
            async handleCopy (con) {
                if (this.disabledCopy) return
                try {
                    await copyToClipboard(con)
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('copySuc'),
                        limit: 1
                    })
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message,
                        limit: 1
                    })
                }
            }
        }
    }
</script>

<style scoped lang="scss">
    .param-env-item {
        position: relative;
        height: 40px;
        background: #fff;
        border: 1px solid #DCDEE5;
        margin-top: -1px;
        padding-left: 24px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        cursor: pointer;
        &:hover {
            border-color: #C4C6CC;
            /* &+div {
                border-top: none;
            } */
            .var-con {
                .var-names {
                    max-width: 362px;
                }
                .var-operate {
                    display: inline-block;
                }
            }
        }
        .var-con {
            width: 100%;
            display: flex;
            align-items: center;
            justify-content: space-between;
            .var-names-wrapper {
                display: flex;
                align-items: center;
            }
            .var-names {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                max-width: 392px;
                color: #313238;
                font-size: 12px;
                letter-spacing: 0;
                line-height: 20px;
                margin-right: 5px;
            }
            .desc-param {
                border-bottom: 1px dashed #979BA5;
            }
            .var-operate {
                display: none;
                i {
                    margin-right: 16px;
                    cursor: pointer;
                    font-size: 14px;
                    color: #63656E;
                }
                .disabled-copy {
                    cursor: not-allowed;
                    color: #C4C6CC;
                }
            }
        }
    }
</style>
