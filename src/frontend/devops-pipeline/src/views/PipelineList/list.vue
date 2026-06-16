<template>
    <bk-resize-layout
        ref="resizeLayout"
        class="pipeline-group-section"
        collapsible
        :min="220"
        :max="400"
        :initial-divide="initialDivide"
        @collapse-change="handleCollapseChange"
        @after-resize="afterResize"
    >
        <pipeline-group-aside slot="aside" />
        <div
            slot="main"
            class="main-content"
        >
            <bk-alert
                v-if="showAlert && taskCount > 0"
                type="warning"
            >
                <div slot="title">
                    {{ $t('taskingDecs', [taskCount]) }}
                    <span
                        @click="handleAlertClose"
                        class="know-btn"
                    >{{ $t('IKnow') }}</span>
                </div>
            </bk-alert>
            <Component
                :is="ListRightComponent"
                ref="listComponent"
            />
        </div>
    </bk-resize-layout>
</template>

<script>
    import {
        PIPELINE_ASIDE_PANEL_TOGGLE,
        PIPELINE_GROUP_ASIDE_WIDTH_CACHE
    } from '@/store/constants'
    import { mapActions } from 'vuex'
    import PatchManageList from './PatchManageList'
    import PipelineGroupAside from './PipelineGroupAside'
    import PipelineManageList from './PipelineManageList'

    export default {
        components: {
            PipelineGroupAside,
            PipelineManageList,
            PatchManageList
        },
        data () {
            return {
                showAlert: true,
                taskCount: 0,
                initialDivide: Number(localStorage.getItem(PIPELINE_GROUP_ASIDE_WIDTH_CACHE)) || 280
            }
        },
        computed: {
            ListRightComponent () {
                return this.$route.params.type === 'patch' ? PatchManageList : PipelineManageList
            }
        },
        created () {
            this.fetchTaskCount()
        },
        mounted () {
            if (localStorage.getItem(PIPELINE_ASIDE_PANEL_TOGGLE) === 'true') {
                this.$refs.resizeLayout.setCollapse(true)
            }
        },
        methods: {
            ...mapActions('crossProjectCopy', [
                'getTaskCount'
            ]),
            async fetchTaskCount () {
                try {
                    const projectId = this.$route.params.projectId
                    const count = await this.getTaskCount({
                        projectId,
                        status: 'EXECUTING'
                    })
                    this.taskCount = count || 0
                    if (this.taskCount > 0) {
                        this.showAlert = true
                    }
                } catch (error) {
                    console.error('获取任务数量失败', error)
                }
            },
            handleCollapseChange (val) {
                localStorage.setItem(PIPELINE_ASIDE_PANEL_TOGGLE, JSON.stringify(val))
            },
            afterResize (width) {
                localStorage.setItem(PIPELINE_GROUP_ASIDE_WIDTH_CACHE, JSON.stringify(width))
            },
            handleAlertClose () {
                this.showAlert = false
                this.$nextTick(() => {
                    if (this.$refs.listComponent && typeof this.$refs.listComponent.updateTableHeight === 'function') {
                        this.$refs.listComponent.updateTableHeight()
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf';
    .pipeline-group-section {
        flex: 1;
        display: flex;
        overflow: hidden;
        border: 0;

        .flex-row {
            display: flex;
            align-items: center;
        }
        .main-content {
            display: flex;
            flex-direction: column;
            flex: 1;
            overflow: hidden;
            height: 100%;

            .know-btn {
                color: #3A84FF;
                cursor: pointer;
                margin-left: 8px;
            }
        }
        .pipeline-list-main {
            display: flex;
            flex-direction: column;
            flex: 1;
            padding: 24px;
            height: 100%;
            .pipeline-list-box {
                flex: 1;
                overflow: hidden;
            }

            .current-pipeline-group-name {
                display: flex;
                align-items: center;
                justify-content: space-between;
                font-size: 14px;
                line-height: 26px;
                margin: 0 0 16px 0;
                p > :first-child {
                    margin: 0 8px 0 0;
                }
                > span {
                    font-weight: bold;
                    color: #313238;
                }
                h5 {
                    color: #313238;
                    font-weight: normal;
                }
                .historical-task {
                    font-weight: 400;
                }
            }
            .pipeline-list-main-header {
                display: flex;
                justify-content: space-between;
                margin-bottom: 16px;
                .pipeline-list-main-header-left-area {
                    flex-shrink: 0;
                }
                .pipeline-list-main-header-right-area {
                    flex: 1;
                    display: flex;
                    margin-left: 100px;
                    .search-pipeline-input {
                        flex: 1;
                        width: 0;
                        background: white;
                        ::placeholder {
                            color: #c4c6cc;
                        }
                    }
                }
            }
            .hidden {
                visibility: hidden;
            }

            .pipeline-cell-link {
                color: $primaryColor;
                cursor: pointer;
                &:hover {
                    color: #699df4;
                }
                &[disabled] {
                    cursor: not-allowed;
                    color: $fontColor
                }
            }
        }
    }
</style>
