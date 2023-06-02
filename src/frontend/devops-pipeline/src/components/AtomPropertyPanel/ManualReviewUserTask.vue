<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in atomPropsModel">
            <form-field v-if="!isHidden(obj, element)" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component
                    :is="obj.component"
                    v-bind="obj"
                    v-model="element[key]"
                    :disabled="disabled"
                    :handle-change="handleChange"
                    :show-content="disabled"
                    :name="key" v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.component) }, obj.rule, { required: obj.required })" />
            </form-field>
        </template>
        <accordion show-content show-checkbox>
            <header class="var-header" slot="header">
                <span>{{ $t('editPage.atomOutput') }}</span>
                <i class="devops-icon icon-angle-down" style="display: block" />
            </header>
            <div slot="content">
                <form-field class="output-namespace" :desc="$t('outputNameSpaceDescTips')" :label="$t('editPage.outputNamespace')" :is-error="errors.has('namespace')" :error-msg="errors.first('namespace')">
                    <vuex-input name="namespace" v-validate.initial="{ varRule: true }" :handle-change="handleUpdateElement" :value="namespace" placeholder="" />
                </form-field>
                <div class="atom-output-var-list">
                    <h4>{{ $t('editPage.outputItemList') }}</h4>
                    <p v-for="(output, key) in outputProps" :key="key">
                        {{ namespace ? `${namespace}_` : '' }}{{ key }}
                        <bk-popover placement="right">
                            <i class="bk-icon icon-info-circle" />
                            <div slot="content">
                                {{ output.description }}
                            </div>
                        </bk-popover>
                        <copy-icon :value="bkVarWrapper(namespace ? `${namespace}_${key}` : key)"></copy-icon>
                    </p>
                </div>
            </div>
        </accordion>
    </div>
</template>

<script>
    import copyIcon from '@/components/copyIcon'
    import validMixins from '../validMixins'
    import atomMixin from './atomMixin'

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
        },
        watch: {
            'element.notifyType' (val) {
                if (val.includes('WEWORK_GROUP')) {
                    this.atomPropsModel.notifyGroup.hidden = false
                } else {
                    this.atomPropsModel.notifyGroup.hidden = true
                    this.handleUpdateElement('notifyGroup', [])
                }
            }
        },
        created () {
            if (this.element && this.element.notifyType.includes('WEWORK_GROUP')) {
                this.atomPropsModel.notifyGroup.hidden = false
            }
        },
        methods: {
            handleChange (name, value) {
                if (name === 'notifyGroup') {
                    const notifyGroup = value.split(',')
                    this.handleUpdateElement(name, notifyGroup)
                } else {
                    this.handleUpdateElement(name, value)
                }
            }
        }
    }
</script>
