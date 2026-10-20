<template>
    <div v-if="showYamlBranch">
        <form-field
            :label="$t('editPage.yamlBranch')"
            :desc="$t('editPage.yamlBranchTips')"
            :is-error="errors.has('yamlBranch')"
            :error-msg="errors.first('yamlBranch')"
        >
            <vuex-input
                name="yamlBranch"
                :value="element.yamlBranch"
                :handle-change="handleUpdateElement"
                :disabled="disabled"
                :placeholder="$t('editPage.yamlBranch')"
            />
        </form-field>
        <p
            v-if="yamlBranchInactive"
            class="yaml-branch-inactive"
        >
            {{ $t('editPage.yamlBranchInactive') }}
        </p>
    </div>
</template>

<script>
    import VuexInput from '@/components/atomFormField/VuexInput'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import yamlBranchMixin from './yamlBranchMixin'

    export default {
        name: 'yaml-branch-field',
        components: {
            VuexInput,
            FormField
        },
        mixins: [yamlBranchMixin],
        props: {
            element: {
                type: Object,
                default: () => ({})
            },
            disabled: {
                type: Boolean,
                default: false
            },
            errors: {
                type: Object,
                default: () => ({
                    has: () => false,
                    first: () => ''
                })
            },
            handleUpdateElement: {
                type: Function,
                required: true
            }
        }
    }
</script>

<style lang="scss" scoped>
    .yaml-branch-inactive {
        margin: -8px 0 16px;
        color: #ea3636;
        font-size: 12px;
        line-height: 20px;
    }
</style>
