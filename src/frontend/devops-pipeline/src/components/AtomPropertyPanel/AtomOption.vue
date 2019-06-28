<template>
    <accordion show-checkbox show-content key="otherChoice">
        <header class="var-header" slot="header">
            <span>流程控制选项</span>
            <i class="bk-icon icon-angle-down" style="display:block"></i>
        </header>
        <div slot="content" class="bk-form bk-form-vertical">
            <template v-for="(obj, key) in optionModel">
                <form-field :key="key" v-if="(!isHidden(obj, element) && container['@type'] !== 'trigger') || key === 'enable'" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component :is="obj.component" :container="container" :element="element" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateElementOption" :value="atomOption[key]" v-bind="obj"></component>
                </form-field>
            </template>
        </div>
    </accordion>
</template>

<script>
    import { mapActions } from 'vuex'
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import {
        getAtomOptionDefault,
        ATOM_OPTION
    } from '@/store/modules/soda/optionConfig'
    export default {
        name: 'atom-config',
        mixins: [atomMixin, validMixins],
        computed: {
            atomOption () {
                return this.element.additionalOptions || {}
            },
            atomCode () {
                return this.element.atomCode
            },
            atomVersion () {
                return this.element.version
            },
            optionModel () {
                return ATOM_OPTION || {}
            }
        },
        watch: {
            atomCode () {
                this.initOptionConfig()
            },
            atomVersion () {
                this.initOptionConfig()
            }
        },
        created () {
            this.initOptionConfig()
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing'
            ]),
            getAtomOptionDefault,
            handleUpdateElementOption (name, value) {
                this.setPipelineEditing(true)
                this.handleUpdateElement('additionalOptions',
                                         Object.assign(this.element.additionalOptions || {}, { [name]: value })
                )
            },
            initOptionConfig () {
                if (this.element.additionalOptions === undefined) {
                    this.handleUpdateElement('additionalOptions', this.getAtomOptionDefault())
                }
            }
        }
    }
</script>
