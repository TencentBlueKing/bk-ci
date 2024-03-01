<template>
    <div class="param-container">
        <header :active="isShow" @click="toggleContent" class="var-header">
            <slot name="header">
                <bk-icon class="toggle-icon" type="right-shape" />
                <span>{{title}}</span>
            </slot>
        </header>
        <transition name="slideLeft">
            <section v-show="isShow" class="var-content">
                <slot name="content">
                    <div
                        v-for="(param) in list"
                        :key="param.id"
                        :class="['variable-item', {
                            'variable-item-editable': editable
                        }]"
                        @click="handleEdit(param.id)"
                    >
                        <div class="var-con">
                            <div class="var-names" :class="{ 'required-param': param.valueNotEmpty, 'desc-param': param.desc }" v-bk-tooltips="{ content: param.desc, disabled: !param.desc, allowHTML: false }">
                                <span>{{ param.id }}</span>
                                <span>({{ param.name || param.id }})</span>
                            </div>
                            <div class="value-operate-row" style="justify-content: space-between;">
                                <div class="param-value">
                                    <span v-if="param.readOnly" class="read-only">{{$t('readonlyParams')}}</span>
                                    <span class="default-value">{{ param.defaultValue || '--' }}</span>
                                </div>
                                <div v-if="editable" class="var-operate">
                                    <div class="operate-btns">
                                        <i @click.stop="handleCopy(bkVarWrapper('variables.' + param.id))" class="bk-icon icon-copy" style="margin-right: 12px;"></i>
                                        <i @click.stop="handleDelete(param.id)" class="bk-icon icon-minus-circle"></i>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </slot>
            </section>
        </transition>
    </div>
</template>

<script>
    import { bkVarWrapper } from '@/utils/util'
    export default {
        props: {
            showContent: {
                type: Boolean,
                default: true
            },
            title: {
                type: String,
                required: true
            },
            list: {
                type: Array,
                default: () => ([])
            },
            handleEdit: {
                type: Function,
                default: () => {}
            },
            handleDelete: {
                type: Function,
                default: () => {}
            },
            editable: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                isShow: this.showContent
            }
        },
        watch: {
            showContent (val) {
                this.isShow = val
            }
        },
        methods: {
            bkVarWrapper,
            handleCopy (con) {
                window.navigator.clipboard.writeText(con)
                this.$bkMessage({
                    theme: 'success',
                    message: this.$t('copySuc'),
                    limit: 1
                })
            },
            toggleContent: function () {
                this.isShow = !this.isShow
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/mixins/ellipsis.scss";
    .param-container {
        margin-bottom: 16px;
        .var-header {
            display: flex;
            color: #313238;
            padding: 0 12px;
            align-items: center;
            cursor: pointer;
            height: 32px;
            font-size: 14px;
            background: #EAEBF0;
            border-radius: 2px 2px 0 0;

            .toggle-icon {
                display: block;
                margin-right: 4px;
                color: #979BA5;
                transition: all 0.3s ease;
            }
            &[active] {
                .toggle-icon {
                    transform: rotate(90deg)
                }
            }
        }
        .var-content {
            width: 100%;
            /* min-height: 64px; */
            .variable-empty {
                height: 64px;
                border-bottom: 1px solid #DCDEE5;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 14px;
                color:#63656E;
            }
            .variable-item {
                width: 100%;
                position: relative;
                height: 64px;
                background: #fff;
                border: 1px solid #DCDEE5;
                margin-top: -1px;
                padding-left: 24px;
                display: flex;
                justify-content: space-between;
                align-items: center;
                &.variable-item-editable {
                    cursor: pointer;
                    &:hover {
                        border-color: #C4C6CC;
                        .var-operate {
                            display: block;
                        }
                    }
                }
                .var-con {
                    width: 100%;
                    font-size: 12px;
                    letter-spacing: 0;
                    line-height: 20px;
                    overflow: hidden;
                    .var-names {
                        color: #313238;
                    }
                    .desc-param {
                        display: inline;
                        border-bottom: 1px dashed #979BA5;
                    }
                    .value-operate-row {
                        width: 100%;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        .param-value {
                            display: flex;
                            align-items: center;
                            .default-value {
                                color: #979BA5;
                                max-width: 300px;
                                @include ellipsis();
                            }
                            .read-only {
                                font-size: 12px;
                                color: #63656E;
                                background: #F0F1F5;
                                border-radius: 2px;
                                margin: 0 4px 0 -2px;
                                padding: 0 4px;
                                transform: scale(0.83);
                            }
                        }

                    }
                    .required-param:before {
                        content: '* ';
                        color: red;
                        position: absolute;
                        left: 14px;
                        top: 11px;
                    }
                }
                .var-operate {
                    display: none;
                    .var-status {
                        margin-right: 16px;
                        display: flex;
                        align-items: center;
                        .circle {
                            margin-left: 8px;
                        }
                    }
                    .operate-btns {
                        display: flex;
                        align-items: flex-end;
                        padding: 0 18px;
                        i {
                            cursor: pointer;
                            font-size: 14px;
                            color: #63656E;
                        }
                    }
                }
            }
        }
    }

</style>
