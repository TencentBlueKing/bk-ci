<template>
    <div class="param-env-item">
        <div class="var-con">
            <div class="var-names"
                :class="{ 'desc-param': desc }"
                v-bk-tooltips="{ content: desc, disabled: !desc }">
                <span>{{ name }}</span>
            </div>
            <div class="var-operate">
                <i
                    class="bk-icon icon-copy"
                    :class="{ 'disabled-copy': disabledCopy }"
                    @click.stop="handleCopy(bkVarWrapper(copyPrefix + name))"
                ></i>
            </div>
        </div>
    </div>
</template>

<script>
    import { bkVarWrapper } from '@/utils/util'
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
            disabledCopy: {
                type: Boolean,
                default: false
            }
        },
        methods: {
            bkVarWrapper,
            handleCopy (con) {
                if (this.disabledCopy) return
                window.navigator.clipboard.writeText(con)
                this.$bkMessage({
                    theme: 'success',
                    message: this.$t('copySuc')
                })
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
            .var-names {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                max-width: 392px;
                color: #313238;
                font-size: 12px;
                letter-spacing: 0;
                line-height: 20px;
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
