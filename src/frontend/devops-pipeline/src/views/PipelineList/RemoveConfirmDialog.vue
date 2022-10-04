<template>
    <bk-dialog
        ext-cls="remove-pipeline-confirm-dialog"
        :width="width"
        :value="isShow"
        :title="title"
        header-position="left"
        :draggable="false"
        @cancel="handleClose"
        @confirm="handleSubmit"
    >
        <p class="remove-confirm-desc" v-if="isRemoveType">
            {{$t('removeConfirmTips', [groupName])}}
        </p>
        <template v-else>
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
                    <bk-tag v-for="name in pipeline.groups" :key="name" :ref="`groupName_${index}`">
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
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import { mapState, mapActions } from 'vuex'
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
                width: 480,
                padding: 40
            }
        },
        computed: {
            ...mapState('pipelines', [
                'allPipelineGroup'
            ]),
            isRemoveType () {
                return this.type === 'remove'
            },
            title () {
                return this.isRemoveType ? this.$t('removeFrom') : ''
            },
            groupNameBoxWidth () {
                return ((this.width - 2 - (this.padding * 2)) * 5 / 7).toFixed(2)
            },
            removedPipelines () {
                return this.pipelineList.map((pipeline, index) => {
                    const visibleTagCount = this.visibleTagCountList[index] ?? pipeline.viewNames.length
                    const overflowCount = pipeline.viewNames.length - visibleTagCount
                    return {
                        name: pipeline.pipelineName,
                        groups: pipeline.viewNames.slice(0, visibleTagCount),
                        hiddenGroups: pipeline.viewNames.slice(visibleTagCount).join(';'),
                        overflowCount,
                        showMoreTag: this.visibleTagCountList[index] === undefined || (overflowCount > 0)
                    }
                })
            }
        },
        updated () {
            setTimeout(() => {
                if (this.visibleTagCountList.length === 0) {
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
                const params = {
                    projectId: this.$route.params.projectId,
                    pipelineIds: this.pipelineList.map(pipeline => pipeline.pipelineId)

                }
                if (this.isRemoveType) {
                    await this.removePipelineFromGroup({
                        ...params,
                        viewId: this.groupId
                    })
                } else {
                    await this.patchDeletePipelines(params)
                }
                this.$showTips({
                    message: this.$t(this.isRemoveType ? 'removeSuc' : 'deleteSuc'),
                    theme: 'success'
                })
                this.$emit('done')
            },
            handleClose () {
                this.visibleTagCountList = []
                this.$emit('close')
            },
            calcOverPos () {
                const tagMargin = 6
                this.visibleTagCountList = this.$refs.belongsGroupBox?.map((_, index) => {
                    const moreTag = this.$refs.groupNameMore?.[index]?.$el
                    const moreTagWidth = (moreTag?.offsetWidth ?? 0) + tagMargin
                    const viewPortWidth = this.groupNameBoxWidth - moreTagWidth
                    let sumTagWidth = 0
                    let tagVisbleCount = 0

                    this.$refs[`groupName_${index}`].every((groupName) => {
                        sumTagWidth += groupName.$el.offsetWidth + tagMargin
                        const isOverSize = sumTagWidth < viewPortWidth
                        isOverSize && tagVisbleCount++
                        return isOverSize
                    })
                    return tagVisbleCount
                })
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
                    flex: 2;
                    @include ellipsis();
                }
                .belongs-pipeline-group {
                    flex: 5;
                    height: 22px;
                    overflow: hidden;
                }
            }
        }
    }
</style>
