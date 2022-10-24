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
        </template>
        <ul class="operate-pipeline-list">
            <li v-for="(pipeline, index) in removedPipelines" :key="pipeline.pipelineId">
                <span>{{ pipeline.name }}</span>
                <div v-if="!isRemoveType" class="belongs-pipeline-group" ref="belongsGroupBox">
                    <bk-tag ext-cls="pipeline-group-name-tag" v-for="name in pipeline.groups" :key="name" :ref="`groupName_${index}`">
                        {{name}}
                    </bk-tag>
                    <bk-popover ref="groupNameMore" v-if="pipeline.showMoreTag" :disabled="!pipeline.hiddenGroups" :content="pipeline.hiddenGroups">
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
            removedPipelines () {
                return this.pipelineList.map((pipeline, index) => {
                    const viewNames = pipeline.viewNames ?? []
                    const visibleTagCount = this.visibleTagCountList[index] ?? viewNames.length
                    const overflowCount = viewNames.length - visibleTagCount

                    return {
                        name: pipeline.pipelineName,
                        groups: viewNames.slice(0, visibleTagCount),
                        hiddenGroups: viewNames.slice(visibleTagCount).join(';'),
                        overflowCount,
                        showMoreTag: this.visibleTagCountList[index] === undefined || (overflowCount > 0)
                    }
                })
            }
        },
        updated () {
            setTimeout(() => {
                if (this.visibleTagCountList.length === 0 && this.pipelineList.length > 0) {
                    this.calcOverPos()
                }
            }, 100)
        },
        methods: {
            ...mapActions('pipelines', [
                'removePipelineFromGroup',
                'patchDeletePipelines'
            ]),
            async handleSubmit () {
                if (this.isBusy) return

                try {
                    this.isBusy = true
                    const params = {
                        projectId: this.$route.params.projectId,
                        pipelineIds: this.pipelineList.map(pipeline => pipeline.pipelineId)

                    }
                    if (this.isRemoveType) {
                        await this.removePipelineFromGroup({
                            ...params,
                            viewId: this.groupId
                        })

                        this.$store.commit('pipelines/UPDATE_PIPELINE_GROUP', {
                            id: this.groupId,
                            body: {
                                pipelineCount: this.groupMap[this.groupId].pipelineCount - (this.pipelineList.length ?? 0)
                            }
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
                this.visibleTagCountList = []
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

                        this.$refs[`groupName_${index}`]?.every((groupName, index) => {
                            sumTagWidth += groupName.$el.offsetWidth + tagMargin
                            const isOverSize = sumTagWidth > viewPortWidth
                            !isOverSize && tagVisbleCount++
                            return isOverSize
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
        }
        .remove-confirm-desc {
            font-size: 14px;
            text-align: left
        }
        .operate-pipeline-list {
            border: 1px solid #DCDEE5;
            border-radius: 2px;
            margin-top: 16px;
            padding: 0 16px;
            overflow: auto;
            > li {
                height: 40px;
                display: flex;
                align-items: center;
                overflow: hidden;
                text-align: left;
                > span {
                    flex: 1;
                    @include ellipsis();
                }
                .belongs-pipeline-group {
                    display: flex;
                    width: 200px;
                    height: 22px;
                    overflow: hidden;
                }
            }
        }
    }
</style>
