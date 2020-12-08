<template>
    <section class="bk-form bk-form-vertical" v-if="showFormUI">
        <template v-for="(group, groupKey) in paramsGroupMap">
            <template v-if="groupKey === 'rootProps'">
                <template v-for="(obj, key) in group.props">
                    <form-field v-if="!isHidden(obj, atomValue) && rely(obj, atomValue)" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                        <component :is="obj.type" :container="container" :atom-value="atomValue" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateAtomInput" :value="atomValue[key]" v-bind="obj" :get-atom-key-modal="getAtomKeyModal" :placeholder="getPlaceholder(obj, atomValue)"></component>
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
                            <component :is="obj.type" :container="container" :atom-value="atomValue" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateAtomInput" :value="atomValue[key]" v-bind="obj" :placeholder="getPlaceholder(obj, atomValue)"></component>
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
                <form-field class="output-namespace" :desc="outputNamespaceDesc" :label="$t('editPage.outputNamespace')" :is-error="errors.has('namespace')" :error-msg="errors.first('namespace')">
                    <vuex-input name="namespace" v-validate.initial="{ varRule: true }" :handle-change="handleUpdateAtomOutputNameSpace" :value="namespace" placeholder="" />
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
    </section>
    <section v-else>
        <div class="empty-tips">{{ $t('editPage.noAppIdTips') }}</div>
    </section>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import Selector from '../AtomFormComponent/Selector'
    import CcAppId from '@/components/AtomFormComponent/CcAppId'
    import AppId from '@/components/AtomFormComponent/AppId'
    import Accordion from '@/components/atomFormField/Accordion'
    import TimePicker from '@/components/AtomFormComponent/TimePicker'
    import Parameter from '@/components/AtomFormComponent/Parameter'
    import Tips from '@/components/AtomFormComponent/Tips'
    import DynamicParameter from '@/components/AtomFormComponent/DynamicParameter'
    import { getAtomDefaultValue } from '@/store/modules/atom/atomUtil'
    import copyIcon from '@/components/copyIcon'
    export default {
        name: 'normal-atom-v2',
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
        mixins: [atomMixin, validMixins],
        computed: {
            appIdProps () {
                let appIdProps
                Object.keys(this.atomPropsModel.input).every(key => {
                    if (this.atomPropsModel.input[key].type.indexOf('app-id') > -1) {
                        appIdProps = {
                            ...this.atomPropsModel.input[key],
                            atomPropsName: key
                        }
                        return false
                    }
                    return true
                })
                return appIdProps
            },
            appIdPropsKey () {
                try {
                    const { appIdKey = 'ccAppId' } = this.appIdProps
                    return appIdKey
                } catch (error) {
                    return ''
                }
            },
            appIdPropsName () {
                return this.appIdProps ? this.appIdProps.atomPropsName : ''
            },
            hasAppId () {
                if (!this.appIdPropsKey) return false
                return this.$store.state.curProject && this.$store.state.curProject[this.appIdPropsKey]
            },
            appId () {
                return this.hasAppId ? this.$store.state.curProject[this.appIdPropsKey] : ''
            },
            showFormUI () {
                return !this.appIdProps || (this.appIdPropsKey && this.hasAppId)
            },
            outputNamespaceDesc () {
                return this.$t('editPage.namespaceTips')
            },
            inputProps () {
                try {
                    const { [this.appIdPropsKey]: ccAppId, ...restProps } = this.atomPropsModel.input
                    return {
                        ...(ccAppId ? {
                            [this.appIdPropsKey]: ccAppId
                        } : {}),
                        ...restProps
                    }
                } catch (e) {
                    console.warn('getAtomModalInput error', e)
                    return {}
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
            outputProps () {
                try {
                    return this.atomPropsModel.output
                } catch (e) {
                    console.warn('getAtomModalOpt error', e)
                    return null
                }
            },
            namespace () {
                try {
                    const ns = this.element.data.namespace || ''
                    return ns.trim().replace(/(.+)?\_$/, '$1')
                } catch (e) {
                    console.warn('getAtomOutput namespace error', e)
                    return ''
                }
            },
            atomValue () {
                try {
                    const atomDefaultValue = getAtomDefaultValue(this.atomPropsModel.input)
                    // 新增字段，已添加插件读取默认值
                    const atomValue = Object.keys(this.element.data.input).reduce((res, key) => {
                        if (atomDefaultValue.hasOwnProperty(key)) {
                            res[key] = this.element.data.input[key]
                        }
                        return res
                    }, atomDefaultValue)
                    this.handleUpdateWholeAtomInput(atomValue)
                    return atomValue
                } catch (e) {
                    console.warn('getAtomInput error', e)
                    return {}
                }
            }
        },
        updated () {
            if (this.appIdPropsKey && this.atomValue[this.appIdPropsName] !== this.appId) {
                this.handleUpdateAtomInput(this.appIdPropsName, this.appId)
            }
        },
        methods: {
            getAtomKeyModal (key) {
                return this.inputProps[key] || null
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
