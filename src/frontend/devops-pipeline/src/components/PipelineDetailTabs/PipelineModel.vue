<template>
    <div class="pipeline-model-arrangement">
        <pipeline
            :editable="false"
            :show-header="false"
            :pipeline="pipelineWithoutTrigger"
            is-preview
            v-on="$listeners"
        >
        </pipeline>
    </div>
</template>

<script>
    import Pipeline from '@/components/Pipeline'
    import { mapState } from 'vuex'

    export default {
        components: {
            Pipeline
        },
        computed: {
            ...mapState('atom', ['pipelineWithoutTrigger'])
        }
    }
</script>

<style lang="scss">
@import "@/scss/mixins/ellipsis";
.pipeline-model-arrangement {
  height: 100%;
  overflow: hidden;
}
.pipeline-params-aside {
  position: absolute;
  top: 0;
  right: 0;
  width: 480px;
  height: 100%;
  font-size: 12px;
  transform: translateX(100%);
  z-index: 999;
  border-left: 1px solid #dcdee5;
  transition: all 0.3s ease;
  &.params-aside-visible {
    transform: translateX(0);
    .pipeline-params-entry > i {
      transform: rotate(0);
    }
  }
  .pipeline-params-entry {
    width: 24px;
    position: absolute;
    background: #c4c6cc;
    border-radius: 4px 0 0 4px;
    color: white;
    padding: 4px;
    font-size: 12px;
    text-align: center;
    left: -24px;
    top: 24px;
    cursor: pointer;
    > i {
      transition: all 0.3s ease;
      display: block;
      transform: rotate(180deg);
    }
  }
  .pipeline-params-aside-content {
    height: 100%;
    background: white;
    .param-type-desc {
      display: flex;
      grid-gap: 12px;
      justify-content: flex-end;
      margin: 24px 0;
    }
    .param-indicator {
      display: flex;
      align-items: center;
      grid-gap: 8px;
    }
    .visible-param-dot,
    .required-param-dot,
    .readonly-param-dot {
      display: inline-block;
      width: 10px;
      height: 10px;
      border-radius: 50%;
      &.visible-param-dot {
        background: #2dcb9d;
      }
      &.required-param-dot {
        background: #ff5656;
      }
      &.readonly-param-dot {
        background: #c4c6cc;
      }
    }
    .pipeline-params-list {
      border: 1px solid #dcdee5;
      > li {
        border-bottom: 1px solid #dcdee5;
        padding: 10px 16px;
        display: grid;
        grid-gap: 24px;
        grid-auto-flow: column;
        grid-template-columns: 1fr auto;
        align-items: center;
        &:last-child {
          border-bottom: none;
        }
        > p {
          display: flex;
          flex-direction: column;
          grid-gap: 6px;
          overflow: hidden;
          > label {
            margin-right: auto;
            @include ellipsis();
            max-width: 100%;
            &.has-param-desc {
              border-bottom: 1px dashed #979ba5;
            }
          }
          > span {
            color: #979ba5;
            @include ellipsis();
          }
        }
        > span {
          display: flex;
          grid-gap: 8px;
        }
      }
    }
    .pipeline-reccomend-version-conf {
      display: flex;
      flex-direction: column;
      grid-gap: 16px;
      margin-top: 24px;
      > li {
        display: flex;
        flex-direction: column;
        grid-gap: 6px;
        font-size: 12px;
        > label {
          line-height: 20px;
          > span {
            color: #979ba5;
          }
        }
      }
    }
  }
}
</style>
