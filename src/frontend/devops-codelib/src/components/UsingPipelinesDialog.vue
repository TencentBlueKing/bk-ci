<template>
    <bk-dialog
        ext-cls="using-pipelines-dialog bk-devops-center-align-dialog"
        :width="400"
        :value="isShow"
        :quick-close="false"
        :show-footer="false"
        header-position="left"
        render-directive="if"
        :draggable="false"
        :on-close="handleClose"
    >
        <span class="using-pipeline-warning-icon">
            <i class="devops-icon icon-exclamation" />
        </span>

        <span class="using-confirm-title">
            {{ $t('codelib.以下流水线仍在使用此代码库') }}
        </span>
        <p class="using-confirm-desc">
            {{ $t('codelib.请前往流水线处理后，再删除代码库') }}
        </p>
        <ul
            class="operate-pipeline-list"
            @scroll.passive="handleScroll"
        >
            <li
                v-for="pipeline in pipelinesList"
                :key="pipeline.pipelineId"
               
            >
                <a @click="handleToPipeline(pipeline)">{{ pipeline.pipelineName }}</a>
            </li>
        </ul>
        <footer>
            <bk-button @click="handleClose">{{$t('codelib.关闭')}}</bk-button>
        </footer>
    </bk-dialog>
</template>

<script>
    export default {
        props: {
            isShow: {
                type: Boolean,
                default: false
            },
            isLoadingMore: Boolean,
            hasLoadEnd: Boolean,
            pipelinesList: {
                type: Array,
                default: () => []
            },
            fetchPipelinesList: {
                type: Function
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        methods: {
            handleClose () {
                this.$emit('update:isShow', false)
            },

            handleScroll (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 30 && !this.hasLoadEnd && !this.isLoadingMore) {
                    this.fetchPipelinesList()
                }
            },

            handleToPipeline (pipeline) {
                window.open(`/console/pipeline/${this.projectId}/${pipeline.pipelineId}`, '__blank')
            }
        }
    }
</script>

<style lang="scss">
    .using-pipelines-dialog {
        text-align: center;
         .bk-dialog-body {
            display: flex;
            flex-direction: column;
            align-items: center;
            max-height: calc(60vh);
        }
        .using-pipeline-warning-icon {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background-color: #FFE8C3;
            color: #FF9C01;
            width: 42px;
            height: 42px;
            font-size: 24px;
            border-radius: 50%;
            flex-shrink: 0;
        }
        .using-confirm-title {
            font-size: 20px;
            color: #313238;
            margin: 20px 0 8px;
        }
        .using-confirm-desc {
            font-size: 14px;
            color: #63656E;;
            margin-bottom: 25px;
        }
        .operate-pipeline-list {
            border: 1px solid #DCDEE5;
            border-radius: 2px;
            overflow: auto;
            flex: 1;
            width: 100%;
            margin-bottom: 25px;
            
            > li {
                width: 100%;
                height: 40px;
                padding: 0 16px;
                display: flex;
                align-items: center;
                overflow: hidden;
                text-align: left;
                border-bottom: 1px solid #DCDEE5;
                &:last-child {
                    border-bottom: 0;
                }
            }
        }
        ::-webkit-scrollbar {
            width: 4px !important;
            height: 4px !important;
        }
    }
</style>
