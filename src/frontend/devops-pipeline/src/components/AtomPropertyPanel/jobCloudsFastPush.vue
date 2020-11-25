<template>
    <div class="pull-code-panel bk-form bk-form-vertical">
        <section v-if="hybridId">
            <template v-for="(obj, key) of newModel">
                <form-field v-if="!obj.hidden" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="(key === 'srcType') ? handleChooseSrcType : handleUpdateElement" :value="element[key]" v-bind="obj"></component>
                    <route-tips :visible="true" :src-tips="srcTips" :path-tips="''" v-if="key === 'srcPath' && element['srcPath']"></route-tips>
                </form-field>
            </template>
        </section>
        <section v-else>
            <div class="empty-tips">{{ $t('editPage.atomForm.noHyAppidTips') }}</div>
        </section>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'

    export default {
        name: 'jobCloudsFastPush',
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
            hybridId () {
                return this.$store.state.curProject.hybridCcAppId || ''
            },
            srcTips () {
                let prefix = ''
                if (this.element.srcType === 'PIPELINE') {
                    prefix = this.$t('details.artifactory') + '/${pipeline.name}/${pipeline.build.id}/'
                } else if (this.element.srcType === 'CUSTOMIZE') {
                    prefix = `${this.$t('details.artifactory')}/`
                } else {
                    return ''
                }
                if (this.element.srcPath === '' || this.element.srcPath === './') {
                    return prefix
                } else {
                    let path = this.element.srcPath
                    if (path.startsWith('./')) {
                        path = path.slice(2)
                    }
                    return prefix + path
                }
            }
        },
        async mounted () {
            this.newModel = JSON.parse(JSON.stringify(this.atomPropsModel))
            this.handleUpdateElement('targetAppId', this.hybridId)
            this.handleChooseSrcType('srcType', this.element.srcType)
        },
        methods: {
            handleChooseSrcType (name, value) {
                if (value === 'PIPELINE') {
                    this.newModel.srcPath.placeholder = this.$t('editPage.atomForm.pipelinePathTips')
                } else {
                    this.newModel.srcPath.placeholder = this.$t('editPage.atomForm.customPathTips')
                }
                this.handleUpdateElement(name, value)
            }
        }
    }
</script>
