<template>
    <div v-bkloading="{ isLoading: !pipelineSetting }" class="bkdevops-clean-policy-setting-tab">
        <p class="hint">
            <i class="devops-icon icon-info-circle-shape"></i>
            {{ $t('settings.cleanTips') }}
        </p>
        <!-- <div v-if="pipelineSetting">
            <form-field :required='true' label='保留天数' :is-error='errors.has("keepDays")' :errorMsg='errors.first("keepDays")'>
                <vuex-input inputType="number" placeholder='请输入流水线执行历史保留天线' name="keepDays" :value="pipelineSetting.keepDays"  v-validate.initial='"required|max:30"' :handleChange="handleBaseInfoChange" />
            </form-field>
            <form-field :required='true' label='最少保留构建数' :is-error='errors.has("keepDays")' :errorMsg='errors.first("keepDays")'>
                <vuex-input inputType="number" placeholder='请输入流水线执行历史保留天线' name="keepDays" :value="pipelineSetting.keepDays"  v-validate.initial='"required|max:30"' :handleChange="handleBaseInfoChange" />
            </form-field>
        </div> -->
    </div>
</template>

<script>

    export default {
        name: 'bkdevops-clean-policy-setting-tab',
        props: {
            pipelineSetting: Object
        }
    }
</script>

<style lang="scss">
    .bkdevops-clean-policy-setting-tab {
        .hint {
            display: flex;
            align-items: center;
            height: 46px;
            background-color: #FAFBFD;
            padding: 0 15px;
            border-radius: 2px;
            margin-bottom: 20px;
            .devops-icon {
                color: #3C96FF;
                margin-right: 10px;
            }
        }
    }
</style>
