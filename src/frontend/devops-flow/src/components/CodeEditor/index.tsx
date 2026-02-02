import { defineComponent, ref, watch, onMounted, onBeforeUnmount, nextTick, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import MonacoEditor from '@/utils/monacoEditor'
import styles from './CodeEditor.module.css'
import { SvgIcon } from '../SvgIcon'
import { Loading } from 'bkui-vue'
import YAML from 'yaml'

export interface HighlightRange {
  startMark: {
    line: number
    column: number
  }
  endMark: {
    line: number
    column: number
  }
}

export interface EditingElementPos {
  stageIndex: number
  containerIndex: number
  elementIndex: number
}

export interface StepInfo {
  pos: { line: number; col: number }
  range: any
  editingElementPos: EditingElementPos
}

export interface CodeEditorProps {
  modelValue?: string
  readOnly?: boolean
  language?: string
  height?: string
  width?: string
  hasError?: boolean
  fileUri?: string
  highlightRanges?: HighlightRange[]
  codeLensTitle?: string
}

export default defineComponent({
  name: 'CodeEditor',
  props: {
    modelValue: {
      type: String as PropType<CodeEditorProps['modelValue']>,
      default: ''
    },
    readOnly: {
      type: Boolean as PropType<CodeEditorProps['readOnly']>,
      default: false
    },
    language: {
      type: String as PropType<CodeEditorProps['language']>,
      default: 'yaml'
    },
    height: {
      type: String as PropType<CodeEditorProps['height']>,
      default: '100%'
    },
    width: {
      type: String as PropType<CodeEditorProps['width']>,
      default: '100%'
    },
    hasError: {
      type: Boolean as PropType<CodeEditorProps['hasError']>,
      default: false
    },
    fileUri: {
      type: String as PropType<CodeEditorProps['fileUri']>,
      default: 'flow.yml'
    },
    highlightRanges: {
      type: Array as PropType<CodeEditorProps['highlightRanges']>,
      default: () => []
    },
    codeLensTitle: {
      type: String as PropType<CodeEditorProps['codeLensTitle']>,
      default: 'Plugin'
    }
  },
  emits: ['update:modelValue', 'change', 'update:hasError', 'step-click'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const editorRef = ref<HTMLDivElement>()
    const isLoading = ref(false)
    const fullScreen = ref(false)
    const errorList = ref<{ message: string; startLineNumber: number; startColumn: number }[]>([])

    let monaco: typeof import('monaco-editor') | null = null
    let editor: import('monaco-editor').editor.IStandaloneCodeEditor | null = null
    let decorationsCollection: import('monaco-editor').editor.IEditorDecorationsCollection | null = null
    let codeLensDisposable: import('monaco-editor').IDisposable | null = null
    let scheduleUpdateCodeLens: ((e?: any) => void) | null = null

    const style = {
      height: props.height,
      width: props.width
    }

    // Initialize Monaco Editor
    const initEditor = async () => {
      if (!editorRef.value) return

      isLoading.value = true

      try {
        monaco = await MonacoEditor.instance()

        const model = monaco.editor.createModel(
          props.modelValue,
          props.language,
          monaco.Uri.parse(props.fileUri)
        )

        editor = monaco.editor.create(editorRef.value, {
          model,
          automaticLayout: true,
          formatOnPaste: true,
          unicodeHighlight: {
            ambiguousCharacters: false
          },
          minimap: {
            enabled: false
          },
          readOnly: props.readOnly,
          scrollBeyondLastLine: false,
          fontSize: 14,
          lineNumbers: 'on',
          roundedSelection: false,
          scrollbar: {
            verticalScrollbarSize: 10,
            horizontalScrollbarSize: 10
          }
        })

        // Listen to marker changes (errors/warnings)
        monaco.editor.onDidChangeMarkers(() => {
          errorList.value = monaco!.editor.getModelMarkers({
            resource: monaco!.Uri.parse(props.fileUri)
          })
          emit('update:hasError', errorList.value.length > 0)
        })

        // Listen to content changes
        editor.onDidChangeModelContent(() => {
          const value = editor!.getValue()
          if (props.modelValue !== value) {
            emit('update:modelValue', value)
            emit('change', value)
          }

          // Update CodeLens when content changes
          if (monaco && !codeLensDisposable && props.modelValue) {
            registerCodeLensProvider()
          } else {
            nextTick(() => {
              scheduleUpdateCodeLens?.()
            })
          }
        })

        // Initialize CodeLens if needed
        if (!codeLensDisposable && props.modelValue && !props.readOnly) {
          registerCodeLensProvider()
        }

        // Initialize highlight
        if (props.highlightRanges && props.highlightRanges.length > 0) {
          highlightBlocks(props.highlightRanges)
        }
      } catch (error: any) {
        console.error('Failed to initialize Monaco Editor:', error)
      } finally {
        isLoading.value = false
      }
    }

    // Toggle fullscreen mode
    const toggleFullScreen = () => {
      fullScreen.value = !fullScreen.value
    }

    // Highlight specific blocks/lines
    const highlightBlocks = (blocks: HighlightRange[]) => {
      if (monaco && editor && Array.isArray(blocks) && blocks.length > 0) {
        const ranges = blocks.map(({ startMark, endMark }) => ({
          range: new monaco!.Range(
            startMark.line + 1,
            startMark.column,
            endMark.line,
            endMark.column
          ),
          options: {
            isWholeLine: true,
            className: 'code-highlight-block',
            marginClassName: 'code-highlight-block'
          }
        }))

        decorationsCollection?.clear()
        decorationsCollection = editor.createDecorationsCollection(ranges)
        editor.revealRangeInCenterIfOutsideViewport(ranges[0]!.range, monaco.editor.ScrollType.Smooth)
      } else {
        decorationsCollection?.clear()
      }
    }

    // Format document
    const format = () => {
      editor?.getAction('editor.action.formatDocument')?.run()
    }

    // Visit YAML and extract steps information
    const visitYaml = (yaml: string): StepInfo[] => {
      if (!monaco || !yaml) return []

      try {
        const lineCounter = new YAML.LineCounter()
        const doc = YAML.parseDocument(yaml, { lineCounter })

        const steps: StepInfo[] = []
        let stageIndex = -1
        let containerIndex = -1

        YAML.visit(doc, {
          Pair(_: unknown, pair: any) {
            if (pair.key && pair.key.value === 'jobs') {
              stageIndex++
              containerIndex = -1
            }
            if (pair.key && pair.key.value === 'steps') {
              if (stageIndex < 0) {
                stageIndex = 0
              }
              containerIndex++
              if (Array.isArray(pair.value.items)) {
                steps.push(...pair.value.items.map((item: any, index: number) => {
                  const startPos = lineCounter.linePos(item.range[0])
                  const endPos = lineCounter.linePos(item.range[1])
                  return {
                    pos: startPos,
                    range: new monaco!.Range(
                      startPos.line,
                      startPos.col,
                      endPos.line,
                      endPos.col
                    ),
                    editingElementPos: {
                      stageIndex,
                      containerIndex,
                      elementIndex: index
                    }
                  }
                }))
                return YAML.visit.SKIP
              }
            }
          }
        })
        return steps
      } catch (error) {
        console.error('Failed to visit YAML:', error)
        return []
      }
    }

    // Get atom by position
    const getAtomByPos = ({ stageIndex, containerIndex, elementIndex }: EditingElementPos): any => {
      try {
        const doc = YAML.parse(props.modelValue || '')
        const steps = getJobsByPos(doc, { stageIndex, containerIndex })
        return steps?.[elementIndex] || null
      } catch (error) {
        console.error('Failed to get atom by position:', error)
        return null
      }
    }

    // Get jobs by position
    const getJobsByPos = (doc: any, { stageIndex, containerIndex }: Omit<EditingElementPos, 'elementIndex'>): any => {
      try {
        if (doc.stages?.[stageIndex] || doc.jobs) {
          const jobs = doc.stages?.[stageIndex]
            ? Object.values(doc.stages[stageIndex].jobs)
            : Object.values(doc.jobs)
          return (jobs as any)[containerIndex]?.steps ?? (jobs as any)[containerIndex]
        }
        return doc.steps
      } catch (error) {
        console.error('Failed to get jobs by position:', error)
        return null
      }
    }

    // Handle CodeLens click
    const handleCodeLensClick = ({ editingElementPos }: StepInfo) => {
      const atom = getAtomByPos(editingElementPos)
      if (atom) {
        if (atom.run) {
          emit('step-click', editingElementPos, {
            ...atom,
            with: {
              script: atom.run
            }
          })
        } else {
          emit('step-click', editingElementPos, atom)
        }
      }
    }

    // Register CodeLens provider
    const registerCodeLensProvider = () => {
      if (!props.readOnly && monaco && editor) {
        codeLensDisposable = monaco.languages.registerCodeLensProvider(props.language, {
          onDidChange: (cb) => {
            scheduleUpdateCodeLens = cb
            return {
              dispose: () => {}
            }
          },
          provideCodeLenses: () => {
            const steps = visitYaml(props.modelValue || '')
            return {
              lenses: steps.map((item, index) => ({
                range: {
                  startLineNumber: item.pos.line,
                  endLineNumber: item.pos.line,
                  startColumn: item.pos.col,
                  endColumn: item.pos.col
                },
                id: String(index),
                command: {
                  id: editor!.addCommand(0, () => {
                    handleCodeLensClick(item)
                  }, '') || '',
                  title: props.codeLensTitle
                }
              })),
              dispose: () => {}
            }
          }
        })
      } else {
        editor?.updateOptions?.({ codeLens: false })
      }
    }

    // Insert fragment at position
    const insertFragmentAtPos = (text: string, { stageIndex, containerIndex, elementIndex }: EditingElementPos) => {
      try {
        const doc = YAML.parse(props.modelValue || '')
        const jobs = getJobsByPos(doc, { stageIndex, containerIndex })

        if (jobs) {
          jobs[elementIndex] = YAML.parse(text)[0]
          const result = YAML.stringify(doc)
          emit('update:modelValue', result)
          emit('change', result)
        }
      } catch (error) {
        console.error('Failed to insert fragment:', error)
      }
    }

    // Expose methods for parent component
    const exposedMethods = {
      format,
      insertFragmentAtPos,
      getAtomByPos,
      highlightBlocks
    }

    // Watch for value changes from parent
    watch(() => props.modelValue, (newValue) => {
      if (monaco && !codeLensDisposable && !props.readOnly) {
        registerCodeLensProvider()
      }
      if (editor && newValue !== editor.getValue()) {
        editor.setValue(newValue)
      }
    })

    // Watch for readOnly changes
    watch(() => props.readOnly, (val) => {
      editor?.updateOptions({ readOnly: val })
    })

    // Watch for highlightRanges changes
    watch(() => props.highlightRanges, (ranges) => {
      highlightBlocks(ranges || [])
    }, { deep: true })

    // Watch for readOnly changes
    watch(() => !props.readOnly, (show) => {
      if (show && monaco && editor) {
        registerCodeLensProvider()
      } else {
        codeLensDisposable?.dispose()
        codeLensDisposable = null
        editor?.updateOptions?.({ codeLens: false })
      }
    })

    onMounted(() => {
      nextTick(() => {
        initEditor()
      })
    })

    onBeforeUnmount(() => {
      decorationsCollection?.clear()
      codeLensDisposable?.dispose()
      editor?.getModel()?.dispose()
      editor?.dispose()
    })

    return () => (
      <div class={[styles.codeEditor, fullScreen.value && styles.fullScreen]} style={style}>
        {isLoading.value && (
          <Loading class={styles.codeEditorLoading}  />
        )}

        {!props.readOnly && (
          <div class={styles.toolbar}>
            <span onClick={format} title={t('format')}>
              <SvgIcon
                name="format"
                size={26}
                class={styles.toolbarIcon}
              />
            </span>
            <span onClick={toggleFullScreen}>
              <SvgIcon
                name={fullScreen.value ? 'un-full-screen' : 'full-screen'}
                size={26}
                class={styles.toolbarIcon}
              />
            </span>
          </div>
        )}

        <div ref={editorRef} class={styles.editorContainer} style={style}></div>

        {!props.readOnly && errorList.value.length > 0 && (
          <ul class={styles.errorSummary}>
            {errorList.value.map((item, index) => (
              <li key={index}>
                <SvgIcon name="circle-close" />
                <p>
                  <span>{item.message}</span>
                  <span class={styles.errorPosition}>
                    ({item.startLineNumber}, {item.startColumn})
                  </span>
                </p>
              </li>
            ))}
          </ul>
        )}
      </div>
    )
  }
})
