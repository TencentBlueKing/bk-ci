import { defineComponent, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as monaco from 'monaco-editor'

// 配置 worker
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'

self.MonacoEnvironment = {
  getWorker(_, label) {
    if (label === 'json') {
      return new jsonWorker()
    }
    if (label === 'css' || label === 'scss' || label === 'less') {
      return new cssWorker()
    }
    if (label === 'html' || label === 'handlebars' || label === 'razor') {
      return new htmlWorker()
    }
    if (label === 'typescript' || label === 'javascript') {
      return new tsWorker()
    }
    return new editorWorker()
  },
}

export default defineComponent({
  name: 'AtomAceEditor',
  props: {
    value: {
      type: String,
      default: '',
    },
    name: {
      type: String,
      required: true,
    },
    lang: {
      type: String,
      default: 'shell',
    },
    readOnly: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    handleChange: {
      type: Function,
      default: () => () => {},
    },
    height: {
      type: [String, Number],
      default: 300,
    },
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const editorContainer = ref<HTMLElement | null>(null)
    let editor: monaco.editor.IStandaloneCodeEditor | null = null

    const initEditor = () => {
      if (!editorContainer.value) return

      // 映射语言名称
      let language = props.lang
      if (language === 'sh' || language === 'bash') {
        language = 'shell'
      }

      editor = monaco.editor.create(editorContainer.value, {
        value: props.value,
        language: language,
        readOnly: props.readOnly || props.disabled,
        theme: 'vs-dark',
        minimap: { enabled: false },
        automaticLayout: true,
        scrollBeyondLastLine: false,
        tabSize: 2,
        fontSize: 12,
        fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      })

      editor.onDidChangeModelContent(() => {
        const val = editor?.getValue() || ''
        if (val !== props.value) {
          emit('update:value', val)
          emit('change', val)
          props.handleChange(props.name, val)
        }
      })
    }

    watch(
      () => props.value,
      (val) => {
        if (editor && val !== editor.getValue()) {
          editor.setValue(val)
        }
      },
    )

    watch(
      () => props.disabled || props.readOnly,
      (val) => {
        if (editor) {
          editor.updateOptions({ readOnly: val })
        }
      },
    )

    onMounted(() => {
      initEditor()
    })

    onBeforeUnmount(() => {
      if (editor) {
        editor.dispose()
      }
    })

    return () => (
      <div
        ref={editorContainer}
        style={{
          height: typeof props.height === 'number' ? `${props.height}px` : props.height,
        }}
      />
    )
  },
})
