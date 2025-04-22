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
        <Component
            :is="ListRightComponent"
            slot="main"
        />
    </bk-resize-layout>
</template>

<script>
    import {
        PIPELINE_ASIDE_PANEL_TOGGLE,
        PIPELINE_GROUP_ASIDE_WIDTH_CACHE
    } from '@/store/constants'
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
                initialDivide: Number(localStorage.getItem(PIPELINE_GROUP_ASIDE_WIDTH_CACHE)) || 280
            }
        },
        computed: {
            ListRightComponent () {
                return this.$route.params.type === 'patch' ? PatchManageList : PipelineManageList
            }
        },
        mounted () {
            if (localStorage.getItem(PIPELINE_ASIDE_PANEL_TOGGLE) === 'true') {
                this.$refs.resizeLayout.setCollapse(true)
            }
        },
        methods: {
            handleCollapseChange (val) {
                localStorage.setItem(PIPELINE_ASIDE_PANEL_TOGGLE, JSON.stringify(val))
            },
            afterResize (width) {
                localStorage.setItem(PIPELINE_GROUP_ASIDE_WIDTH_CACHE, JSON.stringify(width))
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
        .pipeline-list-main {
            display: flex;
            flex-direction: column;
            flex: 1;
            padding: 24px;
            overflow: hidden;
            height: 100%;
            .pipeline-list-box {
                flex: 1;
                overflow: hidden;
            }

            .current-pipeline-group-name {
                display: flex;
                align-items: center;
                font-size: 14px;
                line-height: 26px;
                margin: 0 0 16px 0;
                color: #313238;
                font-weight: normal;
                > :first-child {
                    margin: 0 8px 0 0;
                }
                > span {
                    font-weight: bold;

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
