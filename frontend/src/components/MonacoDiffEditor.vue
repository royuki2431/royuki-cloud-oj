<template>
  <div class="monaco-diff-editor-wrapper">
    <div ref="diffContainer" class="diff-container"></div>
  </div>
</template>

<script setup lang="ts">
import * as monaco from 'monaco-editor'
import { onMounted, onBeforeUnmount, watch, ref } from 'vue'

interface Props {
  original: string
  modified: string
  language?: string
  theme?: string
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  language: 'java',
  theme: 'vs-dark',
  height: '500px',
})

const diffContainer = ref<HTMLElement>()
let diffEditor: monaco.editor.IStandaloneDiffEditor | null = null

onMounted(() => {
  if (!diffContainer.value) return

  // 创建 diff 编辑器
  diffEditor = monaco.editor.createDiffEditor(diffContainer.value, {
    theme: props.theme,
    readOnly: true,
    renderSideBySide: true,
    automaticLayout: true,
    minimap: { enabled: false },
    fontSize: 14,
    lineNumbers: 'on',
    scrollBeyondLastLine: false,
    wordWrap: 'on',
    // 差异显示选项
    renderIndicators: true,
    ignoreTrimWhitespace: false,
    // 滚动条
    scrollbar: {
      vertical: 'auto',
      horizontal: 'auto',
      verticalScrollbarSize: 10,
      horizontalScrollbarSize: 10,
    },
  })

  // 设置模型
  const originalModel = monaco.editor.createModel(props.original, props.language)
  const modifiedModel = monaco.editor.createModel(props.modified, props.language)

  diffEditor.setModel({
    original: originalModel,
    modified: modifiedModel,
  })
})

// 监听内容变化
watch([() => props.original, () => props.modified], ([newOriginal, newModified]) => {
  if (diffEditor) {
    const originalModel = monaco.editor.createModel(newOriginal, props.language)
    const modifiedModel = monaco.editor.createModel(newModified, props.language)
    diffEditor.setModel({
      original: originalModel,
      modified: modifiedModel,
    })
  }
})

// 监听语言变化
watch(() => props.language, (newLang) => {
  if (diffEditor) {
    const model = diffEditor.getModel()
    if (model) {
      monaco.editor.setModelLanguage(model.original, newLang)
      monaco.editor.setModelLanguage(model.modified, newLang)
    }
  }
})

onBeforeUnmount(() => {
  if (diffEditor) {
    const model = diffEditor.getModel()
    model?.original.dispose()
    model?.modified.dispose()
    diffEditor.dispose()
  }
})
</script>

<style scoped>
.monaco-diff-editor-wrapper {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
}

.diff-container {
  width: 100%;
  height: v-bind(height);
}
</style>
