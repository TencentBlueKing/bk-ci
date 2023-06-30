<template>
    <div class="bk-pipeline-card-view">
        <header class="bk-pipeline-card-header">
            <aside class="bk-pipeline-card-header-left-aside">
                <h3>
                    <router-link
                        class="pipeline-cell-link"
                        :to="pipeline.historyRoute"
                    >
                        {{pipeline.pipelineName}}
                    </router-link>
                </h3>
                <p class="bk-pipeline-card-summary">
                    <span>
                        <logo size="16" name="record" />
                        {{pipeline.buildCount}}次
                    </span>
                    <span v-if="pipeline.viewNames" class="pipeline-group-names-span">
                        <logo size="16" name="pipeline-group" />
                        <span v-bk-tooltips="{ content: viewNamesStr, delay: [300, 0], allowHTML: false }">
                            {{viewNamesStr}}
                        </span>
                    </span>
                </p>
            </aside>
            <aside class="bk-pipeline-card-header-right-aside">
                <span
                    :class="{
                        'bk-pipeline-card-trigger-btn': true,
                        'disabled': pipeline.disabled
                    }"
                    v-bk-tooltips="pipeline.tooltips"
                    @click.stop="exec"
                >
                    <logo v-if="pipeline.lock" name="minus-circle"></logo>
                    <logo
                        v-else
                        name="play"
                    />
                </span>
                <bk-button
                    text
                    class="bk-pipeline-card-collect-btn"
                    :theme="pipeline.hasCollect ? 'warning' : ''"
                    @click="collectPipeline(pipeline)">
                    <i :class="{
                        'devops-icon': true,
                        'icon-star': !pipeline.hasCollect,
                        'icon-star-shape': pipeline.hasCollect
                    }" />
                </bk-button>
                <ext-menu :data="pipeline" ext-cls="bk-pipeline-card-more-trigger" :config="pipeline.pipelineActions" />
            </aside>
        </header>
        <section class="bk-pipeline-card-info">
            <i class="bk-pipeline-card-info-status-bar" :style="`background: ${statusColor}`"></i>

            <template v-if="pipeline.latestBuildNum">
                <div class="bk-pipeline-card-info-row build-result-row">
                    <span class="bk-pipeline-card-info-build-result" :style="`color: ${statusColor}`">
                        <pipeline-status-icon :status="pipeline.latestBuildStatus" />
                        {{ $t(`details.statusMap.${pipeline.latestBuildStatus}`) }}
                    </span>
                    <bk-tag>{{ timeTag }}</bk-tag>
                </div>
                <router-link
                    class="pipeline-cell-link bk-pipeline-card-info-row"
                    :to="pipeline.latestBuildRoute"
                >
                    <b>{{latestBuildNum}}</b>
                    <span class="bk-pipeline-card-info-build-msg">{{ pipeline.lastBuildMsg }}</span>
                </router-link>
                <p class="bk-pipeline-card-info-row bk-pipeline-card-desc-row">
                    <span>
                        <logo size="16" :name="pipeline.trigger" />
                        <span>{{ pipeline.latestBuildUserId }}</span>
                    </span>
                    <span v-if="pipeline.webhookAliasName">
                        <logo name="branch" size="16" />
                        <span>{{ pipeline.webhookAliasName }}</span>
                    </span>
                    <span v-if="pipeline.webhookMessage" class="desc">
                        <span>{{ pipeline.webhookMessage }}</span>
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
                v-if="!isRecentView"
                text
                size="small"
                theme="primary"
                @click="removeHandler(pipeline)"
            >
                {{$t('removeFromGroup')}}
            </bk-button>
        </div>
        <div v-else-if="!pipeline.hasPermission && !pipeline.delete" class="pipeline-card-apply-mask">
            <bk-button outline theme="primary" @click="applyPermission(pipeline)">
                {{$t('applyPermission')}}
            </bk-button>
        </div>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import PipelineStatusIcon from '@/components/PipelineStatusIcon'
    import ExtMenu from '@/components/pipelineList/extMenu'
    import { RECENT_USED_VIEW_ID } from '@/store/constants'
    import { statusColorMap } from '@/utils/pipelineStatus'

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
            },
            execPipeline: {
                type: Function,
                default: () => () => ({})
            },
            collectPipeline: {
                type: Function,
                default: () => () => ({})
            },
            applyPermission: {
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
            },
            timeTag () {
                return this.pipeline.progress || `${this.pipeline.latestBuildStartDate}(${this.pipeline.duration})`
            },
            viewNamesStr () {
                return this.pipeline.viewNames.join(';')
            },
            isRecentView () {
                return this.$route.params.viewId === RECENT_USED_VIEW_ID
            }
        },
        methods: {
            exec () {
                if (this.pipeline.disabled) return
                this.execPipeline(this.pipeline)
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/conf";
    @import '@/scss/mixins/ellipsis';
    .bk-pipeline-card-view {
        position: relative;
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
                        >:first-child {
                            margin: 0 4px;
                        }
                    }
                    .pipeline-group-names-span {
                        flex: 1;
                        display: flex;
                        overflow: hidden;
                        > span {
                            @include ellipsis();
                            flex: 1;
                        }
                    }
                }
            }
            .bk-pipeline-card-header-right-aside {
                display: flex;
                align-items: center;
                .bk-pipeline-card-trigger-btn,
                .bk-pipeline-card-collect-btn {
                    display: inline-flex;
                    cursor: pointer;
                    margin: 0 8px;
                    font-size: 16px;

                    &.disabled {
                        color: #DCDEE5;
                        cursor: not-allowed;
                    }
                    &.bk-pipeline-card-trigger-btn:not(.disabled):hover {
                        color: $primaryColor;
                    }
                }
                .bk-pipeline-card-more-trigger {
                    font-size: 24px;
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
                    display: grid;
                    column-gap: 16px;
                    grid-template-columns: auto auto auto;
                    > span {
                        display: flex;
                        align-items: center;
                        overflow: hidden;
                        > span {
                            display: flex;
                            @include ellipsis();
                            min-width: 0;
                        }
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
