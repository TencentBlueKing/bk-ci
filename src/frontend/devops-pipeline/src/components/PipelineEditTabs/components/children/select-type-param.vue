<template>
    <div class="selector-type-param">
        <div class="option-type">
            <div class="type-select" :class="{ 'is-active': selectType === 'options' }" @click="selectType = 'options'">预定义选项</div>
            <div class="type-select" :class="{ 'is-active': selectType === 'remote' }" @click="selectType = 'remote'">从接口获取选项</div>
        </div>
        <div class="option-items">
            <section v-if="selectType === 'options'">
                <key-options :options="param.options" :handle-change-options="handleUpdate" />
            </section>
            <section v-else>
                <bk-form form-type="vertical" class="new-ui-form" :label-width="300">
                    <template v-for="obj in remoteTypeOptions">
                        <form-field :key="obj.key" :desc="obj.tips" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                            <component :is="'vuex-input'" :disabled="disabled" :name="key" v-validate.initial="Object.assign({}, { required: !!obj.required })" :handle-change="handleRemoteParamChange" :value="remoteTypeValue[obj.key]" v-bind="obj" :placeholder="obj.placeholder"></component>
                        </form-field>
                    </template>
                </bk-form>
            </section>
        </div>
    </div>
</template>

<script>
    import KeyOptions from './key-options'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import VuexInput from '@/components/atomFormField/VuexInput'
    export default {
        components: {
            KeyOptions,
            FormField,
            VuexInput
        },
        props: {
            disabled: {
                type: Boolean,
                default: false
            },
            param: {
                type: Object,
                required: true
            },
            handleUpdateOptions: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                remoteTypeValue: {},
                remoteTypeOptions: [
                    {
                        key: 'url',
                        label: '接口URL',
                        placeholder: '请输入',
                        tips: ''
                    },
                    {
                        key: 'resPath',
                        label: '数据对象名称',
                        placeholder: '缺省时默认为data，示例：data.detail.list',
                        tips: ''
                    },
                    {
                        key: 'id',
                        label: '选项ID字段名',
                        placeholder: '选中时将此ID值传递给插件',
                        tips: ''
                    },
                    {
                        key: 'name',
                        label: '选项NAME字段名',
                        placeholder: '用户在页面上看到的选项名称',
                        tips: ''
                    }
                ],
                selectType: 'options'
            }
        },
        computed: {

        },
        created () {
            this.selectType = this.param?.payload?.type || 'options'
            this.remoteTypeValue = Object.assign({}, this.param?.payload || {})
        },
        methods: {
            handleRemoteParamChange (name, value) {
                Object.assign({}, this.remoteTypeValue, { name: value })
                console.log(this.remoteTypeValue, 'remoteTypeVal')
            },
            handleUpdate (name, value) {
                this.handleUpdateOptions(name, value)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .selector-type-param {
        background-color: #FAFBFD;
        margin: 24px 0;
        .option-type {
            height: 40px;
            display: flex;
            align-items: center;
            cursor: pointer;
            border-bottom: 1px solid #DCDEE5;
            .type-select {
                flex: 1;
                font-size: 14px;
                line-height: 22px;
                color: #63656E;
                text-align: center;
            }
            .is-active {
                color: #3A84FF;
                padding: 9px 6px;
                border-bottom: 2px solid #3A84FF;
            }
        }
        .option-items {
            padding: 16px 24px;
        }
    }
</style>
