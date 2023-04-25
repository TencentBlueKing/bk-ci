<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in newModel">
            <form-field v-if="!isHidden(obj, element) && !obj.hidden" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :element="element" :class="obj.class" :name="key" v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.component) }, obj.rule, { required: obj.required })" :handle-change="handleUpdateElement" :value="element[key]" v-bind="obj"></component>
            </form-field>
        </template>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'subPipelineCall',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {}
            }
        },
        created () {
            this.newModel = JSON.parse(JSON.stringify(this.atomPropsModel))
            if (!this.element.subPipelineType) {
                this.handleUpdateElement('subPipelineType', 'ID')
            }
            if (this.element.subPipelineType === 'NAME' && !this.element.inputParam) {
                let inputParams = ''
                const obj = this.element.parameters
                for (const key in obj) {
                    inputParams += `${key}=${obj[key]}\n`
                }
                if (inputParams.endsWith('\n')) inputParams = inputParams.slice(0, -1)
                this.handleUpdateElement('inputParam', inputParams)
            }
        },
        destroyed () {
            if (this.element.subPipelineType === 'NAME') {
                const paramsMap = {}
                if (this.element.inputParam) {
                    const rowArr = this.element.inputParam.split('\n')
                    rowArr.forEach(row => {
                        const itemArr = row.split('=')
                        if (itemArr[0]) {
                            Object.assign(paramsMap, { [itemArr[0]]: itemArr[1] || '' })
                        }
                    })
                }
                this.handleUpdateElement('parameters', typeof paramsMap === 'object' ? paramsMap : {})
            }
        },
        methods: {
        }
    }
</script>
