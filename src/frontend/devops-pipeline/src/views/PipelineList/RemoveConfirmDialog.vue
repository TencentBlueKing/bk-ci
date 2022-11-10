<template>
    <bk-dialog
        ext-cls="remove-pipeline-confirm-dialog"
        :width="width"
        :value="isShow"
        :title="title"
        header-position="left"
        :draggable="false"
        :loading="isBusy"
        @cancel="handleClose"
        @confirm="handleSubmit"
    >
        <p class="remove-confirm-desc" v-if="isRemoveType" v-html="$t('removeConfirmTips', [groupName])">
        </p>
        <template v-else-if="isDeleteType">
            <span class="delete-pipeline-warning-icon">
                <i class="devops-icon icon-exclamation" />
            </span>
            <h2 class="remove-confirm-title">
                {{ $t('deletePipelineConfirm') }}
            </h2>
            <p class="remove-confirm-desc">
                {{$t('deletePipelineConfirmDesc')}}
            </p>
            <bk-alert
                v-if="noPermissionPipelineLength > 0"
                type="warning"
                class="no-permission-pipeline-alert"
                :title="$t('hasNoPermissionPipelineTips', [noPermissionPipelineLength])"
                closable
                :close-text="$t('removeNoPermissionPipeline')"
                @close="removeNoPermissionPipeline"
            >
            </bk-alert>
        </template>

        <ul v-if="removedPipelines.length" class="operate-pipeline-list">
            <li
                v-for="(pipeline, index) in removedPipelines"
                :key="pipeline.pipelineId"
                :class="{
                    'no-permission-pipeline': !pipeline.hasPermission
                }"
            >
                <span>{{ pipeline.name }}</span>
                <div v-if="!isRemoveType" class="belongs-pipeline-group" ref="belongsGroupBox">
                    <bk-tag

                        ext-cls="pipeline-group-name-tag"
                        v-for="name in pipeline.groups"
                        :key="name"
                        :ref="`groupName_${index}`"
                    >
                        {{name}}
                    </bk-tag>
                    <bk-popover
                        v-if="pipeline.showMoreTag"
                        ref="groupNameMore"
                        class="pipeline-group-name-tag"
                        :content="pipeline.hiddenGroups"
                    >
                        <bk-tag>
                            +{{pipeline.overflowCount}}
                        </bk-tag>
                    </bk-popover>
                </div>
            </li>
        </ul>
    </bk-dialog>
</template>

