<template>
    <div class="bk-form bk-form-vertical">
        <section v-if="!isThirdParty">
            <form-field v-for="(obj, key) in newModel" :key="key" :desc="obj.desc" :required="obj.required" v-if="!obj.hidden" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: obj.required })" :handle-change="handleUpdateElement" :value="element[key]" v-bind="obj"></component>
            </form-field>
        </section>
        <section v-else>
            <div class="empty-tips">第三方构建机暂不支持IOS证书安装插件</div>
        </section>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'ios-cert-install',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {}
            }
        },
        created () {
            this.newModel = this.atomPropsModel
        }
    }
</script>
