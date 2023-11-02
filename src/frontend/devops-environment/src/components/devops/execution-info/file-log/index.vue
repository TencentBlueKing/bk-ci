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
    <div
        ref="contentBox"
        class="file-download-log"
        @scroll="handleScroll"
        v-bkloading="{ isLoading, opacity: .1 }">
        <div>
            <file-item
                v-for="(item, index) in renderList"
                :key="index"
                :index="index"
                :open-memo="openMemo"
                :data="item"
                :render-content-map="renderContentMap"
                :is-content-loading="isAsyncContentLoading"
                @on-toggle="handleToggle" />
        </div>
        <div v-if="renderNums < contentList.length" ref="load" class="load-more">
            <div class="loading-flag">
                <Icon type="loading-circle" />
            </div>
            <div>{{ $t('history.加载中') }}</div>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';
    import TaskExecuteService from '@service/task-execute';
    import {
        getOffset,
    } from '@utils/assist';
    import mixins from '../../mixins';
    import FileItem from './log-item';

    export default {
        name: '',
        components: {
            FileItem,
        },
        mixins: [
            mixins,
        ],
        props: {
            name: String,
            stepInstanceId: {
                type: Number,
                required: true,
            },
            ip: {
                type: String,
            },
            batch: {
                type: [Number, String],
            },
            retryCount: {
                type: Number,
                required: true,
            },
            mode: {
                type: String,
                required: true,
            },
        },
        data () {
            return {
                // 基础信息loading
                isLoading: true,
                // 异步获取日志内容loading
                isAsyncContentLoading: false,
                // 文件基础信息列表
                contentList: [],
                // 一屏显示最小日志行数
                onePageNums: 0,
                // 前端分页页码
                page: 0,
                pageSize: 10,
                // 被展开的日志记录
                openMemo: {},
                // 被展开的文件具体日志内容
                renderContentMap: {},
            };
        },
        computed: {
            renderNums () {
                return this.onePageNums + this.pageSize * this.page;
            },
            renderList () {
                return this.contentList.slice(0, this.renderNums);
            },
        },
        watch: {
            /**
             * @desc 查看的日志目标改变，重新获取日志
             */
            name: {
                handler () {
                    this.isLoading = true;
                    this.isMemoChange = false;
                    this.page = 0;

                    this.fetchData();
                    if (this.$refs.contentBox) {
                        this.$refs.contentBox.scrollTop = 0;
                    }
                },
                immediate: true,
            },
        },
        created () {
            // 缓存是否有展开日志的操作
            this.isMemoChange = false;
            // 前端分页加载器，控制触发频率
            this.timer = null;
            // 日志列表中是否包含日志内容标记，如果不包含需要异步获取日志内容
            this.includingLogContent = '';
        },
        mounted () {
            this.calcFirstPageNums();
            window.addEventListener('resize', this.handleScroll);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.handleScroll);
            });
        },
        methods: {
            /**
             * @desc 获取文件日志
             *
             * 默认展开第一个文件的日志
             * 如果文件信息里面不包含日志内容，需要异步获取文件内容
             */
            fetchData () {
                if (!this.ip) {
                    this.isLoading = false;
                    this.contentList = [];
                    return;
                }
                TaskExecuteService.fetchFileLogOfIp({
                    stepInstanceId: this.stepInstanceId,
                    retryCount: this.retryCount,
                    ip: this.ip,
                    batch: this.batch,
                    mode: this.mode,
                }).then((data) => {
                    const {
                        fileDistributionDetails,
                        includingLogContent,
                        finished,
                    } = data;
                    this.contentList = Object.freeze(fileDistributionDetails);
                    this.includingLogContent = includingLogContent;
                    // 初始展开第一个
                    if (this.contentList.length > 0 && !this.isMemoChange) {
                        this.openMemo = {
                            [this.contentList[0].taskId]: true,
                        };
                        this.isMemoChange = true;
                    }
                    // 日志量大异步获取展开的文件
                    this.fetchFileLogOfFile();
                    // 主机还在执行轮询日志
                    if (!finished) {
                        this.$pollingQueueRun(this.fetchData);
                    }
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 异步获取文件内容
             *
             * 如果日志列表中包含日志内容则不用重复获取
             */
            fetchFileLogOfFile: _.throttle(function () {
                if (this.includingLogContent) {
                    return;
                }
                this.isAsyncContentLoading = true;
                TaskExecuteService.fetchFileLogOfFile({
                    stepInstanceId: this.stepInstanceId,
                    retryCount: this.retryCount,
                    taskIds: Object.keys(this.openMemo),
                }).then((data) => {
                    this.renderContentMap = Object.freeze(data.reduce((result, item) => {
                        result[item.taskId] = item.logContent;
                        return result;
                    }, {}));
                })
                    .finally(() => {
                        this.isAsyncContentLoading = false;
                    });
            }, 500),
            /**
             * @desc 外部调用
             */
            resize () {
                this.handleScroll();
            },
            /**
             * @desc 计算首屏需要渲染的文件数
             */
            calcFirstPageNums () {
                const windowHeight = window.innerHeight;
                const { top } = getOffset(this.$refs.contentBox);
                this.onePageNums = Math.ceil((windowHeight - top) / 34) + 3;
            },
            /**
             * @desc 滚动分页加载
             */
            handleScroll: _.throttle(function () {
                if (!this.$refs.load) {
                    return;
                }
                const windowHeight = window.innerHeight;
                const { top } = this.$refs.load.getBoundingClientRect();
                if (windowHeight + 50 > top && !this.timer) {
                    // 本地日志加载器
                    this.timer = setTimeout(() => {
                        this.page += 1;
                        this.timer = 0;
                    }, 350);
                }
            }, 80),
            /**
             * @desc 文件日志展开收起
             *
             * 展开时需要重新获取一次日志
             */
            handleToggle (taskId, toggle) {
                const openMemo = { ...this.openMemo };
                openMemo[taskId] = toggle;
                this.openMemo = Object.freeze(openMemo);
                if (toggle) {
                    this.fetchFileLogOfFile();
                }
            },
        },
    };
</script>
<style lang='postcss'>
    @keyframes file-log-loading-ani {
        0% {
            transform: rotateZ(0);
        }

        100% {
            transform: rotateZ(360deg);
        }
    }

    .file-download-log {
        height: 100%;
        padding-top: 10px;
        overflow-y: scroll;

        &::-webkit-scrollbar {
            width: 14px;
        }

        &::-webkit-scrollbar-thumb {
            background-color: #3b3c42;
            border: 1px solid #63656e;
        }

        &::-webkit-scrollbar-corner {
            background-color: transparent;
        }

        .load-more {
            display: flex;
            align-items: center;
            height: 20px;
            padding-top: 20px;
            padding-bottom: 24px;
            padding-left: 44px;
            color: #fff;

            .loading-flag {
                display: flex;
                align-items: center;
                justify-content: center;
                width: 20px;
                height: 20px;
                transform-origin: center center;
                animation: file-log-loading-ani 1s linear infinite;
            }
        }
    }
</style>
