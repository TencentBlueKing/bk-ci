<template>
    <div class="container">
        <p class="form-title"> {{ $t('store.发布信息') }} </p>
        <div class="form-item-container">
            <div class="bk-form-item versionlog-form-item is-required">
                <label class="bk-label"> {{ $t('store.发布描述') }} </label>
                <div class="bk-form-content template-item-content">
                    <textarea
                        type="text"
                        class="bk-form-input template-versionlog-input"
                        name="pubDescription"
                        v-model="templateForm.pubDescription"
                        v-validate="{
                            required: true
                        }"
                        :class="{ 'is-danger': errors.has('pubDescription') }"
                    >
                    </textarea>
                    <p :class="errors.has('pubDescription') ? 'error-tips' : 'normal-tips'">{{ errors.first("pubDescription") }}</p>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'PublishInfo',
        props: {
            templateForm: {
                type: Object,
                required: true,
                default: () => {}
            },
            type: String
        },
        methods: {
            async validate () {
                return await this.$validator.validate()
            }
        }
    
    }
</script>
