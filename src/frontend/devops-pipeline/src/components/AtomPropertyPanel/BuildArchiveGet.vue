<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in newModel">
            <form-field v-if="!obj.hidden" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: obj.required })" :handle-change="(key === 'buildNoType') ? handleChoose : (key === 'pipelineId') ? handleSelect : handleUpdateElement" :value="element[key]" v-bind="obj"></component>
                <route-tips :visible="true" :src-tips="srcTips" :path-tips="pathTips" v-if="key === 'destPath' && element['srcPaths']"></route-tips>
            </form-field>
        </template>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'build-archive-get',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {},
                list: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            srcTips () {
                const srcItem = this.list.find(item => item.id === this.element.pipelineId)
                const name = srcItem ? srcItem.name : '${pipeline.name}'
                const buildId = (this.element.buildNoType === 'ASSIGN' && this.element.buildNo) ? this.element.buildNo : '${pipeline.build.id}'
                const srcPrefix = `版本仓库/${name}/${buildId}/`
                if (this.element.srcPaths === '' || this.element.srcPaths === './') {
                    return srcPrefix
                } else {
                    let path = this.element.srcPaths
                    path = this.handlePath(path)
                    return srcPrefix + path
                }
            },
            pathTips () {
                let srcPath = this.element.srcPaths
                srcPath = this.handlePath(srcPath, true)
                if (this.element.destPath === '' || this.element.destPath === './') {
                    return ' ${WORKSPACE}/' + srcPath
                } else {
                    let destPath = this.element.destPath
                    destPath = this.handlePath(destPath)
                    return ' ${WORKSPACE}/' + destPath + '/' + srcPath
                }
            }
        },
        async mounted () {
            this.newModel = JSON.parse(JSON.stringify(this.atomPropsModel))

            if (this.element.pipelineId) {
                this.handleSelect('pipelineId', this.element.pipelineId, true)
                this.handleChoose('buildNoType', this.element.buildNoType)
            }
        },
        methods: {
            handleChoose (name, value) {
                if (value === 'ASSIGN') {
                    this.newModel.buildNo.hidden = false
                } else {
                    this.newModel.buildNo.hidden = true
                    this.handleUpdateElement('buildNo', '')
                }
                this.handleUpdateElement(name, value)
            },
            async handleSelect (name, value, init = false) {
                this.handleUpdateElement(name, value)
                // 切换流水线时，重置指定构建号
                if (!init) {
                    this.handleUpdateElement('buildNo', '')
                }
                try {
                    const url = '/process/api/user/archive/' + this.projectId + '/pipelines/' + value + '/getAllBuildNo?page=1&pagesize=-1'
                    const res = await this.$ajax.get(url)
                    const buildNoList = []
                    if (res.data) {
                        for (let i = 0; i < res.data.length; i++) {
                            buildNoList.push(Object.assign({}, { id: res.data[i]['key'], name: res.data[i]['key'] }))
                        }
                        this.newModel.buildNo.list = buildNoList
                    }
                } catch (e) {
                    if (e.code === 403) {
                        if (this.element.buildNoType === 'ASSIGN' && this.element.buildNo) {
                            this.newModel.buildNo.list = [{ id: this.element.buildNo, name: '******（无权限查看）' }]
                        }
                    } else {
                        this.$showTips({
                            theme: 'error',
                            message: e.message || e
                        })
                    }
                } finally {
                    this.isLoading = false
                }
            }
        }
    }
</script>
