<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
-->

<template>
    <div ref="infoBox" class="step-execution-info-box" :style="boxStyles">
        <div class="tab-container">
            <div class="tab-title" :class="host.result">
                <span class="host-ip">{{ host.displayIp || '--' }}</span>
            </div>
            <div class="split-line" />
            <div
                v-if="!isFile"
                class="tab-item"
                :class="{ active: activePanel === 'scriptLog' }"
                @click="handleTogglePanel('scriptLog')">
                {{ $t('history.执行日志') }}
            </div>
            <template v-if="isFile">
                <div
                    class="tab-item"
                    :class="{ active: activePanel === 'download' }"
                    @click="handleTogglePanel('download')">
                    {{ $t('history.下载信息') }}
                </div>
                <div
                    class="tab-item"
                    :class="{ active: activePanel === 'upload' }"
                    @click="handleTogglePanel('upload')">
                    {{ $t('history.上传源信息') }}
                </div>
            </template>
            <div
                v-if="isTask && !isFile"
                class="tab-item"
                :class="{ active: activePanel === 'variable' }"
                @click="handleTogglePanel('variable')">
                {{ $t('history.变量明细') }}
            </div>
            <div class="extend-box">
                <div
                    v-if="activePanel === 'scriptLog'"
                    class="extend-item"
                    v-bk-tooltips="$t('history.下载日志')"
                    @click="handleDownload">
                    <Icon type="download" />
                </div>
                <div
                    v-if="activePanel === 'scriptLog'"
                    class="extend-item"
                    @mouseenter="handleShowSetFont"
                    @mouseleave="handleHideSetFont">Aa</div>
                <div
                    v-if="!isFullscreen"
                    class="extend-item"
                    v-bk-tooltips="$t('history.全屏')"
                    @click="handleFullscreen">
                    <Icon type="full-screen" />
                </div>
                <div
                    v-if="isFullscreen"
                    class="extend-item"
                    v-bk-tooltips="$t('history.还原')"
                    @click="handleExitFullscreen">
                    <Icon type="un-full-screen" />
                </div>
                <div
                    v-if="activePanel === 'scriptLog'"
                    class="extend-item"
                    style="padding-left: 16px; border-left: 1px solid #262626;">
                    <bk-switcher
                        :value="isScriptLogLineFeed"
                        theme="primary"
                        size="small"
                        @change="handleScriptLogLineFeedChange" />
                    <span style="padding-left: 7px; font-size: 12px; color: #979ba5;">{{ $t('history.自动换行') }}</span>
                </div>
            </div>
        </div>
        <div class="tab-content-wraper" :style="contentStyles">
            <component
                ref="view"
                :key="activePanel"
                :is="renderCom"
                :name="`${stepInstanceId}_${host.ip}_${retryCount}`"
                :step-instance-id="stepInstanceId"
                :ip="host.ip"
                :batch="host.batch"
                :retry-count="retryCount"
                :font-size="fontSize"
                :mode="activePanel"
                :line-feed="isScriptLogLineFeed"
                v-bind="$attrs"
                v-on="$listeners" />
        </div>
        <div
            class="font-setting"
            :class="{ active: isFontSet }"
            @mouseenter="handleShowSetFont"
            @mouseleave="handleHideSetFont">
            <div class="font-setting-wraper">
                <div
                    class="font-item"
                    :class="{ active: fontSize === 12 }"
                    style="font-size: 12px;"
                    @click="handleFontChange(12)">Aa</div>
                <div class="line" />
                <div
                    class="font-item"
                    :class="{ active: fontSize === 13 }"
                    style="font-size: 13px;"
                    @click="handleFontChange(13)">Aa</div>
                <div class="line" />
                <div
                    class="font-item"
                    :class="{ active: fontSize === 14 }"
                    style="font-size: 14px;"
                    @click="handleFontChange(14)">Aa</div>
            </div>
        </div>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import TaskExecuteService from '@service/task-execute';
    import ScriptLog from './script-log';
    import FileLog from './file-log';
    import VariableView from './variable-view';

    const STEP_FONT_SIZE_KEY = 'step_execution_font_size';
    const SCRIPT_LOG_AUTO_LINE_FEED = 'script_log_line_feed';

    export default {
        name: '',
        inheritAttrs: false,
        props: {
            name: String,
            host: {
                type: Object,
                required: true,
            },
            stepInstanceId: {
                type: Number,
            },
            retryCount: {
                type: Number,
                required: true,
            },
            isTask: {
                type: Boolean,
                default: false,
            },
            isFile: {
                type: Boolean,
                default: false, // 展示文件日志
            },
        },
        data () {
            let fontSize = parseInt(localStorage.getItem(STEP_FONT_SIZE_KEY), 10);
            if (!fontSize || fontSize < 12) {
                fontSize = 12;
            } else if (fontSize > 14) {
                fontSize = 14;
            }

            return {
                activePanel: '',
                isFullscreen: false,
                isFontSet: false,
                fontSize,
                isScriptLogLineFeed: Boolean(localStorage.getItem(SCRIPT_LOG_AUTO_LINE_FEED)),
            };
        },
        computed: {
            renderCom () {
                const comMap = {
                    scriptLog: ScriptLog,
                    download: FileLog,
                    upload: FileLog,
                    variable: VariableView,
                };
                return comMap[this.activePanel];
            },
            boxStyles () {
                if (this.isFullscreen) {
                    return {
                        position: 'fixed',
                        top: 0,
                        right: 0,
                        bottom: 0,
                        left: 0,
                        zIndex: window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
                    };
                }
                return {};
            },
            contentStyles () {
                const lineHeightMap = {
                    12: 20,
                    13: 21,
                    14: 22,
                };
                return {
                    fontSize: `${this.fontSize}px`,
                    lineHeight: `${lineHeightMap[this.fontSize]}px`,
                };
            },
        },
        watch: {
            isFile: {
                handler (isFile) {
                    this.activePanel = isFile ? 'download' : 'scriptLog';
                },
                immediate: true,
            },
        },
        beforeDestroy () {
            this.handleExitFullscreen();
        },
        mounted () {
            window.addEventListener('keyup', this.handleExitByESC);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('keyup', this.handleExitByESC);
            });
        },
        methods: {
            /**
             * @desc 切换面板
             * @param {String} panel 选中的面板
             */
            handleTogglePanel (panel) {
                this.activePanel = panel;
            },
            /**
             * @desc 下载主机日志
             */
            handleDownload () {
                TaskExecuteService.fetchStepExecutionLogFile({
                    id: this.stepInstanceId,
                    ip: this.host.ip,
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: I18n.t('history.导出日志操作成功'),
                    });
                });
            },
            /**
             * @desc 显示字体大小设置面板
             */
            handleShowSetFont () {
                this.isFontSet = true;
            },
            /**
             * @desc 隐藏字体大小设置面板
             */
            handleHideSetFont () {
                this.isFontSet = false;
            },
            /**
             * @desc 更新日志字体大小
             */
            handleFontChange (fontSize) {
                this.fontSize = fontSize;
                localStorage.setItem(STEP_FONT_SIZE_KEY, fontSize);
            },
            /**
             * @desc 日志全屏
             */
            handleFullscreen () {
                this.isFullscreen = true;
                this.messageInfo(I18n.t('history.按 Esc 即可退出全屏模式'));
                this.infoBoxParentNode = this.$refs.infoBox.parentNode;
                document.body.appendChild(this.$refs.infoBox);
                this.$refs.view && this.$refs.view.resize();
            },
            /**
             * @desc 退出日志全屏
             */
            handleExitFullscreen (event) {
                this.isFullscreen = false;
                if (this.infoBoxParentNode) {
                    this.infoBoxParentNode.appendChild(this.$refs.infoBox);
                    this.infoBoxParentNode = null;
                }
                setTimeout(() => {
                    this.$refs.view && this.$refs.view.resize();
                });
            },
            /**
             * @desc esc键退出日志全屏
             */
            handleExitByESC (event) {
                if (event.keyCode === 27) {
                    this.handleExitFullscreen();
                }
            },
            /**
             * @desc 脚本日志自动换行
             * @param {Boolean} lineFeed
             */
            handleScriptLogLineFeedChange  (lineFeed) {
                this.isScriptLogLineFeed = lineFeed;
                if (lineFeed) {
                    localStorage.setItem(SCRIPT_LOG_AUTO_LINE_FEED, true);
                } else {
                    localStorage.removeItem(SCRIPT_LOG_AUTO_LINE_FEED);
                }
            },
        },
    };
