<template>
    <section
        class="bk-form bk-form-vertical"
        v-if="showFormUI"
    >
        <template v-if="!paramsGroupSort.length">
            <template v-for="(group, groupKey) in paramsGroupMap">
                <template v-if="groupKey === 'rootProps'">
                    <atom-param-row
                        v-for="(obj, key) in group.props"
                        :param="obj"
                        :key="key"
                        :param-key="key"
                        :full-atom-value="atomValue"
                        :get-atom-key-modal="getAtomKeyModal"
                        :container="container"
                        :element="element"
                    />
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
                        <atom-param-row
                            v-for="(obj, key) in group.props"
                            :param="obj"
                            :key="key"
                            :param-key="key"
                            :full-atom-value="atomValue"
                            :get-atom-key-modal="getAtomKeyModal"
                            :container="container"
                            :element="element"
                        />
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
                        <atom-param-row
                            v-for="(obj, key) in group.props"
                            :param="obj"
                            :key="key"
                            :param-key="key"
                            :full-atom-value="atomValue"
                            :get-atom-key-modal="getAtomKeyModal"
                            :container="container"
                            :element="element"
                        />
                    </div>
                </accordion>
                <template v-else>
                    <atom-param-row
                        :param="group"
                        :key="groupKey"
                        :param-key="groupKey"
                        :full-atom-value="atomValue"
                        :get-atom-key-modal="getAtomKeyModal"
                        :container="container"
                        :element="element"
                    />
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
    import AtomParamRow from './AtomParamRow'
    import { getAtomDefaultValue } from '@/store/modules/atom/atomUtil'
    import AtomOutput from './AtomOutput'
    import Accordion from '@/components/atomFormField/Accordion'

    export default {
        name: 'normal-atom-v2',
        components: {
            AtomOutput,
            AtomParamRow,
            Accordion
        },
        props: {
            elementIndex: Number,
            containerIndex: Number,
            stageIndex: Number,
            element: Object,
            container: Object,
            stage: Object,
            atomPropsModel: Object,
            setAtomValidate: Function,
            disabled: Boolean
        },
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
