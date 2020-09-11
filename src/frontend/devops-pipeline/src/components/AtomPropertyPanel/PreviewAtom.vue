<template>
    <section class="bk-form bk-form-vertical" v-if="isCorrectFormat">
        <template v-for="(group, groupKey) in paramsGroupMap">
            <template v-if="groupKey === &quot;rootProps&quot;">
                <template v-for="(obj, key) in group.props">
                    <form-field v-if="!isHidden(obj, atomValue) && rely(obj, atomValue)" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                        <component :is="obj.type" :container="container" :atom-value="atomValue" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdatePreviewInput" :value="atomValue[key]" v-bind="obj" :placeholder="getPlaceholder(obj, atomValue)"></component>
                        <route-tips v-bind="getComponentTips(obj, atomValue)"></route-tips>
                    </form-field>
                </template>
            </template>
            <accordion v-else show-checkbox :show-content="group.isExpanded" :key="groupKey">
                <header class="var-header" slot="header">
                    <span>{{ group.label }}</span>
                    <i class="devops-icon icon-angle-down" style="display: block"></i>
                </header>
                <div slot="content">
                    <template v-for="(obj, key) in group.props">
                        <form-field v-if="!isHidden(obj, atomValue) && rely(obj, atomValue)" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                            <component :is="obj.type" :container="container" :atom-value="atomValue" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdatePreviewInput" :value="atomValue[key]" v-bind="obj" :placeholder="getPlaceholder(obj, atomValue)"></component>
                            <route-tips v-bind="getComponentTips(obj, atomValue)"></route-tips>
                        </form-field>
                    </template>
                </div>
            </accordion>
        </template>

        <accordion v-if="outputProps && Object.keys(outputProps).length > 0" show-checkbox show-content>
            <header class="var-header" slot="header">
                <span>{{ $t('editPage.atomOutput') }}</span>
                <i class="devops-icon icon-angle-down" style="display: block"></i>
            </header>
            <div slot="content">
                <form-field class="output-namespace" :desc="outputNamespaceDesc" label="输出字段命名空间" :is-error="errors.has(&quot;namespace&quot;)" :error-msg="errors.first(&quot;namespace&quot;)">
                    <vuex-input name="namespace" v-validate.initial="{ varRule: true }" v-model="namespace" />
                </form-field>
                <div class="atom-output-var-list">
                    <h4>{{ $t('editPage.outputItemList') }}：</h4>
                    <p v-for="(output, key) in outputProps" :key="key">
                        {{ namespace ? `${namespace}_` : '' }}{{ key }}
                        <bk-popover placement="right">
                            <i class="icon-info-circle" />
                            <div slot="content">
                                {{ output.description }}
                            </div>
                        </bk-popover>
                        <copy-icon :value="`\${${namespace ? `${namespace}_${key}` : key}}`"></copy-icon>
                    </p>
                </div>
            </div>
        </accordion>
    </section>
    <section v-else>
        <div class="empty-tips">{{ emptyTips }}</div>
    </section>
</template>

<script>
    import atomMixin from './atomMixin'
    import Selector from '../AtomFormComponent/Selector'
    import CcAppId from '@/components/AtomFormComponent/CcAppId'
    import AppId from '@/components/AtomFormComponent/AppId'
    import Accordion from '@/components/atomFormField/Accordion'
    import TimePicker from '@/components/AtomFormComponent/TimePicker'
    import Parameter from '@/components/AtomFormComponent/Parameter'
    import Tips from '@/components/AtomFormComponent/Tips'
    import DynamicParameter from '@/components/AtomFormComponent/DynamicParameter'
    import copyIcon from '@/components/copyIcon'

    export default {
        name: 'preview-atom',
        components: {
            Selector,
            Accordion,
            CcAppId,
            AppId,
            TimePicker,
            Parameter,
            Tips,
            DynamicParameter,
            copyIcon
        },
        mixins: [atomMixin],
        props: {
            handleUpdatePreviewInput: Function,
            atomValue: Object
        },
        data () {
            return {
                outputNamespaceDesc: this.$t('editPage.namespaceTips'),
                namespace: ''
            }
        },
        computed: {
            inputProps () {
                try {
                    return this.atomPropsModel.input
                } catch (e) {
                    console.warn('getAtomModalInput error', e)
                    return {}
                }
            },
            outputProps () {
                try {
                    return this.atomPropsModel.output
                } catch (e) {
                    console.warn('getAtomModalOpt error', e)
                    return null
                }
            },
            paramsGroupMap () {
                const inputGroups = this.atomPropsModel.inputGroups || []
                const groupMap = inputGroups.reduce((groupMap, group) => {
                    groupMap[group.name] = {
                        ...group,
                        props: {}
                    }
                    return groupMap
                }, {
                    rootProps: {
                        props: {}
                    }
                })
                Object.keys(this.inputProps).map(key => {
                    const prop = this.inputProps[key]
                    const group = prop.groupName && groupMap[prop.groupName] ? groupMap[prop.groupName] : groupMap['rootProps']
                    group.props[key] = prop
                })
                return groupMap
            },
            isCorrectFormat () {
                return this.atomPropsModel && typeof this.atomPropsModel === 'object' && this.atomPropsModel.input
            },
            emptyTips () {
                return JSON.stringify(this.atomPropsModel) === '{}' ? this.$t('atomDebug.taskJsonEmpty') : this.$t('atomDebug.outputErrTips')
            }
        }
    }
</script>

<style lang="scss">
    .output-namespace {
        margin-bottom: 12px;
    }
    .atom-output-var-list {
        > h4,
        > p {
            margin: 0;
        }
        > p {
            line-height: 36px;
        }
    }
</style>
