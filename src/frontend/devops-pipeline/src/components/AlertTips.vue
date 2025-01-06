<template>
    <div
        :class="['alert-tips', {
            'expand': isShow
        }]"
    >
        <div class="content-warpper">
            <i class="bk-icon icon-info-circle tips-icon" />
            <div id="alert-message">
                <p v-if="title">{{ title }}</p>
                <div
                    :class="[{
                        'expand': isShow
                    }]"
                    v-html="formattedText"
                >
                </div>
            </div>
            <bk-button
                v-if="showExpandBtn"
                class="expand-btn"
                text
                @click="handleExpand"
            >
                <bk-icon
                    type="right-shape shape-icon"
                    :class="[{
                        'expand': isShow
                    }]"
                />
                {{ isShow ? $t('settings.fold') : $t('settings.open') }}
            </bk-button>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'AlertTips',
        props: {
            title: {
                type: String,
                default: ''
            },
            message: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                isShow: false,
                showExpandBtn: false
            }
        },
        computed: {
            formattedText () {
                return this.message.replace(/\n/g, '<br>')
            }
        },
        watch: {
            message: {
                handler () {
                    this.$nextTick(() => {
                        this.showExpandBtn = this.formattedText.includes('<br>')
                    })
                },
                immediate: true
            }
        },
        methods: {
            handleExpand () {
                this.isShow = !this.isShow
            }
        }
    }
</script>

<style lang="scss" scoped>
    .alert-tips {
        position: relative;
        align-items: baseline;
        width: 100%;
        font-size: 12px;
        padding: 0 15px;
        line-height: 20px;
        background: #F0F8FF;
        border: 2px solid #C5DAFF;
        border-radius: 4px;
        overflow: hidden;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        &.expand {
            max-height: 15em;
            overflow: auto;
            -webkit-line-clamp: unset;
        }
        .tips-icon {
            position: relative;
            bottom: 1px;
            color: #3A84FF;
            margin-right: 5px;
            flex-shrink: 0;
        }
        .expand-btn {
            position: absolute;
            top: 0;
            right: 16px;
            font-size: 12px;
            ::v-deep span {
                display: flex;
            }
            .shape-icon {
                position: relative;
                top: 0px;
                margin-right: 4px;
                transform: rotate(90deg);
                &.expand {
                    top: 2px;
                    transform: rotate(-90deg);
                }
            }
        }
    }
    .content-warpper {
        display: flex;
        align-items: baseline;
    }
    #alert-message {
        div {
            display: -webkit-box;
            -webkit-line-clamp: 1;
            -webkit-box-orient: vertical;
            &.expand {
                -webkit-box-orient: unset;
            }
        }
    }
</style>
