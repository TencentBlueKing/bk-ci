<template>
    <bk-dropdown-menu :class="{ &quot;devops-button-dropdown&quot;: true, &quot;disabled&quot;: disabled }">
        <bk-button :disabled="disabled" theme="primary" slot="dropdown-trigger">
            <i class="bk-icon icon-plus"></i>
            <span>关联代码库</span>
        </bk-button>
        <ul class="devops-button-dropdown-menu" slot="dropdown-content">
            <li v-for="typeLabel in codelibTypes" :key="typeLabel" @click="createCodelib(typeLabel)">
                {{typeLabel}}代码库
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
