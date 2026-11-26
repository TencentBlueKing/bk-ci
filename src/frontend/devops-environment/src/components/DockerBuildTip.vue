<template>
    <div class="docker-build-tip">
        <p class="tip-label">{{ $t('environment.dockerEffectScope') }}</p>
        <ul>
            <li>{{ $t('environment.dockerEffectJob') }}</li>
            <li>{{ $t('environment.dockerEffectDirect') }}</li>
        </ul>
        <template v-if="showValueRule">
            <p class="tip-label">{{ $t('environment.dockerValueTitle') }}</p>
            <ul>
                <li>
                    <span class="tip-key">{{ $t('environment.dockerValueJob') }}</span>
                    {{ $t('environment.dockerValueJobResult') }}
                </li>
                <li>
                    <span class="tip-key">{{ $t('environment.dockerValueEnv') }}</span>
                    {{ $t('environment.dockerValueEnvResult') }}
                </li>
                <li>
                    <span class="tip-key">{{ $t('environment.dockerValueNode') }}</span>
                    {{ useThisNode ? $t('environment.dockerValueThisNodeResult') : $t('environment.dockerValueNodeResult') }}
                </li>
            </ul>
        </template>
    </div>
</template>

<script>
    export default {
        name: 'DockerBuildTip',
        props: {
            showValueRule: {
                type: Boolean,
                default: true
            },
            useThisNode: {
                type: Boolean,
                default: false
            }
        }
    }
</script>

<style lang="scss" scoped>
.docker-build-tip {
    font-size: 12px;
    line-height: 20px;
    color: #63656e;

    .tip-label {
        margin: 8px 0 4px;
        color: #313238;
        font-weight: 700;

        &:first-child {
            margin-top: 0;
        }
    }

    ul {
        margin: 0;
        padding-left: 16px;
    }

    .tip-key {
        margin-right: 8px;
        color: #313238;
    }
}
</style>

<style lang="scss">
.docker-build-tip-popover .tippy-tooltip,
.tippy-tooltip.docker-build-tip-popover {
    background-color: #fff !important;
    color: #63656e !important;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
    border: 1px solid #dcdee5;
    max-width: 420px;
}

.docker-build-tip-popover .tippy-content,
.tippy-tooltip.docker-build-tip-popover .tippy-content {
    white-space: normal;
    text-align: left;
    padding: 12px 16px;
}

.tippy-popper[x-placement^='top'] .tippy-tooltip.docker-build-tip-popover .tippy-arrow,
.tippy-popper.docker-build-tip-popover[x-placement^='top'] .tippy-arrow {
    border-top-color: #fff !important;
}

.tippy-popper[x-placement^='bottom'] .tippy-tooltip.docker-build-tip-popover .tippy-arrow,
.tippy-popper.docker-build-tip-popover[x-placement^='bottom'] .tippy-arrow {
    border-bottom-color: #fff !important;
}

.docker-build-tip-popover .docker-build-tip,
.tippy-tooltip.docker-build-tip-popover .docker-build-tip {
    font-size: 12px;
    line-height: 20px;
    color: #63656e;
    white-space: normal;
    text-align: left;
}

.docker-build-tip-popover .docker-build-tip .tip-label,
.tippy-tooltip.docker-build-tip-popover .docker-build-tip .tip-label {
    margin: 8px 0 4px;
    color: #313238;
    font-weight: 700;
}

.docker-build-tip-popover .docker-build-tip .tip-label:first-child,
.tippy-tooltip.docker-build-tip-popover .docker-build-tip .tip-label:first-child {
    margin-top: 0;
}

.docker-build-tip-popover .docker-build-tip ul,
.tippy-tooltip.docker-build-tip-popover .docker-build-tip ul {
    margin: 0;
    padding-left: 16px;
}

.docker-build-tip-popover .docker-build-tip li + li,
.tippy-tooltip.docker-build-tip-popover .docker-build-tip li + li {
    margin-top: 4px;
}

.docker-build-tip-popover .docker-build-tip .tip-key,
.tippy-tooltip.docker-build-tip-popover .docker-build-tip .tip-key {
    margin-right: 4px;
    color: #313238;
}
</style>
