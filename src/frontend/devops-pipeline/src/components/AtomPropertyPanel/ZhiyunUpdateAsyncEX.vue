<template>
    <div class="bk-form bk-form-vertical">
        <form-field v-for="(obj, key) in commonModel[&quot;rows&quot;]" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
            <component :is="obj.component" :container="container" :element="element" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateElement" :value="element[key]" v-bind="obj"></component>
        </form-field>

        <accordion show-checkbox :show-content="element.graceful" :after-toggle="toggleUpgrade" :condition="true">
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

        <accordion show-checkbox show-content>
            <header class="var-header" slot="header">
                <span>{{ $t('editPage.atomForm.otherChoice') }}</span>
            </header>
            <div slot="content">
                <form-field v-for="key in otherModel" v-if="!newModel[key].hidden" :key="key" :desc="newModel[key].desc" :required="newModel[key].required" :label="newModel[key].label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
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
                upgradeModel: ['batchNum', 'batchInterval'],
                otherModel: ['force', 'stop', 'restart', 'updateAppName', 'showUpdatePort', 'showUpdateStartStop']
            }
        },
        computed: {
            commonModel () {
                const { product, pkgName, fromVersion, toVersion, installPath, ips, route } = this.newModel
                return {
                    rows: { product, pkgName, fromVersion, toVersion, installPath, ips, route }
                }
            }
        },
        created () {
            this.newModel = JSON.parse(JSON.stringify(this.atomPropsModel))
            this.handleUpdateElement('showUpdatePort', !this.element.updatePort)
            this.handleUpdateElement('showUpdateStartStop', !this.element.updateStartStop)
        },
        destroyed () {
            this.handleUpdateElement('updatePort', !this.element.showUpdatePort)
            this.handleUpdateElement('updateStartStop', !this.element.showUpdateStartStop)
        },
        methods: {
            toggleUpgrade (element, show) {
                this.handleUpdateElement('graceful', show)
            }
        }
    }
</script>
