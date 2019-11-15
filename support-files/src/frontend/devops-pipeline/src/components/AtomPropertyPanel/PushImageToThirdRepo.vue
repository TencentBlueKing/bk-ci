<template>
    <div class="bk-form bk-form-vertical">
        <form-field v-for="(obj, key) in newModel" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
            <component :is="obj.component" :container="container" :element="element" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleChange" :value="element[key]" v-bind="obj"></component>
        </form-field>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'pushimage-to-thiedrepo',
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
        created () {
            this.newModel = JSON.parse(JSON.stringify(this.atomPropsModel))
            this.requestImageList()
            if (this.element.srcImageName) {
                this.requestImageTagList(this.element.srcImageName)
            }
        },
        methods: {
            async requestImageList () {
                try {
                    const res = await this.$store.dispatch('soda/getImageList', {
                        projectId: this.projectId
                    })
                    if (res && res.imageList.length) {
                        this.newModel.srcImageName.list = res.imageList
                    }
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            async requestImageTagList (imageRepo) {
                try {
                    const res = await this.$store.dispatch('soda/getImageTagList', {
                        repo: imageRepo
                    })
                    if (res && res.tags.length) {
                        this.newModel.srcImageTag.list = res.tags
                    }
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            handleChooseImage (name, value, isUpdate) {
                if (isUpdate) {
                    const isOption = this.newModel.srcImageName.list.find(item => item.name === value)
                    if (isOption) {
                        this.requestImageTagList(isOption.repo)
                    }
                    this.handleUpdateElement('srcImageTag', '')
                    this.newModel.srcImageTag.list = []
                }
                this.handleUpdateElement(name, value)
            },
            handleChange (name, value, isUpdate) {
                if (name === 'srcImageName') {
                    this.handleChooseImage(name, value, isUpdate)
                } else {
                    this.handleUpdateElement(name, value)
                }
            }
        }
    }
</script>
