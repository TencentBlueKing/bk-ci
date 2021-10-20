<template>
    <form class="bk-form bk-form-vertical">
        <form-field v-for="(obj, key) in atomPropsModel" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
            <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateElement" :value="element[key]" v-bind="obj"></component>
        </form-field>
    </form>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'plugin-atom',
        mixins: [atomMixin, validMixins],
        mounted () {
            // @todo 解析data到属性中
            if (this.element.data) {
                const data = JSON.parse(this.element.data)
                for (const obj in data) {
                    this.handleUpdateElement(obj, data[obj])
                }
            }
        },
        destroyed () {
            // 组装data数据
            const ele = {}
            Object.keys(this.atomPropsModel).forEach(key => {
                ele[key] = this.element[key]
            })
            this.handleUpdateElement('data', JSON.stringify(ele))
        },
        methods: {
            handleUpdateElement (name, value) {
                this.updateElement({
                    element: this.element,
                    newParam: {
                        [name]: value
                    }
                })
            }
        }
    }
</script>
