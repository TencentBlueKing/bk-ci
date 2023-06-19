<template>
    <div class="exec-material-row">
        <div class="material-row-info-spans">
            <span v-for="field in materialInfoKeys" :key="field">
                <logo :name="iconArray[field] || 'commit'" size="14" />
                <!-- <bk-link
                    v-if="includeLink(field)"
                    class="material-span"
                    theme="primary"
                    target="_blank"
                    :href="getLink(field)"
                >
                    {{ formatField(field) }}
                </bk-link> -->
                <span
                    v-if="isMR && field === 'webhookSourceTarget'"
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
                    <span class="material-span">
                        {{ materialInfoValueMap[field] }}
                    </span>
                </bk-popover>
            </span>
        </div>
        <span v-if="showMore" @mouseenter="emitMouseEnter" class="exec-more-material">
            <i class="devops-icon icon-ellipsis" />
        </span>
    </div>
</template>
<script>
    import Logo from '@/components/Logo'
    import { getMaterialIconByType } from '@/utils/util'
    export default {
        emits: ['mouseEnter'],
        components: {
            Logo
        },
        props: {
            isWebhook: Boolean,
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
                    webhookSourceTarget: 'branch'
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
                    default:
                        return [
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
            includeLink (field) {
                return [
                    'newCommitId',
                    // 'reviewId',
                    'issueIid',
                    'noteId',
                    'mrIid',
                    'tagName'
                    // 'webhookCommitId'
                ].includes(field) && !this.isSVN
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
                // const webHookRepo = this.material?.webhookRepoUrl?.replace?.(/\.git$/, '') ?? ''
                switch (field) {
                    case 'newCommitId':
                        return this.material?.url ?? ''
                    // case 'reviewId':
                    //     return `${webHookRepo}/reviews/${this.material[field]}` ?? ''
                    // case 'issueIid':
                    //     return `${webHookRepo}/issues/${this.material[field]}` ?? ''
                    // case 'noteId':
                    //     return `${webHookRepo}/merge_requests/${this.material.mrIid}/comments#note_${this.material[field]}` ?? ''
                    // case 'mrIid':
                    //     return this.material?.mrUrl ?? `${webHookRepo}/merge_requests/${this.material[field]}` ?? ''
                    // case 'tagName':
                    //     return `${webHookRepo}/-/tags/${this.material[field]}` ?? ''
                    // case 'webhookCommitId':
                    //     return `${webHookRepo}/commit/${this.material[field]}` ?? ''
                    default:
                        return ''
                }
            }
        }
    }
</script>
