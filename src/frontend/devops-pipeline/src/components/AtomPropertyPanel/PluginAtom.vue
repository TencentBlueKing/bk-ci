<template>
    <form class='bk-form bk-form-vertical'>
        <form-field v-for='(obj, key) in atomPropsModel' :key='key' :desc='obj.desc' :required='obj.required' :label='obj.label' :is-error='errors.has(key)' :errorMsg='errors.first(key)'>
            <component :is='obj.component'  :name='key' v-validate.initial='Object.assign({}, obj.rule, { required: !!obj.required })' :handleChange='handleUpdateElement' :value='element[key]' v-bind='obj' ></component>
        </form-field>
    </form>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'plugin-atom',
        mixins: [ atomMixin, validMixins ],
        methods: {
            handleUpdateElement (name, value) {
                this.updateElement({
                    element: this.element,
                    newParam: {
                        [name]: value
                    }
                })
            }
        },
        mounted () {
            // @todo 解析data到属性中
            if (this.element.data) {
                let data = JSON.parse(this.element.data)
                for (let obj in data) {
                    this.handleUpdateElement(obj, data[obj])
                }
            }
        },
        destroyed () {
            // 组装data数据
            let ele = {}
            Object.keys(this.atomPropsModel).forEach(key => {
                ele[key] = this.element[key]
            })
            this.handleUpdateElement('data', JSON.stringify(ele))
        }
    }
</script>
