<template>
    <div class='pull-code-panel bk-form bk-form-vertical'>
        <form-field v-if="!obj.hidden" v-for='(obj, key) of newModel' :key='key' :desc='obj.desc' :required='obj.required' :label='obj.label' :is-error='errors.has(key)' :errorMsg='errors.first(key)'>
            <component :is='obj.component' :name='key' v-validate.initial='Object.assign({}, obj.rule, { required: !!obj.required })' :handleChange="(key === 'fileSource') ? handleChoose : handleUpdateElement" :value='element[key]' v-bind='obj'></component>
            <route-tips :srcTips="srcTips" :pathTips="''" v-if="key === 'filePath' && element['filePath']"></route-tips>
            <p style="font: 12px" v-if="key === 'packageName'"><a class="text-link" target='_blank' :href='linkUrl'>{{ $t('editPage.atomForm.zhiyunCreateTips') }}</a></p>
        </form-field>
    </div>
</template>

<script>
    import { mapState, mapActions, mapGetters } from 'vuex'
    import atomMixin from './atomMixin'
    import {bus} from '../../utils/bus'
    import validMixins from '../validMixins'

    export default {
        name: 'select-artifactory-file',
        mixins: [ atomMixin, validMixins ],
        data () {
            return {
                linkUrl: `${ZHIYUN_URL}/package/create`,
                newModel: {}
            }
        },
        watch: {
            atomPropsModel (val, oldval) {
                this.newModel = JSON.parse(JSON.stringify(val))
                this.handleChoose('fileSource', this.element.fileSource)
            }
        },
        computed: {
            srcTips () {
                let prefix = `${this.$t('details.artifactory')}/${PIPELINE_NAME}/${BUILD_ID}/`
                if (this.element.fileSource !== 'PIPELINE') {
                    prefix = `${this.$t('details.artifactory')}/`
                }
                if (this.element.filePath === '' || this.element.filePath === './') {
                    return prefix
                } else {
                    let path = this.element.filePath
                    if (path.startsWith('./')) {
                        path = path.slice(2)
                    }
                    return prefix + path
                }
            }
        },
        methods: {
            handleChoose (name, value) {
                if (value === 'PIPELINE') {
                    this.newModel.filePath.placeholder = this.$t('editPage.atomForm.pipelinePathTips')
                } else {
                    this.newModel.filePath.placeholder = this.$t('editPage.atomForm.customPathTips')
                }
                this.handleUpdateElement(name, value)
            }
        },
        mounted () {
            this.newModel = JSON.parse(JSON.stringify(this.atomPropsModel))
            this.handleChoose('fileSource', this.element.fileSource)
        }
    }
</script>
