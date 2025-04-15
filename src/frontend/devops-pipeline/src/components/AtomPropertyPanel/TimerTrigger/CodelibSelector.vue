<template>
    <div class="conditional-input-selector">
        <bk-select
            v-model="repositoryType"
            ext-cls="group-box"
            :clearable="false"
            :disabled="disabled"
            @change="(val) => handleChangeRepositoryType(val)"
        >
            <bk-option
                v-for="item in codelibConfigList"
                :key="item.value"
                :id="item.value"
                :name="item.label"
            >
                <slot
                    name="option-item"
                    v-bind="item"
                ></slot>
            </bk-option>
        </bk-select>
        <request-selector
            v-if="repositoryType === 'ID'"
            class="input-selector"
            v-bind="codelibOption"
            :popover-min-width="250"
            :disabled="disabled"
            :url="getCodeRepoUrl"
            name="repoHashId"
            :value="element['repoHashId']"
            :handle-change="(name, val) => handleChangeRepoHashId(name, val)"
        >
        </request-selector>
        <vuex-input
            v-else
            :value="element['repoName']"
            :disabled="repositoryType === 'SELF'"
            :key="repositoryType"
            :placeholder="repositoryType === 'SELF' ? $t('pacRepoNotSet') : $t('enterRepoAlias')"
            class="input-selector"
            name="repoName"
            :handle-change="handleChange"
        >
        </vuex-input>
    </div>
</template>

<script>
    import VuexInput from '@/components/atomFormField/VuexInput'
    import RequestSelector from '@/components/atomFormField/RequestSelector'
    import { REPOSITORY_API_URL_PREFIX } from '@/store/constants'
    export default {
        name: 'codelib-selector',
        components: {
            VuexInput,
            RequestSelector
        },
        props: {
            disabled: Boolean,
            element: Object,
            handleChange: Function
        },
        computed: {
            repositoryType () {
                return this.element.repositoryType || 'ID'
            },
            getCodeRepoUrl () {
                return `/${REPOSITORY_API_URL_PREFIX}/user/repositories/{projectId}/hasPermissionList?permission=USE&page=1&pageSize=1000`
            },
            codelibOption () {
                return {
                    paramId: 'repositoryHashId',
                    paramName: 'aliasName',
                    searchable: true
                }
            },
            codelibConfigList () {
                return [
                    {
                        value: 'ID',
                        label: this.$t('selectRepo')
                    },
                    {
                        value: 'NAME',
                        label: this.$t('enterAlias')
                    },
                    {
                        value: 'SELF',
                        label: this.$t('monitorPac')
                    }
                ]
            }
        },
        methods: {
            handleChangeRepositoryType (val) {
                this.handleChange('branches', [])
                this.handleChange('repoHashId', '')
                this.handleChange('repoName', '')
                this.handleChange('repositoryType', val)
            },
            handleChangeRepoHashId (name, val) {
                this.handleChange(name, val)
                this.handleChange('branches', [])
            }
        }
    }
</script>
