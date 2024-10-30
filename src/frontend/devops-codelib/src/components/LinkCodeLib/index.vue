<template>
    <bk-dropdown-menu
        class="devops-button-dropdown"
        :disabled="!isListReady"
    >
        <bk-button
            theme="primary"
            slot="dropdown-trigger"
        >
            <i class="devops-icon icon-plus"></i>
            <span>{{ $t('codelib.linkCodelib') }}</span>
        </bk-button>
        <ul
            class="devops-button-dropdown-menu"
            slot="dropdown-content"
        >
            <li
                v-for="item in codelibTypes"
                :key="item.scmType"
                @click="createCodelib(item.scmType)"
                :class="{
                    'disabled-codelib-type': item.status !== 'OK'
                }"
            >
                {{ item.name }}
            </li>
        </ul>
    </bk-dropdown-menu>
</template>

<script>
    import { mapState } from 'vuex'
    export default {
        name: 'link-code-lib',
        props: {
            createCodelib: {
                type: Function,
                required: true
            }
        },
        computed: {
            ...mapState('codelib', [
                'codelibTypes'
            ]),
            isListReady () {
                return this.codelibTypes?.length > 0 && !this.disabled
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
                &.disabled-codelib-type {
                    color: #c4c6cc;
                    cursor: not-allowed;
                }
            }
        }
    }
</style>
