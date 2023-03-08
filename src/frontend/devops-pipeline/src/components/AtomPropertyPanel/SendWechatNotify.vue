<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in newModel">
            <form-field :key="key" :desc="obj.desc" :required="obj.required" v-if="!obj.hidden" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component"
                    :name="key"
                    v-validate.initial="Object.assign({}, { max: getMaxLengthByType(obj.component) }, obj.rule, { required: obj.required })"
                    :handle-change="(key === 'wechatGroupFlag') ? handleChoose : handleUpdateElement"
                    :value="element[key]" v-bind="obj">
                </component>
            </form-field>
        </template>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'sendRTXNotify',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {},
                groupIdStorage: []
            }
        },
        async mounted () {
            const { element } = this
            this.newModel = this.atomPropsModel
            this.groupIdStorage = localStorage.getItem('groupIdStr') ? localStorage.getItem('groupIdStr').split(';').filter(item => item) : []
            this.handleChoose('wechatGroupFlag', element.wechatGroupFlag)
        },
        destroyed () {
            const { element } = this
            const wechatGroup = element.wechatGroup
            // 结尾是否为 ';'
            if (wechatGroup && wechatGroup.charAt(wechatGroup.length - 1) !== ';') {
                this.handleUpdateElement('wechatGroup', wechatGroup + ';')
            }
            element.wechatGroup.split(';').filter(item => item).forEach(item => {
                if (!this.groupIdStorage.includes(item)) {
                    this.groupIdStorage.push(item)
                }
            })
            localStorage.setItem('groupIdStr', this.groupIdStorage.sort().join(';'))
        },
        methods: {
            handleChoose (name, value) {
                value ? this.newModel.wechatGroup.hidden = false : this.newModel.wechatGroup.hidden = true
                this.handleUpdateElement(name, value)
            }
        }
    }
</script>
<style lang="scss">
    .inline-warpper .bk-form-content{
        position: relative;
        display: table;
        border-collapse: separate;
    }
    .inline-warpper.is-danger .bk-form-content {
        padding-bottom: 18px;
    }
    .inline-warpper.is-danger .is-danger {
        position: absolute;
        bottom: 0;
        left: 0;
        margin: 0;
    }
    .inline-warpper .inline {
        position: relative;
        display: table-cell;
        z-index: 2;
        float: left;
        width: 180px;
        margin-bottom: 0;
        border-top-right-radius: 0;
        border-bottom-right-radius: 0;
    }
    .inline-warpper .inline-span {
        display: table-cell;
        padding: 6px 0;
        width: 80px;
        font-size: 14px;
        font-weight: 400;
        line-height: 1;
        vertical-align: middle;
        text-align: center;
        white-space: nowrap;
        background-color: #f8f9fb;
        border: 1px solid #ccc;
        border-left: none;
        border-top-right-radius: 2px;
        border-bottom-right-radius: 2px;
        cursor: default;
    }
</style>
