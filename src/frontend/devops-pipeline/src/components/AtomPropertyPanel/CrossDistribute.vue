<template>
    <div class="bk-form bk-form-vertical">
        <form-field v-for="(obj, key) in newModel" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
            <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.component) }, obj.rule, { required: obj.required })" :handle-change="handleUpdateElement" :value="element[key]" v-bind="obj" :placeholder="getPlaceholder(obj, element)"></component>
            <route-tips v-bind="getComponentTips(obj, element)"></route-tips>
        </form-field>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'cross-distribute',
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
            }
        },
        async mounted () {
            this.newModel = this.atomPropsModel
            await this.requestProjectList()
        },
        methods: {
            async requestProjectList () {
                this.newModel.targetProjectId.isLoading = true
                const tmpList = []
                try {
                    const url = '/project/api/user/projects/'
                    const res = await this.$ajax.get(url)
                    if (res.data && res.data.length > 0) {
                        for (let i = 0; i < res.data.length; i++) {
                            if (res.data[i].projectCode !== this.projectId && res.data[i].approvalStatus === 0) {
                                tmpList.push(Object.assign({}, { id: res.data[i].projectCode, name: res.data[i].projectName }))
                            }
                        }
                        this.newModel.targetProjectId.list = tmpList
                        this.list = tmpList
                    }
                } catch (e) {
                    this.$showTips({
                        theme: 'error',
                        message: e.message || e
                    })
                } finally {
                    this.newModel.targetProjectId.isLoading = false
                }
            }
        }
    }
</script>
