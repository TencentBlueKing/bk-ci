<template>
    <bk-dialog
        ext-cls="remove-pipeline-confirm-dialog"
        width="480"
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
            <li v-for="pipeline in pipelineList" :key="pipeline.pipelineId">
                <span>{{ pipeline.pipelineName }}</span>
                <div v-if="!isRemoveType" class="belongs-pipeline-group">
                    <bk-tag v-for="name in pipeline.viewNames" :key="name">
                        {{name}}
                    </bk-tag>
                </div>
            </li>
        </ul>
    </bk-dialog>
</template>

<script>
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import { mapState } from 'vuex'
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
            pipelineList: {
                type: Array,
                default: () => []
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
            }
        },

        methods: {
            async handleSubmit () {
                if (this.isRemoveType) {
                    //
                } else {
                    await Promise.all(this.pipelineList.map(pipeline => this.delete(pipeline)))
                }
                this.$emit('submit')
            },
            handleClose () {
                this.$emit('close')
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
                > span {
                    flex: 1;
                    @include ellipsis();
                }
                .belongs-pipeline-group {
                    flex: 3;
                }
            }
        }
    }
</style>
