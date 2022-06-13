<template>
    <div class="bk-form bk-form-vertical">
        <!--<form-field v-for='(obj, key) in newModel' v-if='!obj.hidden' :key='key' :desc='obj.desc' :required='obj.required' :label='obj.label' :is-error='errors.has(key)' :errorMsg='errors.first(key)'>
            <component :is='obj.component' v-validate.initial='Object.assign(obj.rule, { required: obj.required })' :name='key' :handleChange='handleUpdateElement' :value='element[key]' v-bind='obj'></component>
        </form-field>-->

        <form-field v-for="(obj, key) in commonModel[&quot;rows&quot;]" v-if="!obj.hidden" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
            <component :is="obj.component" :container="container" :element="element" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="key === &quot;operation&quot; ? handleSelect : handleUpdateElement" :value="element[key]" v-bind="obj"></component>
        </form-field>

        <accordion v-if="element.operation !== 'ROLLBACK'" show-checkbox :show-content="element.graceful" :after-toggle="toggleUpgrade" :condition="true">
            <header class="var-header" slot="header">
                <span>{{ $t('editPage.atomForm.enableUpgrade') }}</span>
                <input class="accordion-checkbox" type="checkbox" :checked="element.graceful" style="margin-left: auto;" />
            </header>
            <div slot="content">
                <form-field v-for="key in upgradeModel" v-if="!newModel[key].hidden" :key="key" :desc="newModel[key].desc" :required="newModel[key].required" :label="newModel[key].label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component :is="newModel[key].component" v-validate.initial="Object.assign({}, newModel[key].rule, { required: newModel[key].required })" :lang="lang" :name="key" :handle-change="handleUpdateElement" :value="element[key]" v-bind="newModel[key]"></component>
                </form-field>
            </div>
        </accordion>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'

    export default {
        name: 'normal-atom',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                upgradeModel: ['batchNum', 'batchInterval']
            }
        },
        computed: {
            commonModel () {
                const { product, pkgName, installPath, ips, operation, curVersion } = this.newModel
                return {
                    rows: { product, pkgName, installPath, ips, operation, curVersion }
                }
            }
        },
        created () {
            this.newModel = JSON.parse(JSON.stringify(this.atomPropsModel))
        },
        destroyed () {
        },
        methods: {
            handleSelect (name, value) {
                this.handleUpdateElement(name, value)
                if (value === 'ROLLBACK') {
                    this.newModel.curVersion.hidden = false
                } else {
                    this.newModel.curVersion.hidden = true
                }
            },
            toggleUpgrade (element, show) {
                this.handleUpdateElement('graceful', show)
            }
        }
    }
</script>
