<template>
    <div class="container-app-selector">
        <bk-select searchable
            :clearable="false"
            :disabled="disabled"
            @selected="handleAppSelector"
            :loading="isLoading"
            :value="app"
        >
            <bk-option v-for="item in appList" :key="item.id" :id="item.id" :name="item.name" :disabled="item.disalbed">
            </bk-option>
        </bk-select>
        <bk-select searchable
            :disabled="disabled"
            :value="version"
            @selected="handleVersionSelector"
        >
            <bk-option v-for="item in versionList" :key="item.id" :id="item.id" :name="item.name">
            </bk-option>
        </bk-select>
        <i v-if="addContainerApp && !disabled" @click="addContainerApp" class="bk-icon icon-plus"></i>
        <i v-if="removeContainerApp && !disabled" @click="removeContainerApp(app)" class="bk-icon icon-minus"></i>
    </div>
</template>

<script>
    export default {
        name: 'container-app-selector',
        props: {
            apps: {
                type: Object,
                default: () => ({}),
                required: true
            },
            disabled: {
                type: Boolean,
                default: false
            },
            handleChange: {
                type: Function,
                required: false
            },
            envs: {
                type: Object
            },
            app: {
                type: String
            },
            version: {
                type: String
            },
            removeContainerApp: {
                type: Function
            },
            addContainerApp: {
                type: Function
            }
        },
        computed: {
            isLoading () {
                return !this.apps
            },
            appList () {
                const selectedList = this.envs ? Object.keys(this.envs) : []
                return this.isLoading ? [] : Object.keys(this.apps).map(app => ({
                    id: app,
                    name: app,
                    disalbed: selectedList.indexOf(app) > -1
                }))
            },
            
            versionList () {
                const { app, apps } = this
                return apps[app] ? apps[app].versions.map(version => ({
                    id: version,
                    name: version
                })) : []
            }
        },
        methods: {
            handleAppSelector (app) {
                this.handleChange(this.app, app, '')
            },
            handleVersionSelector (version) {
                this.handleChange(this.app, this.app, version)
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    .container-app-selector {
        display: flex;
        align-items: center;
        .bk-select {
            width: 44%;
            margin: 0 10px 0 0;
        }
        .bk-icon {
            cursor: pointer;
            margin: 0 6px;
            &.icon-plus {
                display: none;
            }
            &:hover {
                color: $primaryColor;
            }
        }
    }
</style>
