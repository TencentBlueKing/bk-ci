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
            <logo
                name="close-circle"
                size="14"
                class="yaml-branch-inactive-icon"
            />
            {{ $t('editPage.yamlBranchInactive', [currentYamlBranch]) }}
        </p>
    </div>
</template>

<script>
    import VuexInput from '@/components/atomFormField/VuexInput'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import Logo from '@/components/Logo'
    import yamlBranchMixin from './yamlBranchMixin'

    export default {
        name: 'yaml-branch-field',
        components: {
            VuexInput,
            FormField,
            Logo
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
        display: flex;
        align-items: flex-start;
        color: #ea3636;
        font-size: 12px;
        line-height: 20px;
    }
    .yaml-branch-inactive-icon {
        flex-shrink: 0;
        margin: 3px 4px 0 0;
    }
</style>
