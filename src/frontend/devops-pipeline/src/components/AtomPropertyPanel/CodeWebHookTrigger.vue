<template>
    <section class="bk-form bk-form-vertical">
        <template v-for="group in paramsGroupMap">
            <template v-for="(obj, key) in group.props">
                <template v-if="obj.type === 'group'">
                    <form-field-group v-if="rely(obj, atomValue)" :name="key" :value="atomValue[key]" :handle-change="handleUpdateAtomInput" :key="key" v-bind="obj">
                        <template v-for="i in obj.children">
                            <form-field
                                v-if="!isHidden(i, atomValue) && rely(i, atomValue)"
                                :key="i.key"
                                v-bind="i"
                                :is-error="errors.has(i.key)"
                                :error-msg="errors.first(i.key)"
                            >
                                <component
                                    :is="i.component || i.type"
                                    :container="container"
                                    :atom-value="atomValue"
                                    :disabled="disabled"
                                    :name="i.key"
                                    v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.type) },i.rule, { required: !!i.required })"
                                    :handle-change="handleUpdateAtomInput"
                                    :value="atomValue[i.key]"
                                    v-bind="i"
                                    :get-atom-key-modal="getAtomKeyModal"
                                    :placeholder="getPlaceholder(i, atomValue)">
                                </component>
                            </form-field>
                        </template>
                    </form-field-group>
                </template>
                <template v-else>
                    <form-field
                        v-if="!isHidden(obj, atomValue) && rely(obj, atomValue)"
                        :key="key"
                        v-bind="obj"
                        :is-error="errors.has(obj.key)"
                        :error-msg="errors.first(obj.key)"
                    >
                        <component
                            :is="obj.component || obj.type"
                            :container="container"
                            :disabled="disabled"
                            :element="element.data.input"
                            :name="key"
                            v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.type) }, obj.rule, { required: !!obj.required })"
                            :handle-change="handleUpdateAtomInput"
                            :value="atomValue[key]"
                            v-bind="obj"
                            :get-atom-key-modal="getAtomKeyModal"
                            :placeholder="getPlaceholder(obj, atomValue)"
                        >
                        </component>
                    </form-field>
                </template>
            </template>
        </template>
    </section>
</template>

<script>
    import { getAtomDefaultValue } from '@/store/modules/atom/atomUtil'
    import AtomCheckboxList from '@/components/atomFormField/AtomCheckboxList'
    import Tips from '@/components/AtomFormComponent/Tips'
    import validMixins from '../validMixins'
    import atomMixin from './atomMixin'

    export default {
        name: 'code-tgit-webhook-trigger',
        components: {
            Tips,
            AtomCheckboxList
        },
        mixins: [atomMixin, validMixins],
        computed: {
            appIdProps () {
                let appIdProps
                Object.keys(this.atomPropsModelInput).every(key => {
                    if (this.atomPropsModelInput[key].type && this.atomPropsModelInput[key].type?.indexOf('app-id') > -1) {
                        appIdProps = {
                            ...this.atomPropsModelInput[key],
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
            atomPropsModelInput () {
                const input = {}
                Object.keys(this.atomPropsModel.input).forEach(key => {
                    if (key === 'repositoryType') {
                        input[key] = this.atomPropsModel.input[key]
                            this.atomPropsModel.input[key]?.list.forEach(i => {
                                input[i.key] = i
                            })
                    } else if (this.atomPropsModel.input[key].type === 'group') {
                        this.atomPropsModel.input[key].children.forEach(children => {
                            input[children.key] = children
                        })
                    } else {
                        input[key] = this.atomPropsModel.input[key]
                    }
                })
                return input
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
                Object.keys(this.inputProps).forEach(key => {
                    const prop = this.inputProps[key]
                    const group = prop.groupName && groupMap[prop.groupName] ? groupMap[prop.groupName] : groupMap.rootProps
                    group.props[key] = prop
                })
                return groupMap
            },
            atomValue () {
                try {
                    const atomDefaultValue = getAtomDefaultValue(this.atomPropsModelInput)
                    // 新增字段，已添加插件读取默认值
                    const atomValue = Object.keys(this.element.data.input).reduce((res, key) => {
                        if (Object.prototype.hasOwnProperty.call(atomDefaultValue, key)) {
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
        methods: {
            getAtomKeyModal (key) {
                return this.inputProps[key] || null
            }
        }
    }
</script>
