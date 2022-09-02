<template>
    <div class="bk-pipeline-card-view">
        <header class="bk-pipeline-card-header">
            <aside class="bk-pipeline-card-header-left-aside">
                <h3>{{ pipeline.pipelineName }}</h3>
                <p class="bk-pipeline-card-summary">
                    <span>
                        <logo size="12" name="placeholder" />
                        {{pipeline.buildCount}}次
                    </span>
                    <span>
                        <logo size="12" name="placeholder" />
                        {{pipeline.lastBuildTotalCount}} 插件
                    </span>
                </p>
            </aside>
            <aside class="bk-pipeline-card-header-right-aside">
                <logo class="bk-pipeline-card-trigger-btn" name="pause"></logo>
                <ext-menu class="bk-pipeline-card-more" />
            </aside>
        </header>
        <section class="bk-pipeline-card-info">
            <pipeline-status-icon :status="pipeline.latestBuildStatus" />
            <article class="bk-pipeline-card-article">
                <p class="bk-pipeline-card-row">
                    <b>{{latestBuildNum}}</b>
                    <span>{{ pipeline.latestBuildStartDate }}</span>
                </p>
                <p class="bk-pipeline-card-row bk-pipeline-card-desc-row">
                    <span>
                        <logo size="16" :name="pipeline.trigger" />
                        {{ latestBuildUserId }}
                    </span>
                    <span v-if="pipeline.webhookRepoUrl">
                        <logo size="16" name="manualTrigger" />
                        {{ pipeline.webhookRepoUrl }}
                    </span>
                </p>
            </article>
        </section>
        <footer v-if="showGroupInfo" class="bk-pipeline-card-footer">
            <bk-tag class="group-name-tag" v-for="group in pipeline.viewNames" :key="group">
                {{group}}
            </bk-tag>
        </footer>
    </div>
</template>

<script>
    import ExtMenu from '@/components/pipelineList/extMenu'
    import PipelineStatusIcon from '@/components/PipelineStatusIcon'
    import Logo from '@/components/Logo'

    export default {
        components: {
            Logo,
            ExtMenu,
            PipelineStatusIcon
        },
        props: {
            pipeline: {
                type: Object,
                required: true
            },
            hasPermission: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                showGroupInfo: true
            }
        },
        computed: {
            latestBuildNum () {
                return this.pipeline.latestBuildNum ? `#${this.pipeline.latestBuildNum}` : '--'
            }
        },
        methods: {

        }
    }
</script>

<style lang="scss">
    @import "@/scss/conf";
    @import '@/scss/mixins/ellipsis';
    .bk-pipeline-card-view {
        width: 300px;
        background: white;
        border: 1px solid #DCDEE5;
        border-radius: 2px;
        padding: 16px 16px 0 16px;
        .bk-pipeline-card-header {
            display: flex;
            font-size: 12px;
            justify-content: space-between;
            .bk-pipeline-card-header-left-aside {
                display: flex;
                flex-direction: column;
                flex: 1;
                overflow: hidden;

                > h3 {
                    color: $primaryColor;
                    margin: 0;
                    font-weight: normal;
                    @include ellipsis();
                }
                .bk-pipeline-card-summary {
                    display: flex;
                    align-items: center;
                    font-size: 12px;
                    color: #C4C6CC;
                    margin-top: 6px;
                    > span:first-child {
                        margin-right: 20px;
                    }
                    > span {
                        display: flex;
                        align-items: center;
                    }
                }
            }
            .bk-pipeline-card-header-right-aside {
                display: flex;
                align-items: center;
                .bk-pipeline-card-trigger-btn {
                    margin: 0 8px;
                }
                .bk-pipeline-card-more {
                    display: flex;
                    align-items: center;
                }
            }
        }
        .bk-pipeline-card-info {
            position: relative;
            background: #FAFBFD;
            padding: 8px 12px;
            margin: 12px 0;
            display: flex;
            align-items: center;
            border-radius: 4px;
            overflow: hidden;
            &:before {
                position: absolute;
                content: '';
                left: 0;
                width: 4px;
                height: 100%;
                background: #2DCB56;
            }
            .bk-pipeline-card-article {
                font-size: 12px;
                margin-left: 6px;
                flex: 1;
                .bk-pipeline-card-row {
                    display: flex;
                    align-items: center;
                    &:first-child {
                        margin-bottom: 4px;
                        > b {
                            margin-right: 24px;
                            color: #63656E;
                            position: relative;
                            &:after {
                                position: absolute;
                                content: '';
                                right: -12px;
                                width: 1px;
                                height: 100%;
                                background:#DCDEE5;
                            }
                        }
                    }
                    &.bk-pipeline-card-desc-row {
                        color: #C4C6CC;
                        > span:first-child {
                            margin-right: 20px;
                        }
                    }
                    > span {
                        display: flex;
                        align-items: center;
                    }

                }
            }
        }
        .bk-pipeline-card-footer {
            height: 50px;
            display: grid;
            grid-template-columns: repeat(auto-fill, 100px);
            align-items: center;
            overflow: hidden;
            grid-auto-rows: 50px;
            border-top: 1px solid #DCDEE5;
            .group-name-tag {
                @include ellipsis();
                max-width: 100px;
            }
        }
    }
</style>
