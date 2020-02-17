<template>
    <bk-sideslider class="bkci-property-panel" width="640" :is-show.sync="visible" :quick-close="true">
        <header class="stage-panel-header" slot="header">
            {{ stageTitle }}
        </header>
        <section v-if="stage" slot="content" :class="{ 'readonly': !editable }" class="stage-property-panel bk-form bk-form-vertical">
            <form-field :required="true" :label="$t('name')" :is-error="errors.has('name')" :error-msg="errors.first('name')">
                <div class="stage-name">
                    <vuex-input :disabled="!editable" input-type="text" :placeholder="$t('nameInputTips')" name="name" v-validate.initial="'required'" :value="stage.name" :handle-change="handleStageChange" />
                </div>
            </form-field>
            <form-field :required="true" :label="$t('label')" :is-error="errors.has('label')" :error-msg="errors.first('label')">
                <div class="stage-label">
                    <vuex-input :disabled="!editable" input-type="text" :placeholder="$t('nameInputTips')" name="label" :value="stage.label" :handle-change="handleStageChange" />
                </div>
            </form-field>
            <stage-control :stage-control="stage.stageControl" :handle-stage-change="handleStageChange"></stage-control>
        </section>
    </bk-sideslider>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
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
                'isPropertyPanelVisible'
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
            stageTitle () {
                return typeof this.stageIndex !== 'undefined' ? this.stage.name : this.$t('propertyBar')
            }
        },
        watch: {
            errors: {
                deep: true,
                handler: function (errors, old) {
                    const isError = errors.any()
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
