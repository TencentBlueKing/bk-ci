<template>
    <div class="svnpath-item">
        <vuex-input v-bind="$props" />
        <span class="view-input-path" v-if="fullPath">代码真实拉取路径：{{ fullPath }}</span>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import VuexInput from '../VuexInput'

    export default {
        name: 'svnpath-input',
        components: {
            VuexInput
        },
        mixins: [atomFieldMixin],
        props: {
            repositoryHashId: {
                type: String
            },
            value: {
                type: String
            },
            list: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                fullPath: '',
                rootPath: ''
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            value (val) {
                this.fullPath = ''
                if (val && this.rootPath) {
                    this.setFullPath(this.rootPath, val)
                }
            },
            rootPath (val) {
                this.fullPath = ''
                if (val && this.value) {
                    this.setFullPath(val, this.value)
                }
            },
            repositoryHashId (val) {
                this.rootPath = ''
                this.setRootPath(val)
            },
            list: {
                deep: true,
                handler: function (val, oldVal) {
                    (this.rootPath === '' || oldVal.length === 0) && this.repositoryHashId && this.setRootPath(this.repositoryHashId)
                }
            }
        },
        async created () {
            this.repositoryHashId && this.list.length && this.setRootPath(this.repositoryHashId)
        },
        methods: {
            setRootPath (hashId) {
                const items = this.list.filter(item => item.repositoryHashId === hashId)
                if (items.length) {
                    this.rootPath = items[0].url || ''
                }
            },
            setFullPath (rootPath, path) {
                this.fullPath = rootPath.replace(/\/$/, '') + '/' + path.replace(/^\//, '')
            }
        }
    }
</script>

<style lang='scss'>
    .view-input-path {
        padding-top: 5px;
        display: block;
        word-break: break-all;
    }
</style>
