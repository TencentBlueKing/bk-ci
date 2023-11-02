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
        class="step-execute-variable-view"
        v-bkloading="{ isLoading: isLoading, opacity: .1 }">
        <scroll-faker theme="dark">
            <table>
                <thead>
                    <tr>
                        <td style="width: 90px;">{{ $t('history.变量名称') }}</td>
                        <td style="width: 90px;">{{ $t('history.变量类型') }}</td>
                        <td>{{ $t('history.变量值') }}</td>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="(item, index) in variableList" :key="index">
                        <td>{{ item.name }}</td>
                        <td>{{ item.typeText }}</td>
                        <td class="variable-value">{{ item.value }}</td>
                    </tr>
                </tbody>
            </table>
        </scroll-faker>
    </div>
</template>
<script>
    import TaskExecuteService from '@service/task-execute';
    
    export default {
        name: '',
        inheritAttrs: false,
        props: {
            name: String,
            stepInstanceId: {
                type: Number,
                required: true,
            },
            ip: {
                type: String,
            },
        },
        data () {
            return {
                isLoading: true,
                variableList: [],
            };
        },
        watch: {
            name: {
                handler () {
                    this.isLoading = true;
                    this.fetchStepVariables();
                },
                immediate: true,
            },
        },
        methods: {
            // 步骤使用的变量
            fetchStepVariables () {
                if (!this.ip) {
                    this.isLoading = false;
                    return;
                }
                TaskExecuteService.fetchStepVariables({
                    id: this.stepInstanceId,
                    ip: this.ip,
                }).then((data) => {
                    this.variableList = Object.freeze(data);
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
        },
    };
</script>
<style lang='postcss'>
    .step-execute-variable-view {
        height: 100%;
        padding: 0 20px;
        font-family: Monaco, Menlo, "Ubuntu Mono", Consolas, source-code-pro, monospace;
        color: #c4c6cc;
        white-space: pre-line;

        table {
            width: 100%;
        }

        th,
        td {
            height: 40px;
            padding-right: 10px;
            border-bottom: 1px solid #3b3c42;
        }

        th {
            color: #ccc;
        }

        td {
            color: #979ba5;

            &:first-child {
                white-space: pre;
            }

            &.variable-value {
                word-break: break-word;
            }
        }
    }
</style>
