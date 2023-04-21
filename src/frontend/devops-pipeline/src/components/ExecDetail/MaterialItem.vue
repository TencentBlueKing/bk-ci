<template>
    <div class="exec-material-row">
        <span v-for="(field) in materialInfos" :key="field">
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
                <span>{{ material.webhookSourceBranch }}</span>
                <i class="devops-icon icon-arrows-right"></i>
                <logo :name="iconArray[field] || 'commit'" size="14" />
                <span>{{ material.webhookBranch }}</span>
            </span>
            <span class="material-span" v-else>{{ formatField(field) || '--' }}</span>
        </span>
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
            iconArray () {
                const scmIcon = getMaterialIconByType(this.material?.scmType)
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
            materialInfos () {
                if (!this.isWebhook) {
                    return [
                        'aliasName',
                        ...(this.material.scmType === 'CODE_SVN' ? [] : ['branchName']),
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
                    case 'SVN':
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
                ].includes(field)
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
                        return this.material[field] ?? '--'
                }
            },
            getLink (field) {
                const webHookRepo = this.material?.webhookRepoUrl?.replace?.(/\.git$/, '') ?? ''
                switch (field) {
                    case 'newCommitId':
                        return this.material?.url ?? ''
                    // case 'reviewId':
                    //     return `${webHookRepo}/reviews/${this.material[field]}` ?? ''
                    case 'issueIid':
                        return `${webHookRepo}/issues/${this.material[field]}` ?? ''
                    case 'noteId':
                        return `${webHookRepo}/merge_requests/${this.material.mrIid}/comments#note_${this.material[field]}` ?? ''
                    case 'mrIid':
                        return this.material?.mrUrl ?? `${webHookRepo}/merge_requests/${this.material[field]}` ?? ''
                    case 'tagName':
                        return `${webHookRepo}/-/tags/${this.material[field]}` ?? ''
                    // case 'webhookCommitId':
                    //     return `${webHookRepo}/commit/${this.material[field]}` ?? ''
                    default:
                        return ''
                }
            }
        }
    }
</script>
