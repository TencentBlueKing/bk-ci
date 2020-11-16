<template>
    <div class="bk-form bk-form-vertical">
        <form-field v-for="(obj, key) in atomPropsModel" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
            <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: obj.required })" :handle-change="handleUpdateElement" :value="element[key]" v-bind="obj"></component>
        </form-field>
        <accordion show-content show-checkbox>
            <header class="var-header" slot="header">
                <span>{{ $t('editPage.atomOutput') }}</span>
                <i class="devops-icon icon-angle-down" style="display: block"></i>
            </header>
            <div slot="content">
                <form-field class="output-namespace" :desc="$t('editPage.namespaceTips')" :label="$t('editPage.outputNamespace')" :is-error="errors.has('namespace')" :error-msg="errors.first('namespace')">
                    <vuex-input name="namespace" v-validate.initial="{ varRule: true }" :handle-change="handleUpdateElement" :value="namespace" placeholder="" />
                </form-field>
                <div class="atom-output-var-list">
                    <h4>{{ $t('editPage.outputItemList') }}：</h4>
                    <p v-for="(output, key) in outputProps" :key="key">
                        {{ namespace ? `${namespace}_` : '' }}{{ key }}
                        <bk-popover placement="right">
                            <i class="bk-icon icon-info-circle" />
                            <div slot="content">
                                {{ output.description }}
                            </div>
                        </bk-popover>
                        <copy-icon :value="`\${${namespace ? `${namespace}_${key}` : key}}`"></copy-icon>
                    </p>
                </div>
            </div>
        </accordion>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import copyIcon from '@/components/copyIcon'

    export default {
        name: 'manual-review-user-task',
        components: {
            copyIcon
        },
        mixins: [atomMixin, validMixins],
        data () {
            return {
                outputProps: {
                    MANUAL_REVIEWER: { description: this.$t('editPage.manualReviewNameSpaceTip') },
                    MANUAL_REVIEW_SUGGEST: { description: this.$t('editPage.manualReviewSuggestNameSpaceTip') }
                }
            }
        },
        computed: {
            namespace () {
                return this.element.namespace
            }
        }
    }
</script>
