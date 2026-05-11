<template>
    <bk-dialog
        ext-cls="instance-message-dialog"
        padding="0"
        width="700"
        v-model="visible"
        :has-header="false"
        :show-footer="false"
        :mask-close="true"
    >
        <section class="error-detail-content">
            <!-- <div class="message-title">
                {{ title }}
            </div> -->
            <div class="message-subtitle">{{ $t('template.instantiationFailMsg') }}</div>
            <div class="fail-pipeline-content">
                <ul class="fail-list">
                    <li
                        class="item-row"
                        v-for="(item) in failListInfo"
                        :key="item.key"
                    >
                        <div class="pipeline-item">
                            <div
                                class="name"
                                v-bk-overflow-tips
                            >
                                {{ item.key }} {{ item.message }}
                            </div>
                            <ul
                                v-if="item.errors?.length"
                                class="error-details"
                            >
                                <li
                                    v-for="err in item.errors"
                                    :key="err.errorTitle"
                                >
                                    <span>{{ err.errorTitle }}</span>
                                    <ul
                                        class="error-details-item-list"
                                        v-for="errItem in err.errorDetails"
                                        :key="errItem"
                                    >
                                        <li v-html="errItem"></li>
                                    </ul>
                                </li>
                            </ul>
                        </div>
                    </li>
                </ul>
            </div>
        </section>
    </bk-dialog>
</template>

<script>
    import { parseErrorMsg } from '@/utils/util'
    export default {
        props: {
            showInstanceMessage: Boolean,
            showTitle: {
                type: Boolean,
                default: true
            },
            failList: {
                type: Array,
                default: () => []
            },
            successList: {
                type: Array,
                default: () => []
            },
            failMessage: {
                type: Object,
                default: () => ({})
            }
        },
        computed: {
            visible: {
                get () {
                    return this.showInstanceMessage
                },
                set (val) {
                    this.$emit('update:showInstanceMessage', val)
                }
            },
            title () {
                let msg = ''
                if (this.successList.length) {
                    msg = `${this.$t('template.instantiationSucTips', [this.successList.length])}${this.$t('template.instantiationFailTips', [this.failList.length])}`
                } else if (this.failList.length) {
                    msg = this.$t('template.instantiationFailTips', [this.failList.length])
                }
                return msg
            },
            failListInfo () {
                return this.failList.map(key => {
                    const msg = parseErrorMsg(this.failMessage[key])
                    return {
                        key,
                        ...msg
                    }
                })
            }
        },
        methods: {
            close () {
                this.visible = false
                this.$emit('cancel')
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../scss/conf';
    .instance-message-dialog {
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            margin: 0px;
        }
        .error-detail-content {
            padding: 30px 20px;
            min-height: 200px;
            max-height: 600px;
            display: flex;
            flex-direction: column;
            position: relative;
        }
        .message-title {
            margin-bottom: 20px;
            font-size: 16px;
            color: #333C48;
        }
        .message-subtitle {
            font-size: 14px;
            color: #333C48;
            margin-bottom: 10px;
        }
        .fail-pipeline-content {
            flex: 1;
            overflow: auto;
        }
        .fail-list {
            padding-left: 20px;
            margin-top: 10px;
        }
        .item-row {
            list-style: outside;
        }
        .pipeline-item {
            display: flex;
            flex-direction: column;
            .name {
                white-space: nowrap;
                max-width: 500px;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .error-details {
                font-size: 12px;
                margin-top: 8px;
                > li {
                    display: flex;
                    grid-gap: 8px;
                    flex-direction: column;
                    padding-left: 20px;
                    > span {
                        font-weight: 700;
                    }
                    > .error-details-item-list {
                        padding-left: 20px;
                        > li {
                            list-style: circle;
                            > a {
                                padding-left: 4px;
                                color: $primaryColor;
                            }
                        }
                    }
                }
            }
        }
    }
</style>
