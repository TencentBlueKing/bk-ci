<template>
    <div class="exec-material-row">
        <span v-for="(field, index) in materialInfos" :key="field">
            <logo :name="iconArray[index]" size="14" />
            <a
                v-if="field === 'newCommitId'"
                class="material-link"
                theme="primary"
                target="_blank"
                :href="material.url"
            >
                {{ material[field].substring(0, 8) }}
            </a>
            <span class="material-span" v-else>{{ material[field] }}</span>
        </span>
        <span v-if="showMore" @mouseenter="emitMouseEnter" class="exec-more-material">
            <i class="devops-icon icon-ellipsis" />
        </span>
    </div>
</template>
<script>
    import Logo from '@/components/Logo'
    import { getMaterialIconByType } from '@/utils/util'
    export default {
        emits: ['mouseEnter'],
        components: {
            Logo
        },
        props: {
            showMore: {
                type: Boolean,
                default: true
            },
            material: {
                type: Object,
                required: true
            }

        },
        computed: {
            materialInfos () {
                return [
                    'aliasName',
                    'branchName',
                    'newCommitId'
                ]
            },
            iconArray () {
                return [
                    getMaterialIconByType(this.material?.scmType),
                    'branch',
                    'commit'
                ]
            }
        },
        methods: {
            emitMouseEnter () {
                this.$emit('mouseenter')
            }
        }
    }
</script>
