<template>
    <div
        v-if="pipelineSetting"
        class="bkdevops-base-info-setting-tab"
    >
        <div class="pipeline-setting-title">{{ $t('settings.baseInfo') }}</div>
        <bk-form
            form-type="vertical"
            :label-width="300"
            class="new-ui-form"
        >
            <bk-form-item
                :label="$t('pipelineName')"
                :required="true"
            >
                <vuex-input
                    v-bk-focus
                    :disabled="!editable"
                    :placeholder="$t('pipelineNameInputTips')"
                    name="pipelineName"
                    :value="pipelineSetting.pipelineName"
                    v-validate.initial="'required|max:128'"
                    :max-length="128"
                    :handle-change="handleBaseInfoChange"
                />
            </bk-form-item>

            <bk-form-item :required="false">
                <div class="layout-label">
                    <label class="ui-inner-label">
                        <span class="bk-label-text">{{ $t('settings.label') }} </span>
                    </label>
                    <label
                        v-if="editable"
                        class="ui-inner-label"
                    >
                        <span
                            @click="toManageLabel"
                            class="bk-label-text link-text"
                        >{{ $t('settings.manageLabel') }}</span>
                    </label>
                </div>
                <ul class="pipeline-label-selector">
                    <template v-if="tagGroupList.length > 0">
                        <li
                            v-for="(item, index) in tagGroupList"
                            :key="item.id"
                        >
                            <label
                                :title="item.name"
                                class="pipeline-selector-label"
                            > {{ item.name }} </label>
                            <bk-select
                                class="sub-label-select"
                                :disabled="!editable"
                                :value="labelValues[index]"
                                @selected="handleLabelSelect(index, arguments)"
                                @clear="handleLabelSelect(index, [[]])"
                                multiple
                            >
                                <bk-option
                                    v-for="label in item.labels"
                                    :key="label.id"
                                    :id="label.id"
                                    :name="label.name"
                                >
                                </bk-option>
                            </bk-select>
                        </li>
                    </template>
                    <span
                        class="no-label-placeholder"
                        v-else
                    >
                        {{ $t('noLabels') }}
                    </span>
                </ul>
            </bk-form-item>

            <bk-form-item
                :label="$t('desc')"
                :is-error="errors.has('desc')"
                :error-msg="errors.first('desc')"
            >
                <vuex-textarea
                    :disabled="!editable"
                    name="desc"
                    :value="pipelineSetting.desc"
                    :maxlength="100"
                    :placeholder="$t('pipelineDescInputTips')"
                    v-validate.initial="'max:100'"
                    :handle-change="handleBaseInfoChange"
                />
            </bk-form-item>

            <bk-form-item ext-cls="namingConvention">
                <syntax-style-configuration
                    :inherited-dialect="settings.inheritedDialect"
                    :pipeline-dialect="settings.pipelineDialect ?? defaultPipelineDialect"
                    @inherited-change="inheritedChange"
                    @pipeline-dialect-change="pipelineDialectChange"
                />
            </bk-form-item>
        </bk-form>
    </div>
</template>

<script>
    import VuexInput from '@/components/atomFormField/VuexInput/index.vue'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea/index.vue'
    import SyntaxStyleConfiguration from '@/components/syntaxStyleConfiguration'
    import { mapGetters } from 'vuex'

    export default {
        name: 'bkdevops-base-info-setting-tab',
        components: {
            VuexTextarea,
            VuexInput,
            SyntaxStyleConfiguration
        },
        props: {
            pipelineSetting: Object,
            editable: {
                type: Boolean,
                default: true
            },
            handleBaseInfoChange: Function
        },
        data () {
            return {
                settings: {}
            }
        },
        computed: {
            ...mapGetters({
                tagGroupList: 'pipelines/getTagGroupList'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            labelValues () {
                const labels = this.pipelineSetting.labels || []
                return this.tagGroupList.map((tag) => {
                    const currentLables = tag.labels || []
                    const value = []
                    currentLables.forEach((label) => {
                        const index = labels.findIndex((item) => (item === label.id))
                        if (index > -1) value.push(label.id)
                    })
                    return value
                })
            },
            curProject () {
                return this.$store.state.curProject
            },
            defaultPipelineDialect () {
                return this.curProject?.properties?.pipelineDialect
            }
        },
        watch: {
            'pipelineSetting.pipelineAsCodeSettings': {
                handler (val) {
                    if (val) {
                        const { inheritedDialect, pipelineDialect, projectDialect } = this.pipelineSetting.pipelineAsCodeSettings
                        this.settings = {
                            ...this.pipelineSetting.pipelineAsCodeSettings,
                            pipelineDialect: inheritedDialect ? projectDialect : pipelineDialect
                        }
                    }
                },
                immediate: true
            }
        },
        created () {
            this.requestGrouptLists()
        },
        methods: {
            /** *
             * 获取标签及其分组
             */
            async requestGrouptLists () {
                try {
                    const res = await this.$store.dispatch('pipelines/requestTagList', {
                        projectId: this.projectId
                    })

                    this.$store.commit('pipelines/updateGroupLists', res)
                    // 获取当前项目语法风格
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            handleLabelSelect (index, arg) {
                let labels = []
                this.labelValues.forEach((value, valueIndex) => {
                    if (valueIndex === index) labels = labels.concat(arg[0])
                    else labels = labels.concat(value)
                })
                this.handleBaseInfoChange('labels', labels)
            },
            toManageLabel () {
                const url = `${WEB_URL_PREFIX}/pipeline/${this.projectId}/group`
                window.open(url, '_blank')
            },
            inheritedChange (value) {
                this.settings = {
                    ...this.settings,
                    inheritedDialect: value,
                    ...value && { pipelineDialect: this.defaultPipelineDialect }
                }
                this.handleBaseInfoChange('pipelineAsCodeSettings', this.settings)
            },
            pipelineDialectChange (value) {
                this.settings.pipelineDialect = value
                this.handleBaseInfoChange('pipelineAsCodeSettings', this.settings)
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf';
    @import '@/scss/mixins/ellipsis';

    .bkdevops-base-info-setting-tab {
        .pipeline-setting-title {
            font-size: 14px;
            font-weight: bold;
            border-bottom: 1px solid #DCDEE5;
            padding-bottom: 4px;
            margin-bottom: 16px;
        }
        .bk-form-content {
            max-width: 560px;
        }
        .layout-label {
            width: 560px;
            height: 24px;
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            .link-text {
                color: #3A84FF;
                cursor: pointer;
            }
        }
        .pipeline-label-selector {
            border-radius: 2px;
            border: 1px solid #DCDEE5;
            padding: 16px;
            > li {
                display: flex;
                width: 100%;
                overflow: hidden;
                &:not(:last-child) {
                    padding-bottom: 16px;
                }
                .pipeline-selector-label {
                    font-size: 12px;
                    color: #63656E;
                    width: 80px;
                    text-align: right;
                    @include ellipsis();
                    margin-right: 22px;
                }
                .sub-label-select {
                    flex: 1;
                    overflow: hidden;
                }

            }
            .no-label-placeholder {
                display: flex;
                align-items: center;
                justify-content: center;
                color: #979BA5;
                font-size: 12px;
            }
        }
        .namingConvention {
            position: relative;
            .bk-form-control {
                display: flex;
                grid-gap: 16px;
                margin-top: 8px;
            }
        }
    }
</style>