<script>
    import { mapState, mapActions, mapGetters } from 'vuex'
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import {
        UNCLASSIFIED_PIPELINE_VIEW_ID
    } from '@/store/constants'
    export default {
        mixins: [piplineActionMixin],
        props: {
            isShow: Boolean,
            type: {
                type: String,
                default: 'remove'
            },
            groupName: {
                type: String,
                required: true
            },
            groupId: {
                type: String,
                required: true
            },
            pipelineList: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                hideNoPermissionPipeline: false,
                visibleTagCountList: [],
                isBusy: false,
                width: 480,
                padding: 40
            }
        },
        computed: {
            ...mapState('pipelines', [
                'allPipelineGroup'
            ]),
            ...mapGetters('pipelines', [
                'groupMap'
            ]),
            isRemoveType () {
                return this.type === 'remove'
            },
            isDeleteType () {
                return this.type === 'delete'
            },
            title () {
                return this.isRemoveType ? this.$t('removeFrom') : ''
            },
            hasPermissionPipelines () {
                return this.pipelineList.filter(pipeline => pipeline.hasPermission)
            },
            noPermissionPipelineLength () {
                return this.pipelineList.length - this.hasPermissionPipelines.length
            },
            removedPipelines () {
                const list = this.hideNoPermissionPipeline ? this.hasPermissionPipelines : this.pipelineList
                return list.map((pipeline, index) => {
                    const viewNames = pipeline.viewNames ?? []
                    const visibleTagCount = this.visibleTagCountList[index] ?? viewNames.length
                    const overflowCount = viewNames.length - visibleTagCount

                    return {
                        name: pipeline.pipelineName,
                        hasPermission: pipeline.hasPermission,
                        groups: viewNames.slice(0, visibleTagCount),
                        hiddenGroups: viewNames.slice(visibleTagCount).join(';'),
                        overflowCount,
                        showMoreTag: this.visibleTagCountList[index] === undefined || (overflowCount > 0)
                    }
                })
            }
        },
        watch: {
            isShow (val) {
                if (!val) {
                    this.visibleTagCountList = []
                } else {
                    setTimeout(() => {
                        if (this.visibleTagCountList.length === 0 && this.pipelineList.length > 0) {
                            this.calcOverPos()
                        }
                    }, 200)
                }
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'removePipelineFromGroup',
                'patchDeletePipelines'
            ]),
            removeNoPermissionPipeline () {
                this.hideNoPermissionPipeline = true
            },
            async handleSubmit () {
                if (this.isBusy) return

                try {
                    this.isBusy = true
                    const list = this.isRemoveType ? this.pipelineList : this.hasPermissionPipelines
                    if (list.length === 0) {
                        throw Error('noDeletePipelines')
                    }
                    const params = {
                        projectId: this.$route.params.projectId,
                        pipelineIds: list.map(pipeline => pipeline.pipelineId)
                    }

                    if (this.isRemoveType) {
                        await this.removePipelineFromGroup({
                            ...params,
                            viewId: this.groupId
                        })
                        const res = [
                            {
                                id: UNCLASSIFIED_PIPELINE_VIEW_ID,
                                num: list.filter(pipeline => pipeline.viewNames.length === 1).length
                            },
                            {
                                id: this.groupId,
                                num: -list.length
                            }
                        ]
                        res.forEach(item => {
                            console.log(item)
                            this.$store.commit('pipelines/UPDATE_PIPELINE_GROUP', {
                                id: item.id,
                                body: {
                                    pipelineCount: this.groupMap[item.id].pipelineCount + item.num
                                }
                            })
                        })
                    } else {
                        await this.patchDeletePipelines(params)
                    }
                    this.$showTips({
                        message: this.$t(this.isRemoveType ? 'removeSuc' : 'deleteSuc'),
                        theme: 'success'
                    })
                    this.handleClose()
                    this.$emit('done')
                } catch (e) {
                    this.$showTips({
                        message: e.message ?? e,
                        theme: 'error'
                    })
                } finally {
                    this.isBusy = false
                }
            },
            handleClose () {
                this.hideNoPermissionPipeline = false
                this.$emit('close')
            },
            calcOverPos () {
                const tagMargin = 6
                const groupNameBoxWidth = 200
                if (this.$refs.belongsGroupBox?.length > 0) {
                    this.visibleTagCountList = this.$refs.belongsGroupBox?.map((_, index) => {
                        const groupNameLength = this.$refs[`groupName_${index}`]?.length ?? 0
                        const moreTag = this.$refs.groupNameMore?.[index]?.$el
                        const moreTagWidth = (moreTag?.clientWidth ?? 0) + tagMargin
                        const viewPortWidth = groupNameBoxWidth - (groupNameLength > 1 ? moreTagWidth : 0)
                        let sumTagWidth = 0
                        let tagVisbleCount = 0

                        console.log(viewPortWidth, moreTagWidth)

                        this.$refs[`groupName_${index}`]?.every((groupName) => {
                            sumTagWidth += groupName.$el.offsetWidth + tagMargin
                            console.log(index, sumTagWidth, groupName.$el.offsetWidth)
                            const isOverSize = sumTagWidth > viewPortWidth
                            !isOverSize && tagVisbleCount++
                            return !isOverSize
                        })
                        return tagVisbleCount
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    @import '@/scss/conf';
    .remove-pipeline-confirm-dialog {
        text-align: center;
        .delete-pipeline-warning-icon {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background-color: #FFE8C3;
            color: #FF9C01;
            width: 42px;
            height: 42px;
            font-size: 24px;
            border-radius: 50%;
        }
        .danger-text {
            color: $dangerColor;
        }
        .remove-confirm-title {
            font-size: 20px;
            color: #313238;
            margin: 8px 0;
        }
        .remove-confirm-desc {
            font-size: 14px;
            margin-bottom: 25px;
        }
        .no-permission-pipeline-alert {
            text-align: left;
        }
        .operate-pipeline-list {
            border: 1px solid #DCDEE5;
            border-radius: 2px;
            margin-top: 16px;
            overflow: auto;
            > li {
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
                &.no-permission-pipeline {
                    background: #FFF3E1;
                }
                > span {
                    flex: 1;
                    @include ellipsis();
                }
                .belongs-pipeline-group {
                    vertical-align: top;
                    width: 200px;
                    height: 22px;
                    overflow: hidden;
                }
            }
        }
    }
</style>
