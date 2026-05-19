<template>
    <div class="resource-dependency">
        <h3>{{ $t('resourceDependency') }}</h3>
        <!-- 处理资源依赖的内容 -->
        <div class="resource-form">
            <div class="form-item">
                <label>{{ $t('selectResources') }}</label>
                <bk-select
                    multiple
                    :value="formData.resourceDependency.selectedResources"
                    @change="handleUpdate('resourceDependency', 'selectedResources', $event)"
                    :placeholder="$t('pleaseSelectResource')"
                >
                    <bk-option
                        v-for="resource in resourceList"
                        :key="resource.id"
                        :id="resource.id"
                        :name="resource.name"
                    />
                </bk-select>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'ResourceDependency',
        props: {
            formData: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                resourceList: [
                    { id: 'repo', name: this.$t('codeRepository') },
                    { id: 'credential', name: this.$t('credential') },
                    { id: 'variable', name: this.$t('variable') }
                ]
            }
        },
        methods: {
            handleUpdate (stepName, field, value) {
                this.$emit('update-form-data', stepName, field, value)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .resource-dependency {
        padding: 20px;
    }
    .resource-form {
        margin-top: 20px;
    }
    .form-item {
        margin-bottom: 20px;
        
        label {
            display: block;
            margin-bottom: 8px;
            font-weight: 500;
            color: #63656E;
        }
    }
</style>
