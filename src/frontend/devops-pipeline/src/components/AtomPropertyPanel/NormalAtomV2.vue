<template>
    <section
        class="bk-form bk-form-vertical"
        v-if="showFormUI"
    >
        <template v-if="!paramsGroupSort.length">
            <template v-for="(group, groupKey) in paramsGroupMap">
                <template v-if="groupKey === 'rootProps'">
                    <template v-for="(obj, key) in group.props">
                        <form-field
                            v-if="!isHidden(obj, atomValue) && rely(obj, atomValue)"
                            :class="{ 'changed-prop': atomVersionChangedKeys.includes(key) }"
                            :key="key"
                            :desc="obj.desc"
                            :desc-link="obj.descLink"
                            :desc-link-text="obj.descLinkText"
                            :required="obj.required"
                            :label="obj.label"
                            :is-error="errors.has(key)"
                            :error-msg="errors.first(key)"
                        >
                            <component
                                :is="obj.type"
                                :container="container"
                                :atom-value="atomValue"
                                :disabled="disabled"
                                :name="key"
                                v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.type) }, obj.rule, { required: !!obj.required })"
                                :handle-change="handleUpdateAtomInput"
                                :value="atomValue[key]"
                                v-bind="obj"
                                :get-atom-key-modal="getAtomKeyModal"
                                :placeholder="getPlaceholder(obj, atomValue)"
                            />
                            <route-tips v-bind="getComponentTips(obj, atomValue)"></route-tips>
                        </form-field>
                    </template>
                </template>
                <accordion
                    v-else
                    show-checkbox
                    :show-content="group.isExpanded"
                    :key="groupKey"
                >
                    <header
                        class="var-header"
                        slot="header"
                    >
                        <span>{{ group.label }}</span>
                        <i
                            class="devops-icon icon-angle-down"
                            style="display: block"
                        ></i>
                    </header>
                    <div slot="content">
                        <template v-for="(obj, key) in group.props">
                            <form-field
                                v-if="!isHidden(obj, atomValue) && rely(obj, atomValue)"
                                :class="{ 'changed-prop': atomVersionChangedKeys.includes(key) }"
                                :key="key"
                                :desc="obj.desc"
                                :desc-link="obj.descLink"
                                :desc-link-text="obj.descLinkText"
                                :required="obj.required"
                                :label="obj.label"
                                :is-error="errors.has(key)"
                                :error-msg="errors.first(key)"
                            >
                                <component
                                    :is="obj.type"
                                    :container="container"
                                    :atom-value="atomValue"
                                    :name="key"
                                    v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.type) }, obj.rule, { required: !!obj.required })"
                                    :handle-change="handleUpdateAtomInput"
                                    :value="atomValue[key]"
                                    v-bind="obj"
                                    :placeholder="getPlaceholder(obj, atomValue)"
                                />
                                <route-tips v-bind="getComponentTips(obj, atomValue)"></route-tips>
                            </form-field>
                        </template>
                    </div>
                </accordion>
            </template>
        </template>
        <template v-else>
            <template v-for="(group, groupKey) in paramsGroupMap">
                <accordion
                    v-if="group.isInputGroup && rely(group, atomValue)"
                    show-checkbox
                    :show-content="group.isExpanded"
                    :key="groupKey"
                >
                    <header
                        class="var-header"
                        slot="header"
                    >
                        <span>{{ group.label }}</span>
                        <i
                            class="devops-icon icon-angle-down"
                            style="display: block"
                        ></i>
                    </header>
                    <div slot="content">
                        <template v-for="(obj, key) in group.props">
                            <form-field
                                v-if="!isHidden(obj, atomValue) && rely(obj, atomValue)"
                                :class="{ 'changed-prop': atomVersionChangedKeys.includes(key) }"
                                :key="key"
                                :desc="obj.desc"
                                :desc-link="obj.descLink"
                                :desc-link-text="obj.descLinkText"
                                :required="obj.required"
                                :label="obj.label"
                                :is-error="errors.has(groupKey)"
                                :error-msg="errors.first(groupKey)"
                            >
                                <component
                                    :is="obj.type"
                                    :container="container"
                                    :atom-value="atomValue"
                                    :name="key"
                                    v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.type) }, obj.rule, { required: !!obj.required })"
                                    :handle-change="handleUpdateAtomInput"
                                    :value="atomValue[key]"
                                    v-bind="obj"
                                    :placeholder="getPlaceholder(obj, atomValue)"
                                />
                                <route-tips v-bind="getComponentTips(obj, atomValue)"></route-tips>
                            </form-field>
                        </template>
                    </div>
                </accordion>
                <template v-else>
                    <form-field
                        v-if="!isHidden(group, atomValue) && rely(group, atomValue)"
                        :class="{ 'changed-prop': atomVersionChangedKeys.includes(groupKey) }"
                        :key="groupKey"
                        :desc="group.desc"
                        :desc-link="group.descLink"
                        :desc-link-text="group.descLinkText"
                        :required="group.required"
                        :label="group.label"
                        :is-error="errors.has(groupKey)"
                        :error-msg="errors.first(groupKey)"
                    >
                        <component
                            :is="group.type"
                            :container="container"
                            :atom-value="atomValue"
                            :disabled="disabled"
                            :name="groupKey"
                            v-validate.initial="Object.assign({}, { max: getMaxLengthByType(group.type) }, group.rule, { required: !!group.required })"
                            :handle-change="handleUpdateAtomInput"
                            :value="atomValue[groupKey]"
                            v-bind="group"
                            :get-atom-key-modal="getAtomKeyModal"
                            :placeholder="getPlaceholder(group, atomValue)"
                        />
                        <route-tips v-bind="getComponentTips(group, atomValue)"></route-tips>
                    </form-field>
                </template>
            </template>
        </template>
        <atom-output
            :element="element"
            :atom-props-model="atomPropsModel"
        />
    </section>
    <section v-else>
        <div class="empty-tips">{{ $t('editPage.noAppIdTips') }}</div>
    </section>
