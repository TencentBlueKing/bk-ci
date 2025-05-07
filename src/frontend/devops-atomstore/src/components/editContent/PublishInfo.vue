<template>
    <div class="container">
        <p class="form-title"> {{ $t('store.发布信息') }} </p>
        <div class="form-item-container">
            <div class="bk-form-item name-form-item is-required">
                <label class="bk-label"> {{ $t('store.发布者') }} </label>
                <div class="bk-form-content template-item-content">
                    <div style="width: 40%;">
                        <input
                            type="text"
                            class="bk-form-input"
                            :placeholder="$t('store.请输入')"
                            name="publisher"
                            v-model="templateForm.publisher"
                            v-validate="{
                                required: true,
                                max: 20
                            }"
                            :class="{ 'is-danger': errors.has('publisher') }"
                        >
                        <p :class="errors.has('publisher') ? 'error-tips' : 'normal-tips'">{{ errors.first("publisher") }}</p>
                    </div>
                </div>
            </div>
            <div class="bk-form-item versionlog-form-item">
                <label class="bk-label"> {{ $t('store.发布描述') }} </label>
                <div class="bk-form-content template-item-content">
                    <textarea
                        type="text"
                        class="bk-form-input template-versionlog-input"
                        placeholder=""
                        v-model="templateForm.pubDescription"
                    >
                    </textarea>
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
