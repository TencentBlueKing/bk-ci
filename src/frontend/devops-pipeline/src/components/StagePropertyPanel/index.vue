<template>
    <bk-sideslider class="bkci-property-panel" width="640" :is-show.sync="visible" :quick-close="true">
        <header class="stage-panel-header" slot="header">
            {{ stageTitle }}
        </header>
        <section v-if="stage" slot="content" :class="{ 'readonly': !editable }" class="stage-property-panel bk-form bk-form-vertical">
            <form-field :required="true" :label="$t('name')" :is-error="errors.has('name')" :error-msg="errors.first('name')">
                <div class="stage-name">
                    <vuex-input :disabled="!editable" input-type="text" :placeholder="$t('nameInputTips')" name="name" v-validate.initial="'required'" :value="stageTitle" :handle-change="handleStageChange" />
                </div>
            </form-field>
            <form-field :required="true" :label="$t('label')" :is-error="errors.has('tag')" :error-msg="errors.first('tag')">
                <div class="stage-tag">
                    <bk-select v-model="stageTag" v-validate.initial="'required'" name="tag" :disabled="!editable" multiple searchable>
                        <bk-option v-for="tag in stageTagList"
                            :key="tag.id"
                            :id="tag.id"
                            :name="tag.stageTagName">
                        </bk-option>
                    </bk-select>
                </div>
            </form-field>
            <stage-control ref="stageControl" :stage-control="stageControl" :disabled="!editable" :handle-stage-change="handleStageChange"></stage-control>
        </section>
    </bk-sideslider>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import Vue from 'vue'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import StageControl from './StageControl'
    export default {
        name: 'container-property-panel',
        components: {
            VuexInput,
            FormField,
            StageControl
        },
        props: {
            stageIndex: Number,
            stage: Object,
            editable: Boolean
        },
        computed: {
            ...mapState('atom', [
                'isPropertyPanelVisible',
                'stageTagList'
            ]),
            visible: {
                get () {
                    return this.isPropertyPanelVisible
                },
                set (value) {
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            },
            stageTag: {
                get () {
                    return this.stage.tag
                },
                set (tag) {
                    this.handleStageChange('tag', tag)
                }
            },
            stageTitle () {
                return typeof this.stage !== 'undefined' ? this.stage.name : 'stage'
            },
            stageControl () {
                if (this.stage && this.stage.stageControlOption) {
                    return {
                        ...this.stage.stageControlOption,
                        fastKill: this.stage.fastKill
                    }
                }
                return undefined
            }
        },
        watch: {
            errors: {
                deep: true,
                handler: function (errors, old) {
                    const validStageControl = !this.$refs.stageControl || (this.$refs.stageControl && this.$refs.stageControl.validateStageControl())
                    const isError = errors.any() || !validStageControl
                    this.handleStageChange('isError', isError)
                }
            }
        },
        methods: {
            ...mapActions('atom', [
                'updateStage',
                'togglePropertyPanel'
            ]),
            handleStageChange (name, value) {
                if (!this.stage.hasOwnProperty(name)) {
                    Vue.set(this.stage, name, value)
                }
                this.updateStage({
                    stage: this.stage,
                    newParam: {
                        [name]: value
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '../AtomPropertyPanel/propertyPanel';
    .bkci-property-panel {
        font-size: 14px;
    }
</style>
