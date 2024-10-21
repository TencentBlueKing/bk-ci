<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in atomPropsModel">
            <template v-if="obj.type === 'group'">
                <form-field-group
                    v-if="rely(obj, element)"
                    :name="key"
                    :value="element[key]"
                    :handle-change="handleMethods"
                    :key="key"
                    v-bind="obj"
                >
                    <template v-for="(i, index) in obj.children">
                        <form-field
                            :key="i.key"
                            v-if="rely(i, element)"
                            v-bind="i"
                            :is-error="errors.has(i.key)"
                            :error-msg="errors.first(i.key)"
                        >
                            <component
                                :is="i.component"
                                :name="i.key"
                                v-validate.initial="Object.assign({}, { max: getMaxLengthByType(i.component) }, i.rule, { required: !!i.required })"
                                :handle-change="i.key === 'eventType' ? handleBlockEnable : handleMethods"
                                :value="element[i.key] || atomPropsModel[key]?.children[index]?.default"
                                :disabled="disabled"
                                v-bind="i"
                            >
                            </component>
                        </form-field>
                    </template>
                </form-field-group>
            </template>
            <template v-else>
                <form-field
                    v-if="!obj.hidden && rely(obj, element)"
                    :key="key"
                    v-bind="obj"
                    :is-error="errors.has(key)"
                    :error-msg="errors.first(key)"
                >
                    <component
                        :is="obj.component"
                        :name="key"
                        v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.component) }, obj.rule, { required: !!obj.required })"
                        :handle-change="key === 'eventType' ? handleBlockEnable : handleMethods"
                        :value="element[key]"
                        :element="element"
                        :disabled="disabled"
                        v-bind="obj"
                    >
                    </component>
                </form-field>
            </template>
        </template>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'code-git-lab-hook-trigger',
        mixins: [atomMixin, validMixins],
        created () {
            if (!this.atomPropsModel?.userSettings) {
                if (!this.element.repositoryType) {
                    this.handleUpdateElement('repositoryType', 'ID')
                }
                this.handleChooseCodelibType('repositoryType', this.element.repositoryType)
            }
        },
        methods: {
            handleBlockEnable (name, value) {
                this.handleUpdateElement(name, value)
            },
            handleMethods (name, value) {
                if (name === 'repositoryType') {
                    this.handleChooseCodelibType(name, value)
                } else {
                    this.handleUpdateElement(name, value)
                }
            },
            handleChooseCodelibType (name, value) {
                if (!this.atomPropsModel?.userSettings) {
                    if (value === 'ID') {
                        this.atomPropsModel.repositoryHashId.hidden = false
                        this.atomPropsModel.repositoryName.hidden = true
                    } else if (value === 'NAME') {
                        this.atomPropsModel.repositoryHashId.hidden = true
                        this.atomPropsModel.repositoryName.hidden = false
                    }
                }
                this.handleUpdateElement(name, value)
            }
        }
    }
</script>
