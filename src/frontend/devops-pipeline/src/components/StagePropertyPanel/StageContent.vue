<template>
    <section
        v-if="stage"
        :class="{ 'readonly': !editable }"
        class="stage-property-panel bk-form bk-form-vertical"
    >
        <form-field
            required
            :label="$t('name')"
            :is-error="errors.has('name')"
            :error-msg="errors.first('name')"
        >
            <div class="stage-name">
                <vuex-input
                    :disabled="!editable || isFinallyStage"
                    input-type="text"
                    :placeholder="$t('nameInputTips')"
                    name="name"
                    v-validate.initial="'required'"
                    :value="stageTitle"
                    :handle-change="handleStageChange"
                />
            </div>
        </form-field>
        <form-field
            required
            :label="$t('label')"
            :is-error="errors.has('tag')"
            :error-msg="errors.first('tag')"
        >
            <div class="stage-tag">
                <bk-select
                    v-model="stageTag"
                    v-validate.initial="'required'"
                    name="tag"
                    :disabled="!editable"
                    multiple
                    searchable
                >
                    <bk-option
                        v-for="tag in stageTagList"
                        :key="tag.id"
                        :id="tag.id"
                        :name="tag.stageTagName"
                    >
                    </bk-option>
                </bk-select>
            </div>
        </form-field>
        <stage-control
            v-if="!isTriggerStage"
            ref="stageControl"
            :stage-control="stageControl"
            :disabled="!editable"
            :is-finally="isFinallyStage"
            :handle-stage-change="handleStageChange"
        />
    </section>
</template>

<script>
    import { mapActions, mapState, mapGetters } from 'vuex'
    import Vue from 'vue'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import StageControl from './StageControl'
    export default {
        name: 'container-content',
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
                'stageTagList'
            ]),
            ...mapGetters('atom', [
                'isTriggerContainer'
            ]),
            isFinallyStage () {
                return this.stage?.finally === true
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
            isTriggerStage () {
                return this.isTriggerContainer(this.stage?.containers?.[0]) ?? false
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
                    if (!this.editable) {
                        return
                    }

                    const isError = errors.any() || this.$refs.stageControl?.errors?.any?.()

                    this.handleStageChange('isError', isError)
                }
            }
        },

        methods: {
            ...mapActions('atom', [
                'updateStage'
            ]),
            handleStageChange (name, value) {
                if (!Object.prototype.hasOwnProperty.call(this.stage, name)) {
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
