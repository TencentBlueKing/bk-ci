<template>
    <section class="bk-form bk-form-vertical" v-if="showFormUI">
        <template v-for="(group, groupKey) in paramsGroupMap">
            <template v-if="groupKey === &quot;rootProps&quot;">
                <template v-for="(obj, key) in group.props">
                    <form-field v-if="!isHidden(obj, atomValue) && rely(obj, atomValue)" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                        <component :is="obj.type" :container="container" :atom-value="atomValue" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateAtomInput" :value="atomValue[key]" v-bind="obj" :placeholder="getPlaceholder(obj, atomValue)"></component>
                        <route-tips v-bind="getComponentTips(obj, atomValue)"></route-tips>
                    </form-field>
                </template>
            </template>
            <accordion v-else show-checkbox :show-content="group.isExpanded" :key="groupKey">
                <header class="var-header" slot="header">
                    <span>{{ group.label }}</span>
                    <i class="bk-icon icon-angle-down" style="display: block"></i>
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
        <accordion v-if="outputProps" show-checkbox show-content>
            <header class="var-header" slot="header">
                <span>插件输出</span>
                <i class="bk-icon icon-angle-down" style="display: block"></i>
            </header>
            <div slot="content">
                <form-field class="output-namespace" :desc="outputNamespaceDesc" label="输出字段命名空间" :is-error="errors.has(&quot;namespace&quot;)" :error-msg="errors.first(&quot;namespace&quot;)">
                    <vuex-input name="namespace" v-validate.initial="{ varRule: true }" :handle-change="handleUpdateAtomOutputNameSpace" :value="namespace" placeholder="" />
                </form-field>
                <div class="atom-output-var-list">
                    <h4>输出字段列表：</h4>
                    <p v-for="(output, key) in outputProps" :key="key">
                        {{ namespace ? `${namespace}_` : '' }}{{ key }}
                        <bk-popover placement="right">
                            <i class="bk-icon icon-info-circle" />
                            <div slot="content">
                                {{ output.description }}
                            </div>
                        </bk-popover>
                    </p>
                </div>
            </div>
        </accordion>
    </section>
    <section v-else>
        <div class="empty-tips">你正在使用的插件需要使用蓝鲸部署服务,请联系DevOps(蓝盾人工客服)开启</div>
    </section>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import Selector from '../AtomFormComponent/Selector'
    import CcAppId from '@/components/AtomFormComponent/CcAppId'
    import Accordion from '@/components/atomFormField/Accordion'
    import SelectInput from '@/components/AtomFormComponent/SelectInput'
    import DatePicker from '@/components/AtomFormComponent/DatePicker'

    export default {
        name: 'normal-atom-v2',
        components: {
            Selector,
            Accordion,
            CcAppId,
            SelectInput,
            DatePicker
        },
        mixins: [atomMixin, validMixins],
        computed: {
            hasAppId () {
                return this.$store.state.curProject && this.$store.state.curProject.cc_app_id
            },
            appId () {
                return this.hasAppId ? this.$store.state.curProject.cc_app_id : ''
            },
            showFormUI () {
                return !this.CcAppIdPropsKey || (this.CcAppIdPropsKey && this.hasAppId)
            },
            outputNamespaceDesc () {
                return `用于解决流水线下，相同插件有多个实例时，输出字段使用冲突的问题。\n当没有冲突时，无需添加命名空间。\n当修改了命名空间后，后续使用到对应字段的地方也需要同步修改`
            },
            CcAppIdPropsKey () {
                let ccPropsKey
                Object.keys(this.atomPropsModel.input).every(key => {
                    if (this.atomPropsModel.input[key].type === 'cc-app-id') {
                        ccPropsKey = key
                        return false
                    }
                    return true
                })
                return ccPropsKey
            },
            inputProps () {
                try {
                    const { [this.CcAppIdPropsKey]: ccAppId, ...restProps } = this.atomPropsModel.input
                    return {
                        ...(ccAppId ? {
                            [this.CcAppIdPropsKey]: ccAppId
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
                    return {
                        ...this.element.data.input
                    }
                } catch (e) {
                    console.warn('getAtomInput error', e)
                    return {}
                }
            }
        },
        updated () {
            if (this.CcAppIdPropsKey && this.atomValue[this.CcAppIdPropsKey] !== this.appId) {
                this.handleUpdateAtomInput(this.CcAppIdPropsKey, this.appId)
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
