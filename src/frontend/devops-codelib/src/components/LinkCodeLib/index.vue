<template>
    <bk-dropdown-menu :class="{ 'devops-button-dropdown': true, 'disabled': disabled }">
        <bk-button :disabled="disabled" theme="primary" slot="dropdown-trigger">
            <i class="devops-icon icon-plus"></i>
            <span>{{ $t('codelib.linkCodelib') }}</span>
        </bk-button>
        <ul class="devops-button-dropdown-menu" slot="dropdown-content">
            <li v-for="typeLabel in codelibTypes" :key="typeLabel" @click="createCodelib(typeLabel)">
                {{ $t('codelib.typeCodelib', [typeLabel])}}
            </li>
        </ul>
    </bk-dropdown-menu>
</template>

<script>
    import { codelibTypes } from '../../config'
    export default {
        name: 'link-code-lib',
        props: {
            createCodelib: {
                type: Function,
                required: true
            },
            disabled: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            codelibTypes () {
                return codelibTypes
            }
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    .devops-button-dropdown {
        &.disabled {
            pointer-events: none;
        }
        &-menu {
            > li {
                display: block;
                line-height: 41px;
                padding: 0 15px;
                color: $fontColor;
                font-size: 14px;
                text-decoration: none;
                white-space: nowrap;
                cursor: pointer;

                &:hover {
                    background: $bgColor;
                    color: $primaryColor;
                }
            }
        }
    }
</style>
