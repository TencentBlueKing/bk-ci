
<template>
    <div class="execute-history-file-server">
        <div
            class="server-agent-text"
            @click="handlerView"
        >
            123
        </div>
        <bk-dialog
            v-model="isShowDetail"
            class="execute-distory-step-view-server-detail-dialog"
            :ok-text="$t('environment.关闭')"
            :width="1020">
            <template #header>
                <div class="title">
                    <span>{{ $t('environment.服务器文件-服务器列表') }}</span>
                    <i
                        class="dialog-close-btn bk-icon icon-close"
                        @click="handleClose" />
                </div>
            </template>
            <div class="content-wraper">
                123
            </div>
            <template #footer>
                <bk-button @click="handleClose">
                    {{ $t('关闭') }}
                </bk-button>
            </template>
        </bk-dialog>
    </div>
</template>
<script>
    export default {
        props: {
            data: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                isShowDetail: false,
                hostNodeInfo: {}
            }
        },
        methods: {
            handlerView () {
                this.hostNodeInfo = this.data.host.hostNodeInfo
                this.isShowDetail = true
            },
            handlerClose () {
                this.isShowDetail = false
            }
        }
    }
</script>
<style lang='scss'>
.execute-history-file-server {
    min-height: 30px;
    padding: 5px;
    margin-left: -6px;
    cursor: pointer;

    &:hover {
        background: #f0f1f5;
    }

    .server-agent-text {
        .sep-location {
            &::before {
                content: "";
            }
        }
    }
}

.execute-distory-step-view-server-detail-dialog {
    .ip-selector-view-host{
        margin-top: 0 !important;
    }

    .bk-dialog-tool {
        display: none;
    }

    .bk-dialog-header,
    .bk-dialog-footer {
        position: relative;
        z-index: 99999;
        background: #fff;
    }

    .bk-dialog-header {
        padding: 0;
    }

    .bk-dialog-wrapper .bk-dialog-header .bk-dialog-header-inner {
        font-size: 20px;
        color: #000;
        text-align: left;
    }

    .bk-dialog-wrapper .bk-dialog-body {
        padding: 0;

        .server-panel {
            height: 100%;

            &.show-detail {
                overflow: hidden;
            }

            .host-detail.show {
                padding-left: 20%;
            }
        }
    }

    .content-wraper {
        height: 550px;
        margin-top: -1px;
    }

    button[name="cancel"] {
        display: none;
    }

    .title {
        position: relative;
        height: 68px;
        padding-top: 0;
        padding-bottom: 0;
        padding-left: 25px;
        font-size: 20px;
        line-height: 68px;
        color: #000;
        text-align: left;
        border-bottom: 1px solid #dcdee5;
    }

    .dialog-close-btn {
        position: absolute;
        top: 5px;
        right: 5px;
        z-index: 1;
        width: 26px;
        height: 26px;
        font-size: 22px;
        font-weight: 700;
        line-height: 26px;
        color: #979ba5;
        text-align: center;
        cursor: pointer;
        border-radius: 50%;

        &:hover {
            background-color: #f0f1f5;
        }
    }
}
</style>
