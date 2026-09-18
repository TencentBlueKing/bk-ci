<template>
    <div
        :class="['exec-material-row', {
            'fit-content': isFitContent
        }]"
    >
        <div class="material-row-info-spans">
            <span
                v-for="field in materialInfoKeys"
                :key="field"
            >
                <svg
                    v-if="isTAPDMaterialIcon(field)"
                    width="14"
                    height="14"
                    :viewBox="tapdMaterialIcon.viewBox"
                    fill="currentColor"
                    aria-hidden="true"
                >
                    <path
                        v-for="path in tapdMaterialIcon.paths"
                        :key="path"
                        :d="path"
                    />
                </svg>
                <logo
                    v-else
                    :name="iconArray[field] || 'commit'"
                    size="14"
                />
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
                    <logo
                        :name="iconArray[field] || 'commit'"
                        size="14"
                    />
                    <span v-bk-tooltips="{ delay: [300, 0], content: material.webhookBranch, allowHTML: false }">{{ material.webhookBranch }}</span>
                </span>
                <bk-popover
                    v-else
                    :content="materialInfoValueMap[field]"
                    :delay="[300, 0]"
                    class="material-span-tooltip-box"
                >
                    <span
                        :class="{
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
        <span
            v-if="showMore"
            @mouseenter="emitMouseEnter"
            @click="emitClick"
            class="exec-more-material"
        >
            <i class="devops-icon icon-ellipsis" />
        </span>
    </div>
</template>
<script>
    import Logo from '@/components/Logo'
    import { getMaterialIconByType } from '@/utils/util'

    const tapdIconTypeMap = {
        story: {
            viewBox: '0 0 32 32',
            paths: [
                'M18 5V3h-8v2H3v24h14.406v-2H5V7h5v2h8V7h5v10h2V5h-7zm-2 2h-4V5h4v2z',
                'M7.875 12h12.5v2h-12.5v-2zM7.875 17h9.813v2H7.875v-2zM7.875 22h6v2h-6v-2zM27.372 23.281H29v1h-1.628v-1zM25.818 20.472l1.151-1.151.707.707-1.151 1.151-.707-.707zM18.92 20.012l.718-.696 1.133 1.169-.718.696-1.133-1.169zM17.591 23.281h1.628v1h-1.628v-1zM22.813 17.997h1v1.628h-1v-1.628zM23.297 21.038c.709 0 1.381.278 1.884.781s.781 1.172.781 1.884V28h-5.331v-4.297c0-.709.278-1.381.781-1.884s1.172-.781 1.884-.781zm0-1c-.978 0-1.9.381-2.591 1.075s-1.075 1.613-1.075 2.591v4.3h-1.222v.997h9.778v-.997h-1.222v-4.3c0-.978-.381-1.9-1.075-2.591a3.645 3.645 0 00-2.594-1.075z',
                'M22.95 26.7l-.656-.594.956-1.063-1.241-1.244 1.544-1.544.625.625-.919.919 1.209 1.209z'
            ]
        },
        bug: {
            viewBox: '0 0 1024 1024',
            paths: [
                'M161 265c4.4 0 8 3.6 8 8 0 34.8 28.2 63 63 63h560c34.8 0 63.002-28.2 63.002-63 0-4.4 3.596-8 8-8h60c4.4 0 8 3.6 8 8 0 76.802-62.202 139-139.002 139v100h148c4.4 0 8 3.6 8 8v56c0 4.4-3.6 8-8 8H792v96c0 6.5-.288 12.912-.688 19.312C859.904 728.618 908 796.707 908 876c0 4.4-3.6 8-8 8h-56c-4.4 0-8-3.6-8-8 0-44.198-23.904-82.886-59.5-103.686-6 17.094-13.588 33.5-22.689 48.998-24.297 41.498-59.001 76.202-100.499 100.5C611.812 946.111 563.501 960 512 960c-51.494 0-99.69-13.894-141.187-38.189-41.495-24.294-76.2-59.005-100.5-100.499-9.099-15.395-16.72-31.802-22.72-48.998C211.9 793.114 188 831.804 188 876c0 4.4-3.6 8-8 8h-56c-4.4 0-8-3.6-8-8 0-79.293 48.096-147.382 116.687-176.688-.5-6.3-.687-12.813-.687-19.312v-96H84c-4.4 0-8-3.6-8-8v-56c0-4.4 3.6-8 8-8h148V412c-76.8 0-139-62.198-139-139 0-4.4 3.6-8 8-8h60zm147 147v268c0 36.8 9.713 72.006 27.811 102.906a204.34 204.34 0 0073.283 73.283A201.572 201.572 0 00474 880.499V484c0-4.4 3.6-8 8-8h60c4.4 0 8 3.6 8 8v396.5a201.572 201.572 0 0064.906-24.311 204.34 204.34 0 0073.283-73.283C706.288 752.006 716 716.8 716 680V412H308zM520 64c40 0 76.694 8.807 108.093 25.906 31.5 17.2 56.8 42.5 74 74C719.193 195.306 728 232.001 728 272c0 4.4-3.6 8-8 8h-56c-4.4 0-8-3.6-8-8 0-28.299-5.994-53.1-17.094-73.5-10.599-19.4-26.007-34.806-45.405-45.406C573.2 141.894 548.298 136 520 136h-16c-28.298 0-53.1 5.995-73.5 17.094-19.4 10.6-34.807 26.006-45.406 45.406C373.894 218.8 368 243.7 368 272c0 4.4-3.6 8-8 8h-56c-4.4 0-8-3.6-8-8 0-39.999 8.807-76.694 25.907-108.094 17.2-31.5 42.5-56.8 74-74C427.306 72.807 464 64 504 64h16z'
            ]
        }
    }

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
            isSubFlow () {
                return this.material?.channelType === 'CREATIVE_STREAM'
            },
            isTAPD () {
                return this.material?.webhookType === 'TAPD'
            },
            isArtifact () {
                return this.material?.webhookType === 'ARTIFACT'
            },
            tapdMaterialIcon () {
                return tapdIconTypeMap[this.material?.webhookEventType]
            },
            scmType () {
                if (this.isArtifact) {
                    return this.material.webhookType
                }
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
                    webhookAliasName: this.isTAPD ? 'codeTapdWebHookTrigger' : scmIcon,
                    webhookBranch: 'branch',
                    webhookCommitId: 'commit',
                    webhookSourceBranch: 'branch',
                    mrIid: 'webhook-mr',
                    tagName: 'webhook-tag',
                    noteId: 'webhook-note',
                    issueIid: 'webhook-issue',
                    reviewId: 'webhook-review',
                    webhookSourceTarget: 'branch',
                    parentPipelineName: this.isSubFlow ? 'sub-flow' : 'pipeline',
                    parentBuildNum: 'sharp',
                    materialName: this.isArtifact ? 'pipeline' : scmIcon,
                    materialId: 'link'
                }
            },
            materialInfoKeys () {
                if (this.isTAPD) {
                    return [
                        'webhookAliasName',
                        'materialName'
                    ]
                }
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
            isTAPDMaterialIcon (field) {
                return this.isTAPD && field === 'materialName' && this.tapdMaterialIcon
            },
            emitMouseEnter () {
                this.$emit('mouseenter')
            },
            emitClick () {
                this.$emit('click')
            },
            includeLink (field) {
                if (this.isTAPD) {
                    return field === 'materialName'
                }
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
            height: 38px;
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
