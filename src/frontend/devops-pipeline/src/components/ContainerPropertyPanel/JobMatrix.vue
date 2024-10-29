<template>
    <accordion
        show-checkbox
        :show-content="enableMatrix"
        key="otherChoice"
        is-version="true"
    >
        <header
            class="var-header"
            slot="header"
        >
            <span>{{ $t('editPage.enableMatrix') }}</span>
            <input
                class="accordion-checkbox"
                :disabled="disabled"
                :checked="enableMatrix"
                type="checkbox"
                @click.stop
                @change="toggleMatrix"
            />
        </header>
        <div
            slot="content"
            class="bk-form bk-form-vertical"
            v-if="enableMatrix"
        >
            <template v-for="(obj, key) in optionModel">
                <form-field
                    :key="key"
                    :desc="obj.desc"
                    :required="obj.required"
                    :label="obj.label"
                    :is-error="errors.has(key)"
                    :error-msg="errors.first(key)"
                >
                    <component
                        :is="obj.component"
                        :name="key"
                        v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })"
                        :handle-change="handleUpdateJobMatrix"
                        :value="matrixControlOption[key]"
                        :disabled="disabled"
                        v-bind="obj"
                    />
                </form-field>
            </template>
        </div>
    </accordion>
</template>

<script>
    import atomMixin from '@/components/AtomPropertyPanel/atomMixin'
    import validMixins from '@/components/validMixins'
    import jobOptionConfigMixin from '@/store/modules/common/jobOptionConfigMixin'
    import { mapActions } from 'vuex'

    export default {
        name: 'job-mutual',
        mixins: [atomMixin, validMixins, jobOptionConfigMixin],
        props: {
            enableMatrix: {
                type: Boolean,
                default: false
            },
            matrixControlOption: {
                type: Object,
                default: () => ({})
            },
            disabled: {
                type: Boolean,
                default: false
            },
            updateContainerParams: {
                type: Function,
                required: true
            }
        },
        computed: {
            optionModel () {
                return this.JOB_MATRIX || {}
            }
        },
        created () {
            if (!this.disabled) {
                this.initOptionConfig()
            }
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing'
            ]),
            handleUpdateJobMatrix (name, value) {
                this.setPipelineEditing(true)
                this.updateContainerParams(
                    'matrixControlOption',
                    Object.assign(this.matrixControlOption || {}, { [name]: value })
                )
                if (name === 'strategyStr') {
                    this.$validator.validateAll()
                }
            },
            toggleMatrix (e) {
                const enable = e.target.checked
                this.setPipelineEditing(true)
                this.updateContainerParams('matrixGroupFlag', enable)
            },
            initOptionConfig () {
                this.updateContainerParams('matrixControlOption', this.getJobOptionDefault(this.JOB_MATRIX, this.matrixControlOption))
            }
        }
    }
</script>
