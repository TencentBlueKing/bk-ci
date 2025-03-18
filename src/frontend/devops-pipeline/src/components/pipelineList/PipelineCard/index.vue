<template>
    <div class="bk-pipeline-card-view">
        <header class="bk-pipeline-card-header">
            <aside class="bk-pipeline-card-header-left-aside">
                <h3>
                    <span
                        class="pipeline-cell-link"
                        @click="goPipeline(pipeline)"
                        v-bk-overflow-tips
                        v-perm="{
                            hasPermission: pipeline.permissions.canView,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId,
                                resourceType: 'pipeline',
                                resourceCode: pipeline.pipelineId,
                                action: RESOURCE_ACTION.VIEW
                            }
                        }"
                    >
                        {{ pipeline.pipelineName }}
                    </span>
                    <logo
                        class="ml5 template-mode-icon"
                        v-if="pipeline.templateId"
                        name="template-mode"
                        size="12"
                        v-bk-tooltips="$t('pipelineConstraintModeTips')"
                    />
                    <bk-tag
                        v-if="pipeline.onlyDraftVersion"
                        theme="success"
                        class="draft-tag"
                    >
                        {{ $t('draft') }}
                    </bk-tag>
                    <bk-tag
                        v-else-if="pipeline.onlyBranchVersion"
                        theme="warning"
                        class="draft-tag"
                    >
                        {{ $t('history.branch') }}
                    </bk-tag>
                </h3>
                <p class="bk-pipeline-card-summary">
                    <span>
                        <logo
                            size="16"
                            name="record"
                        />
                        {{ pipeline.buildCount }}{{ $t('runs') }}
                    </span>
                    <span
                        v-if="pipeline.viewNames"
                        class="pipeline-group-names-span"
                    >
                        <logo
                            size="16"
                            name="pipeline-group"
                        />
                        <span v-bk-tooltips="{ content: viewNamesStr, delay: [300, 0], allowHTML: false }">
                            {{ viewNamesStr }}
                        </span>
                    </span>
                </p>
            </aside>
            <aside class="bk-pipeline-card-header-right-aside">
                <span
                    v-if="!executeable"
                    class="bk-pipeline-card-trigger-btn"
                    @click="goPipeline(pipeline)"
                    v-perm="{
                        hasPermission: pipeline.permissions.canEdit,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId: projectId,
                            resourceType: 'pipeline',
                            resourceCode: pipeline.pipelineId,
                            action: RESOURCE_ACTION.EDIT
                        }
                    }"
                >
                    <i class="devops-icon icon-edit-line" />
                </span>
                <span
                    v-else
                    v-perm="{
                        hasPermission: pipeline.permissions.canExecute,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId,
                            resourceType: 'pipeline',
                            resourceCode: pipeline.pipelineId,
                            action: RESOURCE_ACTION.EXECUTE
                        }
                    }"
                    :class="{
                        'bk-pipeline-card-trigger-btn': true,
                        'disabled': pipeline.disabled
                    }"
                    v-bk-tooltips="pipeline.tooltips"
                    @click.stop="execPipeline(pipeline)"
                >
                    <logo
                        v-if="pipeline.lock"
                        name="minus-circle"
                    ></logo>
                    <logo
                        v-else
                        name="play"
                    />
                </span>
                <ext-menu
                    :data="pipeline"
                    ext-cls="bk-pipeline-card-more-trigger"
                    :config="pipeline.pipelineActions"
                />
            </aside>

            <div
                :class="{
                    'collect-btn-background': true,
                    'is-collect': pipeline.hasCollect
                }"
            >
                <bk-button
                    text
                    class="bk-pipeline-card-collect-btn"
                    :theme="pipeline.hasCollect ? 'warning' : ''"
                    @click="collectPipeline(pipeline)"
                >
                    <i
                        :class="{
                            'devops-icon': true,
                            'icon-star': !pipeline.hasCollect,
                            'icon-star-shape': pipeline.hasCollect
                        }"
                    />
                </bk-button>
            </div>
        </header>
        <section class="bk-pipeline-card-info">
            <i
                class="bk-pipeline-card-info-status-bar"
                :style="`background: ${statusColor}`"
            ></i>

            <template v-if="pipeline.latestBuildNum">
                <div class="bk-pipeline-card-info-row build-result-row">
                    <span
                        class="bk-pipeline-card-info-build-result"
                        :style="`color: ${statusColor}`"
                    >
                        <pipeline-status-icon :status="pipeline.latestBuildStatus" />
                        {{ $t(`details.statusMap.${pipeline.latestBuildStatus}`) }}
                    </span>
                    <bk-tag ext-cls="bk-pipeline-card-info-build-time-tag">
                        <span
                            class="bk-pipeline-card-info-build-time-tag-span"
                            v-bk-overflow-tips
                        >
                            {{ timeTag }}
                        </span>
                    </bk-tag>
                </div>
                <router-link
                    class="pipeline-cell-link bk-pipeline-card-info-row"
                    :to="pipeline.latestBuildRoute"
                >
                    <b>{{ latestBuildNum }}</b>
                    <span class="bk-pipeline-card-info-build-msg">{{ pipeline.lastBuildMsg }}</span>
                </router-link>
                <p class="bk-pipeline-card-info-row bk-pipeline-card-desc-row">
                    <span>
                        <logo
                            size="16"
                            :name="pipeline.startType"
                        />
                        <span>{{ pipeline.latestBuildUserId }}</span>
                    </span>
                    <span v-if="pipeline.webhookAliasName">
                        <logo
                            name="branch"
                            size="16"
                        />
                        <span>{{ pipeline.webhookAliasName }}</span>
                    </span>
                    <span
                        v-if="pipeline.webhookMessage"
                        class="desc"
                    >
                        <span>{{ pipeline.webhookMessage }}</span>
                    </span>
                </p>
            </template>
            <div
                v-else
                class="un-exec-pipeline-card-info"
            >
                <pipeline-status-icon :status="pipeline.latestBuildStatus" />
                {{ $t('unexecute') }}
            </div>
        </section>
        <div
            v-if="pipeline.delete"
            class="pipeline-card-delete-mask"
        >
            <span>{{ $t('alreadyDeleted') }}</span>
            <bk-button
                v-if="!isRecentView"
                text
                size="small"
                theme="primary"
                @click="removeHandler(pipeline)"
            >
                {{ $t('removeFromGroup') }}
            </bk-button>
        </div>
        <div
            v-else-if="!pipeline.permissions.canView && !pipeline.delete"
            class="pipeline-card-apply-mask"
        >
            <bk-button
                outline
                theme="primary"
                @click="applyPermission(pipeline)"
            >
                {{ $t('apply') }}
            </bk-button>
        </div>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import ExtMenu from '@/components/pipelineList/extMenu'
    import PipelineStatusIcon from '@/components/PipelineStatusIcon'
    import { RECENT_USED_VIEW_ID } from '@/store/constants'
    import {
        handlePipelineNoPermission,
        RESOURCE_ACTION
    } from '@/utils/permission'
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
            }
        },
        data () {
            return {
                RESOURCE_ACTION
            }
        },
        computed: {
            executeable () {
                return this.pipeline.released || this.pipeline.onlyBranchVersion
            },
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
            },
            projectId () {
                return this.$route.params.projectId
            }

        },
        methods: {
            applyPermission (pipeline) {
                handlePipelineNoPermission({
                    projectId: this.projectId,
                    resourceCode: pipeline.pipelineId,
                    action: RESOURCE_ACTION.VIEW
                })
            },
            goPipeline (pipeline) {
                const { onlyDraftVersion, pipelineId, projectId, historyRoute } = pipeline
                const editRoute = {
                    name: 'pipelinesEdit',
                    params: {
                        projectId,
                        pipelineId
                    }
                }
                if (onlyDraftVersion) {
                    this.$router.push(editRoute)
                    return
                }
                this.$router.push(historyRoute ?? editRoute)
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
        &:hover {
            .collect-btn-background {
                display: block !important;
            }
        }
        .bk-pipeline-card-header {
            display: flex;
            font-size: 12px;
            justify-content: space-between;
            .bk-pipeline-card-header-left-aside {
                display: flex;
                flex-direction: column;
                flex: 1;
                overflow: hidden;
                z-index: 150;

                > h3 {
                    color: $primaryColor;
                    margin: 0;
                    font-weight: normal;
                    display: flex;
                    line-height: 22px;
                    align-items: center;
                    grid-gap: 10px;
                    .pipeline-cell-link {
                        @include ellipsis();
                    }
                    .template-mode-icon {
                        flex-shrink: 0;
                    }
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
                .bk-pipeline-card-trigger-btn {
                    display: inline-flex;
                    cursor: pointer;
                    margin: 0 8px;
                    font-size: 16px;
                    color: #979BA5;
                    &.disabled {
                        color: #DCDEE5;
                        cursor: not-allowed;
                    }
                    .icon-edit-line {
                        font-size: 20px;
                    }
                    &.bk-pipeline-card-trigger-btn:not(.disabled):hover {
                        color: $primaryColor;
                    }
                }
                .bk-pipeline-card-more-trigger {
                    font-size: 24px;
                }
            }
            .collect-btn-background {
                display: none;
                position: absolute;
                left: 0;
                top: 0;
                z-index: 99;
                border-width: 36px 36px 0 0;
                border-style: solid;
                border-color: #f0f1f5 transparent transparent transparent;
                &.is-collect {
                    display: block;
                }
                .bk-pipeline-card-collect-btn {
                    position: absolute;
                    top: -35px;
                    left: -5px;
                    display: inline-flex;
                    cursor: pointer;
                    margin: 0 8px;
                    font-size: 14px;
                    &.disabled {
                        color: #DCDEE5;
                        cursor: not-allowed;
                    }
                    &.bk-pipeline-card-trigger-btn:not(.disabled):hover {
                        color: $primaryColor;
                    }
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
                    .bk-pipeline-card-info-build-time-tag {
                        overflow: hidden;
                        &-span {
                            width: 100%;
                            @include ellipsis();
                        }
                    }
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
                    color: #63656e;
                    &:hover {
                        color: $primaryColor;
                    }
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
