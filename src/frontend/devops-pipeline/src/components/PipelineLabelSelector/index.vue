<template>
    <section class="pipeline-label-selector-wrapper">
        <header class="header-wrapper">
            <div>{{ $t('label') }}</div>
            <bk-button
                text
                size="small"
                @click="handleGoPipelinesGroup"
            >
                {{ $t('labelManage') }}
            </bk-button>
        </header>
        <div class="content-wrapper">
            <bk-select
                :class="[
                    'tag-group-selector',
                    {
                        'disabled': disabled
                    }
                ]"
                v-model="labelGroupValues"
                multiple
                searchable
                @toggle="handleToggleSelectTagGroup"
            >
                <div slot="trigger">
                    <div
                        :class="[
                            'add-label-group',
                            {
                                'disabled': disabled
                            }
                        ]"
                    >
                        <i class="bk-icon left-icon icon-devops-icon icon-plus" />
                        <span>{{ $t('addLabels') }}</span>
                    </div>
                </div>
                <bk-option
                    v-for="option in tagGroupList"
                    :key="option.id"
                    :id="option.id"
                    :name="option.name"
                >
                </bk-option>
            </bk-select>
            <ul class="tag-selection-list">
                <template v-if="tagGroupSelection.length">
                    <li
                        class="tag-item"
                        v-for="(group, groupIndex) in tagGroupSelection"
                        :key="group.id"
                    >
                        <span
                            class="tag-name"
                            v-bk-overflow-tips
                        >
                            {{ group.name }}
                        </span>
                        <bk-select
                            :value="labelMap[group.id]"
                            class="label-selector"
                            :disabled="disabled"
                            :key="groupIndex"
                            multiple
                            @change="group.handleChange"
                        >
                            <bk-option
                                v-for="label in group.labels"
                                :key="label.id"
                                :id="label.id"
                                :name="label.name"
                            >
                            </bk-option>
                        </bk-select>
                        <i
                            :class="[
                                'bk-icon icon-minus-circle-shape minus-circle-icon',
                                {
                                    'disabled': disabled
                                }
                            ]"
                            @click="handleMinusTagGroup(group.id)"
                        />
                    </li>
                </template>
            </ul>
        </div>
    </section>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'PipelineLabelSelector',
        props: {
            value: {
                type: Array,
                default: () => []
            },
            disabled: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                labelGroupValues: [],
                labels: [],
                labelMap: {},
                labelSet: new Set(this.value),
                isUpdated: true
            }
        },
        computed: {
            ...mapState('pipelines', [
                'tagGroupList'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            tagGroupSelection () {
                return this.tagGroupList?.filter(item => this.labelGroupValues.includes(item.id)).map(i => {
                    return {
                        id: i.id,
                        name: i.name,
                        labels: i.labels,
                        handleChange: (...args) => this.handleChangeLabel(i.id, ...args)
                    }
                })
            }
        },
        watch: {
            value: {
                async handler (val) {
                    if (!Array.isArray(val)) return
                    if (val.length) await this.requestTagList(this.$route.params)
                    this.labelSet = new Set(val)
                    if (this.isUpdated) {
                        await this.updateValues()
                    }
                    this.isUpdated = true
                },
                immediate: true
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'requestTagList'
            ]),
            /**
             * value(选中标签), tagGroupList(所有分组标签)
             * id(组id), labels(组对应选中的值)
             * 根据传入的value，进行分组
             * 返回结果为 { id: labels }
             */
            updateValues () {
                this.labelMap = this.tagGroupList.reduce((acc, group) => {
                    const labels = group.labels
                        .filter(label => this.value.includes(label.id))
                        .map(label => label.id)

                    if (labels.length > 0) {
                        acc[group.id] = labels
                    }
                    return acc
                }, {})
                this.labelGroupValues = Object.keys(this.labelMap)
            },
            /**
             * 跳转到标签管理
             */
            handleGoPipelinesGroup () {
                window.open(`${window.location.origin}/console/pipeline/${this.projectId}/group`, '_blank')
            },
            handleToggleSelectTagGroup (val) {
                if (val && !this.tagGroupList.length) {
                    this.requestTagList(this.$route.params)
                }
            },
            /**
             * 删除标签组
             * 更新选中标签数据
             * @param index 下标
             */
            handleMinusTagGroup (id) {
                const index = this.labelGroupValues.findIndex(group => group === id)
                this.labelGroupValues.splice(index, 1)
                this.handleChangeLabel(id, [])
            },
            /**
             * 选择标签组下的标签
             * @param list 选中的标签
             */
            handleChangeLabel (groupId, labelIds) {
                if (labelIds.length === 0) {
                    this.labelMap[groupId]?.forEach(id => {
                        this.labelSet.delete(id)
                    })
                } else {
                    this.labelSet = new Set([
                        ...this.labelSet,
                        ...labelIds
                    ])
                }
                this.labelMap[groupId] = labelIds
                this.isUpdated = false
                this.$emit('update:value', Array.from(this.labelSet), this.labelMap)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .pipeline-label-selector-wrapper {
        font-size: 12px;
        .header-wrapper {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 12px;
        }
    }
    .content-wrapper {
        background: #F5F7FA;
        padding: 16px 0;
    }
    .tag-group-selector {
        margin: 0 16px;
        &:hover {
            border-color: #3a84ff;
        }
        &.disabled {
            border-color: #C4C6CC !important;
        }
    }
    .add-label-group {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 32px;
        line-height: 32px;
        background: #fff;
        border-radius: 4px;
        cursor: pointer;
        &:hover {
            color: #3a84ff;
            border-color: #3a84ff;
        }
        &.disabled {
            color: #c4c6cc !important;
            border-color: #C4C6CC !important;
            cursor: not-allowed;
        }
        
        .icon-plus {
            font-size: 22px;
        }
    }
    .tag-selection-list {
        max-height: 200px;
        overflow: auto;
        padding: 0 16px;
        &::-webkit-scrollbar {
            width: 5px;
        }
        &::-webkit-scrollbar {
            background: #F5F7FA;
        }
        &::-webkit-scrollbar-thumb {
            border-radius: 13px;
            background-color: #d4dae3;
        }
        .tag-item {
            display: flex;
            align-items: center;
            margin-top: 10px;
        }
        .tag-name {
            width: 70px;
            flex-shrink: 0;
            text-align: right;
            text-overflow: ellipsis;
            overflow: hidden;
        }
        .label-selector {
            width: 198px;
            margin: 0 10px;
            flex-shrink: 0;
        }
        .minus-circle-icon {
            flex-shrink: 0;
            font-size: 14px;
            color: #C4C6CC;
            cursor: pointer;
            &.disabled {
                cursor: not-allowed;
            }
        }
    }
</style>
