<template>
    <div class="bk-form bk-form-vertical">
        <section v-if="appId">
            <form-field :desc="$t('editPage.atomForm.appidDesc')" :label="$t('editPage.atomForm.appidLabel')">
                <vuex-input readonly :value="appName" disabled />
            </form-field>
            <template v-for="(obj, key) of atomPropsModel">
                <form-field v-if="!isHidden(obj, element)" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateElement" :value="element[key]" v-bind="obj" :placeholder="getPlaceholder(obj, element)"></component>
                    <route-tips v-bind="getComponentTips(obj, element)"></route-tips>
                </form-field>
            </template>
        </section>
        <section v-else>
            <div class="empty-tips">{{ $t('editPage.atomForm.noAppidTips') }}</div>
        </section>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'

    export default {
        mixins: [atomMixin, validMixins],
        computed: {
            curProject () {
                return this.$store.state.curProject
            },
            appId () {
                return this.curProject ? this.curProject.ccAppId : ''
            },
            hasAppId () {
                return !!this.appId
            },
            appName () {
                return this.curProject ? this.curProject.ccAppName : ''
            },
            isGseAtom () {
                return ['gseKitProcRunCmdDev', 'gseKitProcRunCmdProd'].includes(this.atomCode)
            },
            atomCode () {
                return this.element.atomCode
            },
            atomVersion () {
                return this.element.version
            }
        },
        watch: {
            atomCode () {
                this.initAppId()
            },
            atomVersion () {
                this.initAppId()
            }
        },
        mounted () {
            this.initAppId()
        },
        destroyed () {
            if (this.isGseAtom && this.element.showParams !== undefined) {
                this.handleUpdateElement('params', this.element.showParams.split(' '))
            }
        },
        methods: {
            initAppId () {
                if (['openStatePushFile', 'cloudStone'].includes(this.atomCode)) { // hack ccAppId
                    this.handleUpdateElement('ccAppId', this.appId)
                } else if (this.isGseAtom) {
                    this.handleUpdateElement('appId', this.appId)

                    if (this.appId) {
                        if (this.element.params) {
                            this.handleUpdateElement('showParams', this.element.params.join(' '))
                        }
                        if (this.element.envId) {
                            this.handleUpdateElement('envId', this.element.envId.toString())
                        }
                    }
                } else {
                    this.handleUpdateElement('appid', this.appId)
                }
            }
        }
    }
</script>