</script>
<style lang='postcss'>
    .step-execution-info-box {
        position: relative;
        height: 100%;

        .tab-container {
            position: relative;
            z-index: 1;
            display: flex;
            font-size: 13px;
            line-height: 42px;
            color: #c4c6cc;
            background: #2f3033;
            box-shadow: 0 2px 4px 0 #000;
            align-items: center;

            .tab-title {
                width: 166px;
                font-size: 14px;
                color: #dcdee5;
                cursor: default;

                &.success,
                &.fail,
                &.running,
                &.waiting {
                    &::before {
                        display: inline-block;
                        width: 3px;
                        height: 12px;
                        background: #2dc89d;
                        content: "";
                    }
                }

                &.fail {
                    &::before {
                        background: #ea3636;
                    }
                }

                &.running {
                    &::before {
                        background: #699df4;
                    }
                }

                &.waiting {
                    &::before {
                        background: #dcdee5;
                    }
                }

                .host-ip {
                    margin-left: 13px;
                }
            }

            .split-line {
                width: 1px;
                height: 20px;
                margin-right: -1px;
                background: #63656e;
            }

            .tab-item {
                position: relative;
                height: 42px;
                padding: 0 20px;
                cursor: pointer;
                user-select: none;

                &.active {
                    z-index: 1;
                    color: #fff;
                    background: #212124;

                    &::before {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 2px;
                        background: #3a84ff;
                        content: "";
                    }
                }
            }

            .extend-box {
                display: flex;
                margin-left: auto;
                font-size: 18px;

                .extend-item {
                    display: flex;
                    height: 42px;
                    padding: 0 10px;
                    margin-left: 2px;
                    cursor: pointer;
                    align-items: center;
                    justify-content: center;
                }
            }
        }

        .tab-content-wraper {
            flex: 1;
            height: calc(100% - 42px);
            background: #1d1d1d;
        }

        .font-setting {
            position: absolute;
            top: 55px;
            right: 94px;
            z-index: 1;
            width: 160px;
            color: #979ba5;
            background: #2f3033;
            border: 1px solid;
            border-radius: 2px;
            opacity: 0%;
            visibility: hidden;
            transform: translateY(-15px);
            box-shadow: 0 2px 4px 0 rgb(0 0 0 / 50%);
            transition: all 0.15s;
            border-image-source: linear-gradient(#3b3c42, #292a2e);
            border-image-slice: 1;
            border-image-width: 1px;

            &.active {
                opacity: 100%;
                visibility: visible;
                transform: translateY(0);
            }

            .font-setting-wraper {
                position: relative;
                z-index: 1;
                display: flex;
                background: inherit;
                align-items: center;
            }

            &::before {
                position: absolute;
                top: -20px;
                right: 38px;
                width: 42px;
                height: 25px;
                cursor: pointer;
                content: "";
            }

            &::after {
                position: absolute;
                top: -5px;
                left: 50%;
                width: 11px;
                height: 11px;
                background: inherit;
                border: 1px solid #3b3c42;
                border-bottom: none;
                border-left: none;
                content: "";
                transform: translateX(-50%) rotateZ(-45deg);
            }

            .font-item {
                display: flex;
                height: 42px;
                cursor: pointer;
                transition: all 0.15s;
                align-items: center;
                flex: 1;
                justify-content: center;

                &:hover,
                &.active {
                    color: #fafbfd;
                }
            }

            .line {
                width: 1px;
                height: 18px;
                background: #63656e;
            }
        }
    }
</style>
