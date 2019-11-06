<template>
    <div class="bk-form bk-form-vertical">
        <form-field v-for="(obj, key) in newModel" :key="key" :desc="obj.desc" :required="obj.required" v-if="!obj.hidden" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)" :class="{ 'inline-warpper': key === 'relativeDate' }">
            <component :is="obj.component"
                :name="key"
                v-validate.initial="Object.assign({}, obj.rule, { required: obj.required })"
                :handle-change="(key === 'enableGroupId') ? handleChoose : (key === 'timeType') ? updataDateType : handleUpdateElement"
                :value="element[key]" v-bind="obj">
            </component>
            <span class="inline-span" v-if="key === 'relativeDate'">{{obj.inlineText}}</span>
        </form-field>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'experience',
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
            if (this.element.expireDate) {
                this.handleUpdateElement(element.timeType === 'RELATIVE' ? 'relativeDate' : 'absoluteDate', element.expireDate)
            }
            this.updataDateType('timeType', element.timeType)
            this.handleChoose('enableGroupId', element.enableGroupId)
        },
        destroyed () {
            const { element } = this
            if (element.timeType === 'RELATIVE') {
                this.handleUpdateElement('expireDate', element.relativeDate)
            } else {
                this.handleUpdateElement('expireDate', element.absoluteDate)
            }
            const wechatGroup = element.groupId
            // 结尾是否为 ';'
            if (wechatGroup && wechatGroup.charAt(wechatGroup.length - 1) !== ';') {
                this.handleUpdateElement('groupId', wechatGroup + ';')
            }
            element.groupId.split(';').filter(item => item).forEach(item => {
                if (!this.groupIdStorage.includes(item)) {
                    this.groupIdStorage.push(item)
                }
            })
            localStorage.setItem('groupIdStr', this.groupIdStorage.sort().join(';'))
        },
        methods: {
            handleChoose (name, value) {
                value ? this.newModel.groupId.hidden = false : this.newModel.groupId.hidden = true
                this.handleUpdateElement(name, value)
            },
            updataDateType (name, value) {
                if (value === 'RELATIVE') {
                    this.newModel.relativeDate.hidden = false
                    this.newModel.absoluteDate.hidden = true
                } else {
                    this.newModel.relativeDate.hidden = true
                    this.newModel.absoluteDate.hidden = false
                }
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
