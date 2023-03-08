<template>
    <div class="bk-form bk-form-vertical tcls-panel">
        <form-field v-for="(obj, key) in newModel" :key="key" :desc="obj.desc" :required="obj.required" v-if="!obj.hidden" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
            <component :is="obj.component" :class="obj.class" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: obj.required })" :handle-change="(key === 'mtclsApp') ? handleChoose : handleUpdateElement" :value="element[key]" v-bind="obj"></component>
            <bk-button inline="true" v-if="key === 'envId'" @click="handleGetEnvId">{{ $t('editPage.atomForm.reflash') }}</bk-button>
        </form-field>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'tcls',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {}
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        mounted () {
            this.newModel = this.atomPropsModel
            if (this.element.ticketId) {
                this.handleChoose('mtclsApp', this.element.mtclsApp, false)
                this.handleGetEnvId()
            }
        },
        destroyed () {
            if (!this.element.mtclsApp || this.element.mtclsApp === 'TCLS') {
                this.handleUpdateElement('updatePkgType', '')
            }
            if (this.element.hashMd5 === '') {
                this.handleUpdateElement('hashMd5', 'null')
            }
            if (this.element.hashUrl === '') {
                this.handleUpdateElement('hashUrl', 'null')
            }
        },
        methods: {
            handleChoose (name, value, resetEnvId = true) {
                this.newModel.envId.list = []
                if (resetEnvId) {
                    this.handleUpdateElement('envId', '')
                }
                if (value === true || value === 'MTCLS') {
                    this.newModel.updatePkgType.hidden = false
                    this.newModel.serviceId.hidden = false
                    this.newModel.tclsAppId.hidden = true
                } else {
                    this.newModel.updatePkgType.hidden = true
                    this.newModel.serviceId.hidden = true
                    this.newModel.tclsAppId.hidden = false
                }
                this.handleUpdateElement(name, value)
            },
            async handleGetEnvId () {
                const tclsAppId = this.element.tclsAppId
                const ticketId = this.element.ticketId
                const mtclsApp = this.element.mtclsApp
                const serviceId = this.element.serviceId
                if (ticketId === '') {
                    this.$showTips({
                        message: this.$t('editPage.atomForm.ticketId'),
                        theme: 'error'
                    })
                    return
                }
                if (!tclsAppId && (mtclsApp === false || mtclsApp === 'TCLS')) {
                    this.$showTips({
                        message: this.$t('editPage.atomForm.tclsId'),
                        theme: 'error'
                    })
                    return
                }
                if (!serviceId && (mtclsApp === false || mtclsApp === 'MTCLS')) {
                    this.$showTips({
                        message: this.$t('editPage.atomForm.serviceId'),
                        theme: 'error'
                    })
                    return
                }
                try {
                    this.newModel.envId.placeholder = this.$t('editPage.atomForm.loadingTips')
                    // this.newModel.envId.disabled = true
                    const url = `/process/api/user/tcls/${this.projectId}/getEnvList?tclsAppId=${tclsAppId}&ticketId=${ticketId}&mtclsApp=${mtclsApp}&serviceId=${serviceId}`
                    const res = await this.$ajax.get(url)
                    if (res.data) {
                        this.newModel.envId.list = res.data.map(item => ({
                            id: item.envId,
                            name: item.envName
                        }))
                    }
                } catch (e) {
                    this.$showTips({
                        message: e.message || e,
                        theme: 'error'
                    })
                } finally {
                    this.newModel.envId.placeholder = this.$t('editPage.atomForm.selectTips')
                    this.newModel.envId.disabled = false
                }
            }
        }
    }
</script>

<style lang="scss">
    .tcls-panel {
        .inline {
            display: inline-block;
            width: 86%;
        }
        .bk-form-help {
            display: inline-block;
        }
        button {
            margin: 0 0 3px 5px;
        }
    }
</style>
