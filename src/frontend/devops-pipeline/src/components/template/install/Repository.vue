<template>
    <div class="repository-wrapper">
        <bk-form class="import-template-form">
            <bk-form-item
                :label="$t('template.repository')"
                required
            >
                <bk-select
                    v-model="repository"
                    :placeholder="$t('template.repositoryPlaceholder')"
                >
                    <bk-option
                        v-for="option in list"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name"
                    >
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item
                :label="$t('template.defaultBranch')"
            >
                <div class="">master</div>
            </bk-form-item>
            <bk-form-item
                :label="$t('template.template')"
                required
            >
                <bk-select
                    v-model="templates"
                    searchable
                    multiple
                >
                    <bk-option
                        v-for="option in list"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name"
                    >
                    </bk-option>
                </bk-select>
            </bk-form-item>
        </bk-form>
    </div>
</template>

<script setup name='Repository'>
    import { ref } from 'vue'
    // import UseInstance from '@/hook/useInstance'
    // const { proxy } = UseInstance()
    // const projectId = computed(() => proxy.$route.params.projectId)
    const repository = ref('')

</script>

<style lang="scss">
    .import-template-form {
        max-width: 920px;
        margin-top: 30px;
        margin-bottom: 20px;
    }
</style>
