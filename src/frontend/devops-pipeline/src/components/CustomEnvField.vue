
<template>
    <accordion show-checkbox show-content>
        <header class="var-header" slot="header">
            <span>{{ $t('storeMap.customEnv') }}</span>
            <i class="devops-icon icon-angle-down" style="display:block"></i>
        </header>
        <div slot="content" class="bk-form bk-form-vertical">
            <form-field
                :is-error="errors.has(customEnvModel.key)"
                :error-msg="errors.first(customEnvModel.key)"
            >
                <key-value-normal
                    :name="customEnvModel.key"
                    :handle-change="handleChange"
                    :value="value"
                    :disabled="disabled"
                />
            </form-field>
        </div>
    </accordion>
</template>

<script>
    import Accordion from '@/components/atomFormField/Accordion'
    import KeyValueNormal from '@/components/atomFormField/KeyValueNormal'
    import FormField from '@/components/AtomPropertyPanel/FormField'

    export default {
        components: {
            Accordion,
            KeyValueNormal,
            FormField
        },
        props: {
            value: {
                type: Array,
                default: () => []
            },
            disabled: Boolean
        },
        emits: ['update:value', 'input', 'change'],
        computed: {
            customEnvModel () {
                return {
                    key: 'customEnv',
                    component: 'key-value-normal',
                    default: [],
                    allowNull: true
                }
            }
        },
        methods: {
            handleChange (name, value) {
                console.log(name, value)
                this.$emit('update:value', value)
                this.$emit('input', value)
                this.$emit('change', value)
            }
        }
    }
</script>
