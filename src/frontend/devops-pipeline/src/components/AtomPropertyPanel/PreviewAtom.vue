<template>
    <section class="bk-form bk-form-vertical" v-if="isCorrectFormat">
        <template v-if="!paramsGroupSort.length">
            <template v-for="(group, groupKey) in paramsGroupMap">
                <template v-if="groupKey === 'rootProps'">
                    <template v-for="(obj, key) in group.props">
                        <form-field
                            v-if="!isHidden(obj, atomValue) && rely(obj, atomValue)"
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
                                v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })"
                                :handle-change="handleUpdatePreviewInput"
                                :value="atomValue[key]"
                                v-bind="obj"
                                :placeholder="getPlaceholder(obj, atomValue)"
                            />
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
                            <form-field
                                v-if="!isHidden(obj, atomValue) && rely(obj, atomValue)"
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
                                    v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })"
                                    :handle-change="handleUpdatePreviewInput"
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
                <accordion v-if="group.isInputGroup && rely(group, atomValue)" show-checkbox :show-content="group.isExpanded" :key="groupKey">
                    <header class="var-header" slot="header">
                        <span>{{ group.label }}</span>
                        <i class="devops-icon icon-angle-down" style="display: block"></i>
                    </header>
                    <div slot="content">
                        <template v-for="(obj, key) in group.props">
                            <form-field
                                v-if="!isHidden(obj, atomValue) && rely(obj, atomValue)"
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
                                    v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })"
                                    :handle-change="handleUpdatePreviewInput"
                                    :value="atomValue[key]"
                                    v-bind="obj"
                                    :placeholder="getPlaceholder(obj,atomValue)"
                                />
                                <route-tips v-bind="getComponentTips(obj, atomValue)"></route-tips>
                            </form-field>
                        </template>
                    </div>
                </accordion>
                <template v-else>
                    <form-field
                        v-if="!isHidden(group, atomValue) && rely(group, atomValue)"
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
                            :name="groupKey"
                            v-validate.initial="Object.assign({}, group.rule, { required: !!group.required })"
                            :handle-change="handleUpdatePreviewInput"
                            :value="atomValue[groupKey]"
                            v-bind="group" :placeholder="getPlaceholder(group, atomValue)"
                        />
                        <route-tips v-bind="getComponentTips(group, atomValue)"></route-tips>
                    </form-field>
                </template>
            </template>
        </template>
        <atom-output :element="{}" :atom-props-model="atomPropsModel" />
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
    import DynamicParameterSimple from '@/components/AtomFormComponent/DynamicParameterSimple'
    import AtomOutput from './AtomOutput'

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
            DynamicParameterSimple,
            AtomOutput
        },
        mixins: [atomMixin],
        props: {
            handleUpdatePreviewInput: Function,
            atomValue: Object
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
