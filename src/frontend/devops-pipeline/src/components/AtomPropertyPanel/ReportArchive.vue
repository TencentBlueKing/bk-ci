<template>
    <div class="bk-form bk-form-vertical">
        <form-field v-for="(obj, key) in commonModel[&quot;row&quot;]" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
            <component :is="obj.component" :container="container" :element="element" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateElement" :value="element[key]" v-bind="obj"></component>
        </form-field>

        <accordion show-checkbox :show-content="isShowEmailNotice" :after-toggle="toggleEmailNotice" :condition="true">
            <header class="var-header" slot="header">
                <span>同时启用邮件通知</span>
                <input class="accordion-checkbox" type="checkbox" :checked="element.enableEmail" style="margin-left: auto;" />
            </header>
            <div slot="content">
                <form-field v-for="key in optionalModel" v-if="!newModel[key].hidden" :key="key" :desc="newModel[key].desc" :required="newModel[key].required" :label="newModel[key].label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
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
                isShowEmailNotice: false,
                optionalModel: ['emailReceivers', 'emailTitle']
            }
        },
        computed: {
            commonModel () {
                if (this.element['@type'] === 'reportArchiveService') {
                    const { nodeId, fileDir, indexFile, reportName } = this.newModel
                    return {
                        'row': { nodeId, fileDir, indexFile, reportName }
                    }
                } else {
                    const { fileDir, indexFile, reportName } = this.newModel
                    return {
                        'row': { fileDir, indexFile, reportName }
                    }
                }
            }
        },
        created () {
            this.newModel = JSON.parse(JSON.stringify(this.atomPropsModel))
            if (this.element.enableEmail) {
                this.isShowEmailNotice = true
            } else {
                this.handleUpdateElement('enableEmail', false)
                this.handleUpdateElement('emailReceivers', [])
                this.handleUpdateElement('emailTitle', '【${pipeline.name}】  #${pipeline.build.num} 自定义报告已归档')
            }
        },
        methods: {
            toggleEmailNotice (element, show) {
                if (show) {
                    this.handleUpdateElement('enableEmail', true)
                    this.isShowEmailNotice = true
                } else {
                    this.handleUpdateElement('enableEmail', false)
                    this.isShowEmailNotice = false
                }
            }
        }
    }
</script>