</template>

<script>
    import AppId from '@/components/AtomFormComponent/AppId'
    import CcAppId from '@/components/AtomFormComponent/CcAppId'
    import DynamicParameter from '@/components/AtomFormComponent/DynamicParameter'
    import DynamicParameterSimple from '@/components/AtomFormComponent/DynamicParameterSimple'
    import Parameter from '@/components/AtomFormComponent/Parameter'
    import TimePicker from '@/components/AtomFormComponent/TimePicker'
    import Tips from '@/components/AtomFormComponent/Tips'
    import Accordion from '@/components/atomFormField/Accordion'
    import { getAtomDefaultValue } from '@/store/modules/atom/atomUtil'
    import Selector from '../AtomFormComponent/Selector'
    import validMixins from '../validMixins'
    import AtomOutput from './AtomOutput'
    import atomMixin from './atomMixin'

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
            DynamicParameterSimple,
            AtomOutput
        },
        mixins: [atomMixin, validMixins],
        computed: {
            appIdProps () {
                let appIdProps
                Object.keys(this.atomPropsModel.input).every(key => {
                    if (this.atomPropsModel.input[key].type?.indexOf('app-id') > -1) {
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
            inputProps () {
                try {
                    const { [this.appIdPropsKey]: ccAppId, ...restProps } = this.atomPropsModel.input
                    return {
                        ...(ccAppId
                            ? {
                                [this.appIdPropsKey]: ccAppId
                            }
                            : {}),
                        ...restProps
                    }
                } catch (e) {
                    console.warn('getAtomModalInput error', e)
                    return {}
                }
            },
            paramsGroupSort () {
                return this.atomPropsModel.sort || []
            },
            paramsGroupMap () {
                const inputGroups = this.atomPropsModel.inputGroups || []
                if (this.paramsGroupSort.length) {
                    const inputGroupMap = inputGroups.reduce((inputGroupMap, group) => {
                        inputGroupMap[group.name] = {
                            ...group,
                            isInputGroup: true,
                            props: []
                        }
                        return inputGroupMap
                    }, {})

                    const groupMap = {}
                    Object.keys(this.inputProps).forEach(key => {
                        const prop = this.inputProps[key]
                        if (prop.groupName) {
                            inputGroupMap[prop.groupName].props.push(prop)
                            groupMap[prop.groupName] = inputGroupMap[prop.groupName]
                        } else {
                            groupMap[key] = prop
                        }
                    })
                    const sortGroupMap = {}
                    this.paramsGroupSort.forEach(key => {
                        sortGroupMap[key] = groupMap[key]
                    })
                    return sortGroupMap
                } else {
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
                    Object.keys(this.inputProps).forEach(key => {
                        const prop = this.inputProps[key]
                        const group = prop.groupName && groupMap[prop.groupName] ? groupMap[prop.groupName] : groupMap.rootProps
                        group.props[key] = prop
                    })
                    return groupMap
                }
            },
            atomValue () {
                try {
                    const atomDefaultValue = getAtomDefaultValue(this.atomPropsModel.input)

                    // 新增字段，已添加插件读取默认值
                    const atomValue = Object.keys(atomDefaultValue).reduce((res, key) => {
                        if (!Object.prototype.hasOwnProperty.call(this.element.data.input, key)) {
                            res[key] = atomDefaultValue[key]
                        }
                        return res
                    }, this.element.data.input)
                    return atomValue
                } catch (e) {
                    console.warn('getAtomInput error', e)
                    return {}
                }
            }
        },
        updated () {
            if (this.appIdPropsKey && this.atomValue[this.appIdPropsName] !== this.appId) {
                // this.handleUpdateAtomInput(this.appIdPropsName, this.appId)
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
.atom-output-var-list {

    >h4,
    >p {
        margin: 0;
    }

    >p {
        line-height: 36px;
    }
}
</style>
