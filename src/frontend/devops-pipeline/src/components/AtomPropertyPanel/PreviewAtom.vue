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
                    <i class="bk-icon icon-angle-down" style="display: block"></i>
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
        <!-- <atom-output :element="element" :atom-props-model="atomPropsModel"></atom-output> -->
    </section>
    <section v-else>
        <div class="empty-tips">{{ $t('outputErrTips') }}</div>
    </section>
</template>

<script>
    // import AtomOutput from './AtomOutput'
    import atomMixin from './atomMixin'
    import Selector from '../AtomFormComponent/Selector'
    import CcAppId from '@/components/AtomFormComponent/CcAppId'
    import AppId from '@/components/AtomFormComponent/AppId'
    import Accordion from '@/components/atomFormField/Accordion'
    import SelectInput from '@/components/AtomFormComponent/SelectInput'
    import TimePicker from '@/components/AtomFormComponent/TimePicker'
    import Parameter from '@/components/AtomFormComponent/Parameter'
    import Tips from '@/components/AtomFormComponent/Tips'

    export default {
        name: 'preview-atom',
        components: {
            // AtomOutput,
            Selector,
            Accordion,
            CcAppId,
            AppId,
            SelectInput,
            TimePicker,
            Parameter,
            Tips
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
                console.log(this.atomPropsModel, 2)
                return this.atomPropsModel && typeof this.atomPropsModel === 'object' && this.atomPropsModel.input
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
