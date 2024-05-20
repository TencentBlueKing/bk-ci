<template>
    <div :class="['exec-material-row', {
        'fit-content': isFitContent
    }]">
        <div class="material-row-info-spans">
            <span v-for="field in materialInfoKeys" :key="field">
                <logo :name="iconArray[field] || 'commit'" size="14" />
                <bk-link
                    v-if="includeLink(field)"
                    class="material-span"
                    theme="primary"
                    target="_blank"
                    :href="getLink(field)"
                >
                    {{ formatField(field) }}
                </bk-link>
                <span
                    v-else-if="isMR && field === 'webhookSourceTarget'"
                    class="mr-source-target"
                >
                    <span v-bk-tooltips="{ delay: [300, 0], content: material.webhookSourceBranch, allowHTML: false }">{{ material.webhookSourceBranch }}</span>
                    <i class="devops-icon icon-arrows-right"></i>
                    <logo :name="iconArray[field] || 'commit'" size="14" />
                    <span v-bk-tooltips="{ delay: [300, 0], content: material.webhookBranch, allowHTML: false }">{{ material.webhookBranch }}</span>
                </span>
                <bk-popover
                    v-else
                    :content="materialInfoValueMap[field]"
                    :delay="[300, 0]"
                    class="material-span-tooltip-box"
                >
                    <span :class="{
                              'material-span': true,
                              'material-url': field === 'materialId'
                          }"
                        @click="handleToLink(field)"
                    >
                        {{ materialInfoValueMap[field] }}
                    </span>
                </bk-popover>
            </span>
        </div>
        <span v-if="showMore" @mouseenter="emitMouseEnter" @click="emitClick" class="exec-more-material">
            <i class="devops-icon icon-ellipsis" />
        </span>
    </div>
