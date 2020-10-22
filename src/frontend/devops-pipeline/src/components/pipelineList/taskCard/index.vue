<template>
    <div class="task-card" :class="`${config.isRunning ? `task-${config.status}` : ''}`">
        <div v-if="!hasPermission" class="card-overflow">
        </div>
        <bk-button
            class="apply-button"
            theme="success"
            v-if="!hasPermission"
            @click="applyPermission(config)"
        >
            {{ $t('newlist.applyPerm') }}
        </bk-button>
        <div class="task-card-header">
            <a
                class="task-card-name text-overflow"
                :title="config.pipelineName"
                :href="getHistoryURL(config.pipelineId)"
                @click.stop.prevent="e => goHistory(e, config.pipelineId)"
            >
                <span class="template-tag" v-if="config.isInstanceTemplate">{{ $t('newlist.temp') }}</span>
                {{ config.pipelineName }}
            </a>
            <!-- 状态切换按钮 start -->
            <triggers
                :pipeline-id="config.pipelineId"
                :status="config.status"
                :can-manual-startup="canManualStartup"
                @exec="triggersExec"
            ></triggers>
            <!-- 状态切换按钮 end -->
            <!-- 角标 start -->
            <!-- <bk-popover :content="config.pipelineName" placement="right" class="corner-tips"> -->
            <div
                class="corner-mark"
                :class="config.status"
                v-if="!config.isRunning && (config.status === 'known_error' || config.status === 'success')"
            >
                <i class="devops-icon icon-exclamation" v-if="config.status === 'known_error'"></i>
                <i class="devops-icon icon-check-1" v-else></i>
            </div>
            <!-- </bk-popover> -->
            <!-- 角标 end -->
        </div>

        <!-- 任务未执行时，显示状态内容 start -->
        <template v-if="!config.isRunning">
            <div class="task-card-content" @click.stop="cardContentClick">
                <template v-if="config.content.length">
                    <p class="content-row" v-for="(row, cindex) of config.content" :key="row.key">
                        <span class="row-key">{{ row.key }}</span>
                        :
                        <span
                            class="row-value"
                            :class="!cindex ? config.status : ''"
                        >{{ row.value }}</span>
                    </p>
                </template>
            </div>

            <div class="task-card-footer">
                <template v-if="config.footer.length">
                    <div class="footer-item" v-for="(item, eIndex) of config.footer" :key="eIndex">
                        <div
                            class="card-footer-wrapper"
                            @click.stop="item.handler(config.pipelineId)"
                        >
                            <p class="upper-row">{{ item.upperText }}</p>
                            <p class="lower-row">{{ item.lowerText }}</p>
                        </div>
                    </div>
                </template>

                <!-- ext menu start -->
                <ext-menu :config="config"></ext-menu>
                <!-- ext menu end -->
            </div>
        </template>
        <!-- 任务未执行时，显示状态内容 end -->

        <!-- 任务执行时，显示进度条 start -->
        <template v-else>
            <a
                class="task-card-running-multi"
                :href="getHistoryURL(config.pipelineId)"
                @click.stop.prevent="e => goHistory(e, config.pipelineId)"
                v-if="config.runningInfo.buildCount > 1"
            >
                {{ $t('newlist.multipleBuilds') }}
            </a>
            <div class="task-card-running" @click.stop="cardContentClick" v-else>
                <div class="running-detail clearfix">
                    <div class="running-detail-text fl">{{ config.runningInfo.time }}</div>

                    <div class="running-detail-text fr">{{ config.runningInfo.percentage }}</div>
                </div>
                <progress-bar :percentage="config.runningInfo.percentage" :status="config.status"></progress-bar>
                <div class="running-log">{{ config.runningInfo.log }}</div>

                <p class="custom-btns" v-if="config.customBtns">
                    <template v-if="config.isRunning">
                        <a
                            href="javascript:;"
                            class="text-link"
                            v-for="(btn, bIndex) of config.customBtns"
                            @click.stop="emitEventHandler(btn.handler, config.pipelineId)"
                            :key="bIndex"
                        >
                            {{ btn.text }}
                            <i
                                class="devops-icon"
                                v-if="btn.icon"
                                :class="`icon-${btn.icon}`"
                            ></i>
                        </a>
                    </template>
                </p>
            </div>
        </template>
        <!-- 任务执行时，显示进度条 end -->
    </div>
</template>

