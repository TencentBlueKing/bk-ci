<template>
    <div class="pull-code-panel bk-form bk-form-vertical">
        <section v-if="appId">
            <form-field :desc="$t('editPage.atomForm.appidDesc')" :label="$t('editPage.atomForm.appidLabel')">
                <vuex-input readonly :value="appName" disabled />
            </form-field>
            <form-field v-if="!obj.hidden" v-for="(obj, key) of newModel" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="key === 'tcmAppId' ? handleSelectTcmId : (key === 'templateId' ? handleSelectTemplate : handleUpdateElement)" :value="element[key]" v-bind="obj"></component>
            </form-field>
        </section>
        <section v-if="!appId">
            <div class="empty-tips">{{ $t('editPage.atomForm.noAppidTips') }}</div>
        </section>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'

    export default {
        name: 'tcm',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {}
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            appId () {
                return this.$store.state.curProject.ccAppId || ''
            },
            appName () {
                return this.$store.state.curProject.ccAppName || ''
            }
        },
        mounted () {
            this.handleUpdateElement('appId', this.appId)
            if (this.appId) {
                this.newModel = this.atomPropsModel
            }
            if (this.element.tcmAppId) {
                this.handleSelectTcmId('tcmAppId', this.element.tcmAppId)
            }
        },
        methods: {
            async handleSelectTcmId (name, value) {
                this.handleUpdateElement(name, value)
                if (value && this.appId) {
                    const res = await this.$store.dispatch('common/getTcmTemplate', {
                        appId: this.appId,
                        tcmId: value
                    })
                    const list = []
                    if (res && res.length) {
                        for (let i = 0; i < res.length; i++) {
                            list.push(Object.assign({}, { id: res[i].templateId, name: res[i].templateName }))
                        }
                        this.newModel.templateId.list = list
                    }
                }
            },
            async handleSelectTemplate (name, value) {
                this.handleUpdateElement(name, value)
                if (value && this.appId && this.element.tcmAppId) {
                    const res = await this.$store.dispatch('common/getTcmTemplateParam', {
                        appId: this.appId,
                        tcmId: this.element.tcmAppId,
                        templateId: value
                    })
                    const list = []
                    if (res && res.length) {
                        for (let i = 0; i < res.length; i++) {
                            list.push(Object.assign({}, { key: res[i].paramName, value: '', seq: res[i].seq }))
                        }
                    }
                    this.handleUpdateElement('workJson', list)
                }
            }
        }
    }
</script>