</template>
<script>
    import Logo from '@/components/Logo'
    import { getMaterialIconByType } from '@/utils/util'
    export default {
        emits: ['mouseEnter', 'click'],
        components: {
            Logo
        },
        props: {
            isWebhook: Boolean,
            isFitContent: {
                type: Boolean,
                default: true
            },
            showMore: {
                type: Boolean,
                default: true
            },
            material: {
                type: Object,
                required: true
            }

        },
        computed: {
            isMR () {
                return [
                    'MERGE_REQUEST',
                    'PULL_REQUEST',
                    'MERGE_REQUEST_ACCEPT'
                ].includes(this.material?.webhookEventType)
            },
            scmType () {
                return this.isWebhook ? `CODE_${this.material?.codeType}` : this.material?.scmType
            },
            isSVN () {
                return this.scmType === 'CODE_SVN'
            },
            iconArray () {
                const scmIcon = getMaterialIconByType(this.scmType)
                return {
                    aliasName: scmIcon,
                    branchName: 'branch',
                    newCommitId: 'commit',
                    webhookAliasName: scmIcon,
                    webhookBranch: 'branch',
                    webhookCommitId: 'commit',
                    webhookSourceBranch: 'branch',
                    mrIid: 'webhook-mr',
                    tagName: 'webhook-tag',
                    noteId: 'webhook-note',
                    issueIid: 'webhook-issue',
                    reviewId: 'webhook-review',
                    webhookSourceTarget: 'branch',
                    parentPipelineName: 'pipeline',
                    parentBuildNum: 'sharp',
                    materialName: scmIcon,
                    materialId: 'icon-link'
                }
            },
            materialInfoKeys () {
                if (!this.isWebhook) {
                    return [
                        'aliasName',
                        ...(this.isSVN ? [] : ['branchName']),
                        'newCommitId'
                    ]
                }
                switch (this.material?.webhookEventType) {
                    case 'PUSH':
                        return [
                            'webhookAliasName',
                            'webhookBranch',
                            'webhookCommitId'
                        ]
                    case 'MERGE_REQUEST':
                    case 'PULL_REQUEST':
                        return [
                            'webhookAliasName',
                            'webhookSourceTarget',
                            'mrIid'
                        ]
                    case 'MERGE_REQUEST_ACCEPT':
                        return [
                            'webhookAliasName',
                            'webhookSourceTarget',
                            'mrIid'
                        ]
                    case 'TAG_PUSH':
                        return [
                            'webhookAliasName',
                            'webhookBranch',
                            'tagName'
                        ]
                    case 'NOTE':
                        return [
                            'webhookAliasName',
                            'noteId'
                        ]
                    case 'ISSUES':
                        return [
                            'webhookAliasName',
                            'issueIid'
                        ]
                    case 'REVIEW':
                        return [
                            'webhookAliasName',
                            'reviewId'
                        ]
                    case 'POST_COMMIT':
                        return [
                            'webhookAliasName',
                            'webhookCommitId'
                        ]
                    case 'CHANGE_COMMIT':
                        return [
                            'webhookAliasName',
                            'webhookCommitId'
                        ]
                    case 'PARENT_PIPELINE':
                        return [
                            'parentPipelineName',
                            'parentBuildNum'
                        ]
                    default:
                        return this.material?.materialId
                            ? [
                                'materialName',
                                'materialId'
                            ]
                            : [
                                'webhookAliasName',
                                'webhookBranch'
                            ]
                }
            },
            materialInfoValueMap () {
                return this.materialInfoKeys.reduce((acc, key) => {
                    acc[key] = this.formatField(key)
                    return acc
                }, {})
            }
        },
        methods: {
            emitMouseEnter () {
                this.$emit('mouseenter')
            },
            emitClick () {
                this.$emit('click')
            },
            includeLink (field) {
                return [
                    'newCommitId',
                    'reviewId',
                    'issueIid',
                    'noteId',
                    'mrIid',
                    'tagName',
                    'webhookCommitId',
                    'parentBuildNum'
                ].includes(field) && !this.isSVN && this.getLink(field)
            },
            formatField (field) {
                switch (field) {
                    case 'reviewId':
                    case 'issueIid':
                    case 'noteId':
                    case 'mrIid':
                        return `[${this.material[field]}]`
                    case 'newCommitId':
                    case 'webhookCommitId':
                        return this.material?.[field]?.slice?.(0, 8) ?? '--'
                    default:
                        return this.material?.[field] ?? '--'
                }
            },
            getLink (field) {
                switch (field) {
                    case 'newCommitId':
                        return this.material?.url ?? ''
                    default:
                        return this.material?.linkUrl ?? ''
                }
            },

            handleToLink (field) {
                if (field === 'materialId') {
                    window.open(this.getLink(field), '_blink')
                }
            }
        }
    }
</script>
<style lang="scss">
    @import "@/scss/mixins/ellipsis";
    .exec-material-row {
            // padding: 0 0 8px 0;
            display: grid;
            grid-gap: 20px;
            grid-auto-flow: column;
            &.fit-content {
                grid-auto-columns: minmax(auto, max-content) 36px;
                .material-row-info-spans {
                    grid-auto-columns: minmax(auto, max-content);
                }
            }

            .material-row-info-spans {
                display: grid;
                grid-auto-flow: column;
                grid-gap: 20px;
                > span {
                    @include ellipsis();
                    display: inline-flex;
                    min-width: auto;
                    align-items: center;
                    > svg {
                        flex-shrink: 0;
                        margin-right: 6px;
                    }
                }
            }
            &.visible-material-row {
              border: 1px solid transparent;
              padding-bottom: 0px;
              align-items: center;

            }
            .exec-more-material {
                display: inline-flex;
                align-items: center;

            }

            .mr-source-target {
                display: grid;
                align-items: center;
                grid-auto-flow: column;
                grid-gap: 6px;
                .icon-arrows-right {
                    color: #C4C6CC;
                    font-weight: 800;
                }
                > span {
                    @include ellipsis();
                }
            }
            .material-span-tooltip-box {
                flex: 1;
                overflow: hidden;
                font-size: 0;
                > .bk-tooltip-ref {
                    width: 100%;
                    .material-span {
                        width: 100%;
                    }
                }
            }
            .material-span {
              @include ellipsis();
              font-size: 12px;
              .bk-link-text {
                font-size: 12px;
              }
            }
            .material-url {
                color: #3a84ff;
                cursor: pointer;
            }
          }
</style>