<script>
    import mixins from '../pipeline-list-mixins'

    export default {
        mixins: [mixins],
        props: {
            canManualStartup: {
                type: Boolean,
                default: true
            },
            hasPermission: {
                type: Boolean,
                default: true
            },
            config: {
                type: Object,
                default () {
                    return {
                        buildId: 0,
                        buttonAllow: {},
                        content: [],
                        customBtns: [],
                        extMenu: [],
                        footer: [],
                        isRunning: false,
                        name: '',
                        runningInfo: {
                            time: '0',
                            percentage: '0%',
                            log: '',
                            buildCount: 0
                        },
                        status: 'success'
                    }
                }
            },
            index: {
                // 卡片当前在列表中的位置索引
                type: Number,
                default: 0
            }
        },
        data () {
            return {
                iconStatusMap: {
                    error: 'exclamation',
                    warning: 'pause',
                    success: 'check-1'
                },
                isShowExtMenu: false,
                extMenuTimer: -1,
                taskNameWidth: 0
            }
        },
        methods: {
            /**
             * 点击content部分的回调
             */
            cardContentClick () {
                const { config } = this

                if (config.status === 'not_built') {
                    this.$router.push({
                        name: 'pipelinesHistory',
                        params: {
                            projectId: this.$route.params.projectId,
                            pipelineId: config.pipelineId
                        }
                    })
                } else {
                    this.$router.push({
                        name: 'pipelinesDetail',
                        params: {
                            projectId: this.$route.params.projectId,
                            pipelineId: config.pipelineId,
                            buildNo: config.buildId
                        }
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
@import "./../../../scss/conf";

.card-overflow {
    width: 100%;
    height: 100%;
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    margin: auto;
    background-color: #333c48;
    z-index: 2;
    opacity: 0.6;
    border-radius: 3px;
}
.apply-button {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    z-index: 4;
    opacity: 1;
}
.task-card {
    position: relative;
    width: 311px;
    height: 215px;
    border: 1px solid $borderWeightColor;
    border-radius: 3px;
    box-shadow: 0 3px 6px rgba(0, 0, 0, 0.04);
    transition: all 0.3s ease;
    background-color: #fff;
    &:hover {
        box-shadow: 0 3px 8px 0 rgba(0, 0, 0, 0.2),
            0 0 0 1px rgba(0, 0, 0, 0.08);
    }
    &-header {
        position: relative;
        width: 100%;
        height: 60px;
        line-height: 60px;
        border-bottom: 1px solid $borderWeightColor;
        .bk-tooltip {
            width: 75%;
            height: 60px;
            margin-left: 24px;
            &-rel {
                width: 100%;
            }
            &-inner {
                white-space: normal;
            }
        }
        & > .task-card-name {
            display: block;
            width: 70%;
            margin-left: 24px;
        }
        .template-tag {
            display: inline-block;
            margin-top: -3px;
            line-height: 16px;
            border: 1px solid $primaryColor;
            border-radius: 2px;
            background-color: #e1f3ff;
            font-weight: 400;
            font-size: 12px;
            color: $primaryColor;
            vertical-align: middle;
            padding: 0px 1px;
        }
    }
    &-name {
        width: 90%;
        font-size: 18px;
        color: #333948;
        cursor: pointer;
        &:hover {
            color: $primaryColor;
        }
    }
    &-content {
        height: 95px;
        padding-left: 25px;
        cursor: pointer;
    }
    &-footer {
        display: flex;
        height: 58px;
        border-top: 1px solid $borderWeightColor;
        .footer-item {
            flex: 1;
            &:hover {
                background-color: #fafbfd;
                .upper-row {
                    color: $primaryColor;
                }
                .lower-row {
                    color: $fontWeightColor;
                }
            }
            & + .footer-item {
                border-left: 1px solid $borderWeightColor;
            }
        }
    }
    &-running {
        height: 153px;
        padding: 36px 24px 0;
        cursor: pointer;
        .running-detail {
            margin-bottom: 9px;
            padding: 0 10px;
            &-text {
                font-size: 14px;
            }
        }
        .running-log {
            height: 19px;
            margin-top: 18px;
            font-size: 14px;
            text-align: center;
        }
    }
    &-footer {
        position: relative;
    }
    &-running-multi {
        display: block;
        cursor: pointer;
        font-size: 14px;
        line-height: 155px;
        height: 155px;
        color: $primaryColor;
        text-align: center;
    }
    .content-row {
        font-size: 14px;
        &:first-child {
            padding-top: 25px;
        }
        & + .content-row {
            margin-top: 12px;
        }
    }
    .corner-mark {
        position: absolute;
        top: -2px;
        left: -2px;
        border-width: 18px;
        border-color: transparent transparent transparent transparent;
        border-style: solid;
        border-top-left-radius: 3px;
        & > .devops-icon {
            position: absolute;
            top: -15px;
            left: -15px;
            margin: 0;
            color: #fff;
        }
        &.known_error {
            border-color: $dangerColor transparent transparent $dangerColor;
        }
        &.success {
            border-color: $successColor transparent transparent $successColor;
        }
    }
    .custom-btns {
        padding-top: 3px;
        text-align: right;
        .text-link + .text-link {
            margin-left: 15px;
        }
    }
    .card-footer-wrapper {
        height: 100%;
        padding-top: 5px;
        cursor: pointer;
    }
    .upper-row {
        font-size: 24px;
        text-align: center;
        font-family: "PingFangSC-Regular", "Microsoft Yahei";
    }
    .lower-row {
        font-size: 12px;
        color: #c3cdd7;
        text-align: center;
    }
    .footer-ext-item {
        position: relative;
        width: 23px;
        height: 100%;
        border-left: 1px solid $borderWeightColor;
        cursor: pointer;
        &:hover,
        &.active {
            background-color: $bgHoverColor;
            .ext-dot {
                background-color: $primaryColor;
            }
        }
    }
    .row-key {
        display: inline-block;
        min-width: 90px;
        margin-right: 5px;
        text-align: right;
    }
    .row-value {
        margin-left: 5px;
        &.known_error {
            color: $dangerColor;
        }
        &.success {
            color: $successColor;
        }
    }
    .pipelines-triggers {
        position: absolute;
        top: 14px;
        right: 26px;
    }
}

@keyframes loading {
    from {
        transform: rotate(0);
    }
    to {
        transform: rotate(360deg);
    }
}
</style>
