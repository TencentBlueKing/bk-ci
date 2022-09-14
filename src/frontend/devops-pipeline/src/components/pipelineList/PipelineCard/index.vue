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
                    <span v-if="pipeline.viewNames" class="pipeline-group-names-span">
                        <logo size="12" name="placeholder" />
                        {{pipeline.viewNames.join(';')}}
                    </span>
                </p>
            </aside>
            <aside class="bk-pipeline-card-header-right-aside">
                <logo class="bk-pipeline-card-trigger-btn" name="pause"></logo>
                <ext-menu :data="pipeline" class="bk-pipeline-card-more" :config="pipeline.pipelineActions" />
            </aside>
        </header>
        <section class="bk-pipeline-card-info">
            <i class="bk-pipeline-card-info-status-bar" :style="`background: ${statusColor}`"></i>

            <template v-if="pipeline.latestBuildNum">
                <div class="bk-pipeline-card-info-row build-result-row">
                    <span class="bk-pipeline-card-info-build-result" :style="`color: ${statusColor}`">
                        <pipeline-status-icon :status="pipeline.latestBuildStatus" />
                        构建成功
                    </span>
                    <bk-tag>{{ pipeline.latestBuildStartDate }}</bk-tag>
                </div>
                <p class="bk-pipeline-card-info-row">
                    <b>{{latestBuildNum}}</b>
                    <span class="bk-pipeline-card-info-build-msg">{{ pipeline.lastBuildMsg }}</span>
                </p>
                <p class="bk-pipeline-card-info-row bk-pipeline-card-desc-row">
                    <span>
                        <logo size="16" :name="pipeline.trigger" />
                        {{ pipeline.latestBuildUserId }}
                    </span>
                    <span v-if="pipeline.webhookRepoUrl">
                        <logo size="16" name="manualTrigger" />
                        {{ pipeline.webhookRepoUrl }}
                    </span>
                </p>
            </template>
            <div v-else class="un-exec-pipeline-card-info">
                <pipeline-status-icon :status="pipeline.latestBuildStatus" />
                {{ $t('unexecute') }}
            </div>
        </section>
        <div v-if="pipeline.delete" class="pipeline-card-delete-mask">
            <span>{{$t('alreadyDeleted')}}</span>
            <bk-button
                text
                size="small"
                theme="primary"
                @click="removeHandler(pipeline)"
            >
                {{$t('removeqFrom')}}
            </bk-button>
        </div>
        <div v-else-if="!pipeline.hasPermission" class="pipeline-card-apply-mask">
            <bk-button outline theme="primary" @click="applyPermission(pipeline)">
                {{$t('applyPermission')}}
            </bk-button>
        </div>
    </div>
</template>

<script>
    import ExtMenu from '@/components/pipelineList/extMenu'
    import PipelineStatusIcon from '@/components/PipelineStatusIcon'
    import Logo from '@/components/Logo'
    import { statusColorMap } from '@/utils/pipelineStatus'
    import { bus } from '@/utils/bus'

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
            removeHandler: {
                type: Function,
                default: () => () => ({})
            }
        },
        computed: {
            latestBuildNum () {
                return this.pipeline.latestBuildNum ? `#${this.pipeline.latestBuildNum}` : '--'
            },
            statusColor () {
                return statusColorMap[this.pipeline.latestBuildStatus]
            }
        },
        methods: {
            applyPermission ({ pipelineName, pipelineId }) {
                bus.$emit(
                    'set-permission',
                    this.$permissionResourceMap.pipeline,
                    this.$permissionActionMap.view,
                    [{
                        id: pipelineId,
                        name: pipelineName
                    }],
                    this.$route.params.projectId
                )
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/conf";
    @import '@/scss/mixins/ellipsis';
    .bk-pipeline-card-view {
        position: relative;
        width: 408px;
        height: 188px;
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
                    .pipeline-group-names-span {
                        flex: 1;
                        @include ellipsis();
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
            padding: 8px 16px;
            margin: 12px 0;
            display: flex;
            flex-direction: column;
            align-items: center;
            height: 96px;
            border-radius: 4px;
            font-size: 12px;
            overflow: hidden;
            .bk-pipeline-card-info-status-bar {
                position: absolute;
                left: 0;
                top: 0;
                width: 4px;
                height: 100%;
            }
            .un-exec-pipeline-card-info {
                width: 100%;
                height: 100%;
                display: flex;
                align-items: center;
                color: #C4C6CC;
                > :first-child {
                    margin-right: 10px;
                }
            }
            .bk-pipeline-card-info-row {
                display: flex;
                align-self: stretch;
                align-items: center;
                flex: 1;
                &.build-result-row {
                    justify-content: space-between;
                    .bk-pipeline-card-info-build-result {
                        display: flex;
                        flex: 1;
                        align-items: center;
                        > :first-child {
                            margin-right: 10px;
                        }
                    }
                }
                .bk-pipeline-card-info-build-msg {
                    flex: 1;
                    @include ellipsis();
                }
                &.bk-pipeline-card-desc-row {
                    > span {
                        display: flex;
                        align-items: center;
                        color: #979BA5;
                    }
                }
                > b {
                    &:after {
                        content: "|";
                        margin: 0 8px;
                        font-weight: normal;

                    }
                }
            }
        }
        .pipeline-card-delete-mask,
        .pipeline-card-apply-mask {
            width: 100%;
            height: 100%;
            position: absolute;
            left: 0;
            top: 0;
            background: rgba(255, 255, 255, 0.5);
            display: flex;
            align-items: center;
            justify-content: center;
            flex-direction: column;
            font-size: 12px;
            color: #C4C6CC;
            &.pipeline-card-apply-mask {
                align-items: flex-end;
                justify-content: flex-start;
                padding: 25px 16px;
                background: rgba(234, 235, 240, .4)
            }
        }
    }
</style>
