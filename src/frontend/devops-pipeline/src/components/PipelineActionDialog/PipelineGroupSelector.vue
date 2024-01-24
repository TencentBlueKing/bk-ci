<template>
    <bk-form form-type="vertical">
        <bk-form-item>
            <label v-if="dynamicGroupEditable" class="label-selector-label">
                <span>{{ $t('label') }}</span>
                <span class="pipeline-label-action-span">
                    <router-link target="_blank" :to="addLabelRoute" class="pipeline-label-action-span-btn">
                        <logo name="plus" size="20" />
                        {{$t('addLabel')}}
                    </router-link>
                    <span @click="refreshLabel" class="pipeline-label-action-span-btn">
                        <logo name="refresh" size="16" />
                        {{$t('editPage.atomForm.reflash')}}
                    </span>
                </span>
            </label>
            <PipelineLabelSelector
                ref="labelSelector"
                v-model="labels"
                :editable="dynamicGroupEditable"
                @change="updateDynamicGroup"
            />
        </bk-form-item>

        <bk-form-item label-width="auto" :label="$t('dynamicPipelineGroup')">
            <bk-select
                disabled
                multiple
                :value="dynamicGroup"
                :loading="isMatching"
                :placeholder="$t('pipelineDynamicMatchPlaceholder')"

            >
                <bk-option
                    v-for="group in dynamicPipelineGroups"
                    :key="group.id"
                    :id="group.id"
                    :name="group.name"
                >
                </bk-option>
            </bk-select>
        </bk-form-item>
        <bk-form-item label-width="auto" :label="$t('staticPipelineGroup')">
            <bk-select
                multiple
                :disabled="!staticGroupEditable"
                v-model="staticViews"
                @change="emitChange"
                :popover-options="{
                    appendTo: 'parent'
                }"
            >
                <bk-option-group
                    v-for="(group, index) in visibleStaticGroups"
                    :name="group.name"
                    :key="index">
                    <bk-option v-for="option in group.children"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-option-group>
            </bk-select>
        </bk-form-item>
    </bk-form>
</template>

<script>
    import Logo from '@/components/Logo'
    import PipelineLabelSelector from '@/components/PipelineActionDialog/PipelineLabelSelector'
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import { mapActions, mapGetters, mapState } from 'vuex'

    export default {
        components: {
            PipelineLabelSelector,
            Logo
        },
        mixins: [piplineActionMixin],
        props: {
            dynamicGroupEditable: {
                type: Boolean,
                default: true
            },
            pipelineName: {
                type: String,
                required: true
            },
            value: {
                type: Object,
                requied: true
            },
            hasManagePermission: {
                type: Boolean,
                default: false
            },
            staticGroupEditable: {
                type: Boolean,
                default: true
            },
            staticGroups: {
                type: Array
            }
        },
        data () {
            return {
                initTags: {},
                dynamicGroup: [],
                isMatching: false,
                labels: this.value?.labels ?? [],
                staticViews: this.value?.staticViews ?? []
            }
        },
        computed: {
            ...mapState('pipelines', [
                'allPipelineGroup'
            ]),
            ...mapGetters('pipelines', [
                'staticPipelineGroups',
                'dynamicPipelineGroups'
            ]),
            addLabelRoute () {
                return {
                    name: 'pipelinesGroup'
                }
            },
            visibleStaticGroups () {
                const staticGroups = [
                    {
                        name: this.$t('personalViewList'),
                        children: []
                    }
                ]
                if (this.hasManagePermission) {
                    staticGroups.push({
                        name: this.$t('projectViewList'),
                        children: []
                    })
                }

                return (this.staticGroups ?? this.staticPipelineGroups).reduce((acc, group) => {
                    const pos = group.projected ? 1 : 0
                    if (acc[pos]) {
                        acc[pos].children.push(group)
                    }
                    return acc
                }, staticGroups)
            }
        },
        watch: {
            value (val) {
                this.labels = val?.labels ?? []
                this.staticViews = val?.staticViews ?? []
            },
            '$route.params.projectId': function () {
                this.reset()
                this.refreshLabel()
            },
            pipelineName () {
                this.$nextTick(() => {
                    this.updateDynamicGroup(this.labels, this.initTags)
                })
            }
        },
        created () {
            if (this.allPipelineGroup.length === 0 && !Array.isArray(this.staticGroups)) {
                this.requestGetGroupLists(this.$route.params)
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'matchDynamicView',
                'requestGetGroupLists'
            ]),
            refreshLabel () {
                this.$refs?.labelSelector.init?.()
            },
            async updateDynamicGroup (labels, tags) {
                if (this.isMatching || !this.pipelineName) return
                this.isMatching = true
                this.labels = labels
                this.initTags = tags
                try {
                    const { data } = await this.matchDynamicView({
                        projectId: this.$route.params.projectId,
                        pipelineName: this.pipelineName,
                        labels: Object.keys(tags).map(key => ({
                            groupId: key,
                            labelIds: tags[key]
                        }))
                    })
                    this.dynamicGroup = data
                } catch (e) {
                    console.error(e)
                } finally {
                    this.isMatching = false
                    this.emitChange()
                }
            },
            reset () {
                this.initTags = {}
                this.dynamicGroup = []
                this.isMatching = false
                this.labels = []
                this.staticViews = []
                this.emitChange()
            },
            emitChange () {
                this.$emit('input', {
                    labels: this.labels,
                    staticViews: this.staticViews
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf';
    .label-selector-label {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 12px;
        .pipeline-label-action-span {
            display: grid;
            grid-gap: 20px;
            grid-template-columns: repeat(2, auto);
            .pipeline-label-action-span-btn {
                color: $primaryColor;
                position: relative;
                display: flex;
                align-items: center;
                cursor: pointer;
                >:first-child {
                    margin-right: 6px;
                }
                &:first-child::before {
                    content: '|';
                    position: absolute;
                    right: -13px;
                    color: #DCDEE5;
                }
            }
        }
    }
</style>
