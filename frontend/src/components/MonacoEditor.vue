<template>
  <div class="monaco-editor-wrapper">
    <!-- 工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-tooltip content="格式化代码 (Shift+Alt+F)" placement="bottom">
          <el-button size="small" @click="formatCode" :icon="MagicStick">格式化</el-button>
        </el-tooltip>
        <el-tooltip content="撤销 (Ctrl+Z)" placement="bottom">
          <el-button size="small" @click="undo" :icon="RefreshLeft" />
        </el-tooltip>
        <el-tooltip content="重做 (Ctrl+Y)" placement="bottom">
          <el-button size="small" @click="redo" :icon="RefreshRight" />
        </el-tooltip>
        <el-divider direction="vertical" />
        <el-tooltip content="查找替换 (Ctrl+H)" placement="bottom">
          <el-button size="small" @click="openFindReplace" :icon="Search">查找</el-button>
        </el-tooltip>
      </div>
      <div class="toolbar-right">
        <el-tag size="small" :type="errors.length > 0 ? 'danger' : 'success'">
          {{ errors.length > 0 ? `${errors.length} 个问题` : '无错误' }}
        </el-tag>
        <el-select v-model="currentFontSize" size="small" style="width: 80px" @change="changeFontSize">
          <el-option :value="12" label="12px" />
          <el-option :value="14" label="14px" />
          <el-option :value="16" label="16px" />
          <el-option :value="18" label="18px" />
        </el-select>
      </div>
    </div>
    
    <div ref="editorContainer" class="monaco-editor-container"></div>
    
    <!-- 错误提示面板 -->
    <div v-if="showErrors && errors.length > 0" class="error-panel">
      <div class="error-header">
        <span class="error-icon">⚠️</span>
        <span>发现 {{ errors.length }} 个问题</span>
        <el-button type="text" size="small" @click="showErrors = false">收起</el-button>
      </div>
      <div class="error-list">
        <div 
          v-for="(error, index) in errors" 
          :key="index" 
          class="error-item"
          :class="error.severity === 8 ? 'error' : 'warning'"
          @click="goToError(error)"
        >
          <span class="error-line">第 {{ error.startLineNumber }} 行</span>
          <span class="error-message">{{ error.message }}</span>
        </div>
      </div>
    </div>
    <!-- 错误数量提示 -->
    <div 
      v-if="!showErrors && errors.length > 0" 
      class="error-badge"
      @click="showErrors = true"
    >
      <span class="badge-icon">⚠️</span>
      <span>{{ errors.length }} 个问题</span>
    </div>
    
    <!-- 状态栏 -->
    <div class="editor-statusbar">
      <span>行 {{ cursorPosition.line }}, 列 {{ cursorPosition.column }}</span>
      <span>{{ props.language.toUpperCase() }}</span>
      <span>{{ charCount }} 字符</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import * as monaco from 'monaco-editor'
import { onMounted, onBeforeUnmount, watch, ref, reactive } from 'vue'
import { MagicStick, RefreshLeft, RefreshRight, Search } from '@element-plus/icons-vue'

interface Props {
  modelValue: string
  language?: string
  theme?: string
  readonly?: boolean
  height?: string
  enableLinting?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  language: 'java',
  theme: 'vs-dark',
  readonly: false,
  height: '500px',
  enableLinting: true,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'errors', errors: monaco.editor.IMarkerData[]): void
}>()

const editorContainer = ref<HTMLElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null
const errors = ref<monaco.editor.IMarkerData[]>([])
const showErrors = ref(false)
const currentFontSize = ref(14)
const charCount = ref(0)
const cursorPosition = reactive({ line: 1, column: 1 })

// 代码补全提供器存储
let completionProviders: monaco.IDisposable[] = []

// 语言特定的语法检查规则
const languagePatterns: Record<string, Array<{ pattern: RegExp; message: string; severity: number }>> = {
  java: [
    { pattern: /class\s+\w+\s*{[^}]*public\s+static\s+void\s+main\s*\(\s*String\s*\[\s*\]\s*\w+\s*\)/s, message: '', severity: 0 }, // 正确的main方法
    { pattern: /\bSystem\.out\.println\s*\([^)]*$/, message: '缺少右括号', severity: 8 },
    { pattern: /\bif\s*\([^)]*$/, message: 'if 语句缺少右括号', severity: 8 },
    { pattern: /\bfor\s*\([^)]*$/, message: 'for 语句缺少右括号', severity: 8 },
    { pattern: /\bwhile\s*\([^)]*$/, message: 'while 语句缺少右括号', severity: 8 },
    { pattern: /[^;{}]\s*\n\s*(public|private|protected|class|if|for|while|return)\b/, message: '可能缺少分号', severity: 4 },
  ],
  cpp: [
    { pattern: /#include\s*<[^>]*$/, message: '缺少右尖括号 >', severity: 8 },
    { pattern: /\bcout\s*<<[^;]*$/, message: '可能缺少分号', severity: 4 },
    { pattern: /\bcin\s*>>[^;]*$/, message: '可能缺少分号', severity: 4 },
    { pattern: /int\s+main\s*\(\s*\)\s*{/, message: '', severity: 0 }, // 正确的main函数
  ],
  c: [
    { pattern: /#include\s*<[^>]*$/, message: '缺少右尖括号 >', severity: 8 },
    { pattern: /\bprintf\s*\([^)]*$/, message: 'printf 缺少右括号', severity: 8 },
    { pattern: /\bscanf\s*\([^)]*$/, message: 'scanf 缺少右括号', severity: 8 },
  ],
  python: [
    { pattern: /\bdef\s+\w+\s*\([^)]*$/, message: '函数定义缺少右括号', severity: 8 },
    { pattern: /\bif\s+[^:]*$/, message: 'if 语句缺少冒号', severity: 4 },
    { pattern: /\bfor\s+[^:]*$/, message: 'for 语句缺少冒号', severity: 4 },
    { pattern: /\bwhile\s+[^:]*$/, message: 'while 语句缺少冒号', severity: 4 },
    { pattern: /\bdef\s+\w+\s*\([^)]*\)\s*[^:]/, message: '函数定义缺少冒号', severity: 4 },
  ],
}

// 通用括号匹配检查
const checkBrackets = (code: string): monaco.editor.IMarkerData[] => {
  const markers: monaco.editor.IMarkerData[] = []
  const stack: Array<{ char: string; line: number; col: number }> = []
  const pairs: Record<string, string> = { '(': ')', '[': ']', '{': '}' }
  const closers: Record<string, string> = { ')': '(', ']': '[', '}': '{' }
  
  const lines = code.split('\n')
  
  for (let lineNum = 0; lineNum < lines.length; lineNum++) {
    const line = lines[lineNum]
    let inString = false
    let stringChar = ''
    
    for (let col = 0; col < line.length; col++) {
      const char = line[col]
      const prevChar = col > 0 ? line[col - 1] : ''
      
      // 处理字符串
      if ((char === '"' || char === "'") && prevChar !== '\\') {
        if (!inString) {
          inString = true
          stringChar = char
        } else if (char === stringChar) {
          inString = false
        }
        continue
      }
      
      if (inString) continue
      
      // 处理注释
      if (char === '/' && line[col + 1] === '/') break
      
      if (pairs[char]) {
        stack.push({ char, line: lineNum + 1, col: col + 1 })
      } else if (closers[char]) {
        if (stack.length === 0 || stack[stack.length - 1].char !== closers[char]) {
          markers.push({
            severity: monaco.MarkerSeverity.Error,
            message: `多余的 '${char}'`,
            startLineNumber: lineNum + 1,
            startColumn: col + 1,
            endLineNumber: lineNum + 1,
            endColumn: col + 2,
          })
        } else {
          stack.pop()
        }
      }
    }
  }
  
  // 检查未闭合的括号
  for (const item of stack) {
    markers.push({
      severity: monaco.MarkerSeverity.Error,
      message: `'${item.char}' 未闭合`,
      startLineNumber: item.line,
      startColumn: item.col,
      endLineNumber: item.line,
      endColumn: item.col + 1,
    })
  }
  
  return markers
}

// 执行语法检查
const performLinting = () => {
  if (!editor || !props.enableLinting || props.readonly) return
  
  const code = editor.getValue()
  const model = editor.getModel()
  if (!model) return
  
  const markers: monaco.editor.IMarkerData[] = []
  
  // 括号匹配检查
  markers.push(...checkBrackets(code))
  
  // 语言特定检查
  const patterns = languagePatterns[props.language] || []
  const lines = code.split('\n')
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    
    for (const rule of patterns) {
      if (rule.severity === 0) continue // 跳过正确模式
      if (rule.pattern.test(line)) {
        markers.push({
          severity: rule.severity as monaco.MarkerSeverity,
          message: rule.message,
          startLineNumber: i + 1,
          startColumn: 1,
          endLineNumber: i + 1,
          endColumn: line.length + 1,
        })
      }
    }
  }
  
  // 设置标记
  monaco.editor.setModelMarkers(model, 'linter', markers)
  errors.value = markers
  emit('errors', markers)
}

// 跳转到错误位置
const goToError = (error: monaco.editor.IMarkerData) => {
  if (editor) {
    editor.setPosition({ lineNumber: error.startLineNumber, column: error.startColumn })
    editor.focus()
    editor.revealLineInCenter(error.startLineNumber)
  }
}

// 防抖处理
let lintTimeout: ReturnType<typeof setTimeout> | null = null
const debouncedLint = () => {
  if (lintTimeout) clearTimeout(lintTimeout)
  lintTimeout = setTimeout(performLinting, 500)
}

// 注册代码补全提供器
const registerCompletionProviders = () => {
  // 清除旧的提供器
  completionProviders.forEach(p => p.dispose())
  completionProviders = []

  // Java 代码补全
  const javaProvider = monaco.languages.registerCompletionItemProvider('java', {
    provideCompletionItems: (model, position) => {
      const word = model.getWordUntilPosition(position)
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn,
      }

      const suggestions: monaco.languages.CompletionItem[] = [
        // 常用代码片段
        { label: 'sout', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'System.out.println(${1});', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '打印输出', range },
        { label: 'main', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'public static void main(String[] args) {\n\t${1}\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'main 方法', range },
        { label: 'for', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n\t${3}\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'for 循环', range },
        { label: 'fori', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'for (int ${1:i} = 0; ${1:i} < ${2:array}.length; ${1:i}++) {\n\t${3}\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '数组遍历', range },
        { label: 'foreach', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'for (${1:Type} ${2:item} : ${3:collection}) {\n\t${4}\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'foreach 循环', range },
        { label: 'while', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'while (${1:condition}) {\n\t${2}\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'while 循环', range },
        { label: 'if', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'if (${1:condition}) {\n\t${2}\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'if 语句', range },
        { label: 'ifelse', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'if (${1:condition}) {\n\t${2}\n} else {\n\t${3}\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'if-else 语句', range },
        { label: 'try', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'try {\n\t${1}\n} catch (${2:Exception} e) {\n\t${3:e.printStackTrace();}\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'try-catch 语句', range },
        // 常用类
        { label: 'Scanner', kind: monaco.languages.CompletionItemKind.Class, insertText: 'Scanner', documentation: 'java.util.Scanner - 输入扫描器', range },
        { label: 'ArrayList', kind: monaco.languages.CompletionItemKind.Class, insertText: 'ArrayList<${1:Type}>', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'java.util.ArrayList', range },
        { label: 'HashMap', kind: monaco.languages.CompletionItemKind.Class, insertText: 'HashMap<${1:Key}, ${2:Value}>', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'java.util.HashMap', range },
        { label: 'Arrays', kind: monaco.languages.CompletionItemKind.Class, insertText: 'Arrays', documentation: 'java.util.Arrays - 数组工具类', range },
        { label: 'Math', kind: monaco.languages.CompletionItemKind.Class, insertText: 'Math', documentation: 'java.lang.Math - 数学工具类', range },
        // 常用方法
        { label: 'System.out.println', kind: monaco.languages.CompletionItemKind.Method, insertText: 'System.out.println(${1});', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '打印并换行', range },
        { label: 'System.out.print', kind: monaco.languages.CompletionItemKind.Method, insertText: 'System.out.print(${1});', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '打印不换行', range },
        { label: 'Integer.parseInt', kind: monaco.languages.CompletionItemKind.Method, insertText: 'Integer.parseInt(${1:str})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '字符串转整数', range },
        { label: 'String.valueOf', kind: monaco.languages.CompletionItemKind.Method, insertText: 'String.valueOf(${1:value})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '转换为字符串', range },
        // 导入语句
        { label: 'import Scanner', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'import java.util.Scanner;', documentation: '导入 Scanner', range },
        { label: 'import ArrayList', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'import java.util.ArrayList;', documentation: '导入 ArrayList', range },
        { label: 'import Arrays', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'import java.util.Arrays;', documentation: '导入 Arrays', range },
      ]
      return { suggestions }
    }
  })
  completionProviders.push(javaProvider)

  // Python 代码补全
  const pythonProvider = monaco.languages.registerCompletionItemProvider('python', {
    provideCompletionItems: (model, position) => {
      const word = model.getWordUntilPosition(position)
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn,
      }

      const suggestions: monaco.languages.CompletionItem[] = [
        { label: 'def', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'def ${1:function_name}(${2:params}):\n\t${3:pass}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '定义函数', range },
        { label: 'class', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'class ${1:ClassName}:\n\tdef __init__(self${2:, params}):\n\t\t${3:pass}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '定义类', range },
        { label: 'for', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'for ${1:i} in ${2:range(n)}:\n\t${3:pass}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'for 循环', range },
        { label: 'while', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'while ${1:condition}:\n\t${2:pass}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'while 循环', range },
        { label: 'if', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'if ${1:condition}:\n\t${2:pass}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'if 语句', range },
        { label: 'ifelse', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'if ${1:condition}:\n\t${2:pass}\nelse:\n\t${3:pass}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'if-else 语句', range },
        { label: 'try', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'try:\n\t${1:pass}\nexcept ${2:Exception} as e:\n\t${3:print(e)}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'try-except 语句', range },
        { label: 'print', kind: monaco.languages.CompletionItemKind.Function, insertText: 'print(${1})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '打印输出', range },
        { label: 'input', kind: monaco.languages.CompletionItemKind.Function, insertText: 'input(${1})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '读取输入', range },
        { label: 'range', kind: monaco.languages.CompletionItemKind.Function, insertText: 'range(${1:n})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '生成范围', range },
        { label: 'len', kind: monaco.languages.CompletionItemKind.Function, insertText: 'len(${1})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '获取长度', range },
        { label: 'list', kind: monaco.languages.CompletionItemKind.Function, insertText: 'list(${1})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '转换为列表', range },
        { label: 'map', kind: monaco.languages.CompletionItemKind.Function, insertText: 'map(${1:int}, ${2:input().split()})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '映射函数', range },
        { label: 'split', kind: monaco.languages.CompletionItemKind.Method, insertText: 'split(${1})', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '分割字符串', range },
      ]
      return { suggestions }
    }
  })
  completionProviders.push(pythonProvider)

  // C/C++ 代码补全
  const cppProvider = monaco.languages.registerCompletionItemProvider('cpp', {
    provideCompletionItems: (model, position) => {
      const word = model.getWordUntilPosition(position)
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn,
      }

      const suggestions: monaco.languages.CompletionItem[] = [
        { label: 'include', kind: monaco.languages.CompletionItemKind.Snippet, insertText: '#include <${1:iostream}>', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '包含头文件', range },
        { label: 'main', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'int main() {\n\t${1}\n\treturn 0;\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'main 函数', range },
        { label: 'for', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n\t${3}\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'for 循环', range },
        { label: 'while', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'while (${1:condition}) {\n\t${2}\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'while 循环', range },
        { label: 'if', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'if (${1:condition}) {\n\t${2}\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'if 语句', range },
        { label: 'cout', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'cout << ${1} << endl;', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '输出', range },
        { label: 'cin', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'cin >> ${1};', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '输入', range },
        { label: 'vector', kind: monaco.languages.CompletionItemKind.Class, insertText: 'vector<${1:int}>', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'STL vector', range },
        { label: 'string', kind: monaco.languages.CompletionItemKind.Class, insertText: 'string', documentation: 'std::string', range },
        { label: 'sort', kind: monaco.languages.CompletionItemKind.Function, insertText: 'sort(${1:arr}.begin(), ${1:arr}.end());', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '排序', range },
      ]
      return { suggestions }
    }
  })
  completionProviders.push(cppProvider)

  // C 语言也使用 C++ 的补全
  const cProvider = monaco.languages.registerCompletionItemProvider('c', {
    provideCompletionItems: (model, position) => {
      const word = model.getWordUntilPosition(position)
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn,
      }

      const suggestions: monaco.languages.CompletionItem[] = [
        { label: 'include', kind: monaco.languages.CompletionItemKind.Snippet, insertText: '#include <${1:stdio.h}>', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '包含头文件', range },
        { label: 'main', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'int main() {\n\t${1}\n\treturn 0;\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'main 函数', range },
        { label: 'for', kind: monaco.languages.CompletionItemKind.Snippet, insertText: 'for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n\t${3}\n}', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: 'for 循环', range },
        { label: 'printf', kind: monaco.languages.CompletionItemKind.Function, insertText: 'printf("${1:%d}\\n", ${2:var});', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '格式化输出', range },
        { label: 'scanf', kind: monaco.languages.CompletionItemKind.Function, insertText: 'scanf("${1:%d}", &${2:var});', insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet, documentation: '格式化输入', range },
      ]
      return { suggestions }
    }
  })
  completionProviders.push(cProvider)
}

// 注册格式化提供器
const registerFormatProviders = () => {
  // Java 格式化
  const javaFormatter = monaco.languages.registerDocumentFormattingEditProvider('java', {
    provideDocumentFormattingEdits: (model) => {
      const text = model.getValue()
      const formatted = formatJavaCode(text)
      return [{
        range: model.getFullModelRange(),
        text: formatted
      }]
    }
  })
  completionProviders.push(javaFormatter)

  // Python 格式化
  const pythonFormatter = monaco.languages.registerDocumentFormattingEditProvider('python', {
    provideDocumentFormattingEdits: (model) => {
      const text = model.getValue()
      const formatted = formatPythonCode(text)
      return [{
        range: model.getFullModelRange(),
        text: formatted
      }]
    }
  })
  completionProviders.push(pythonFormatter)

  // C/C++ 格式化
  const cppFormatter = monaco.languages.registerDocumentFormattingEditProvider('cpp', {
    provideDocumentFormattingEdits: (model) => {
      const text = model.getValue()
      const formatted = formatCppCode(text)
      return [{
        range: model.getFullModelRange(),
        text: formatted
      }]
    }
  })
  completionProviders.push(cppFormatter)

  const cFormatter = monaco.languages.registerDocumentFormattingEditProvider('c', {
    provideDocumentFormattingEdits: (model) => {
      const text = model.getValue()
      const formatted = formatCppCode(text)
      return [{
        range: model.getFullModelRange(),
        text: formatted
      }]
    }
  })
  completionProviders.push(cFormatter)
}

// Java 代码格式化
const formatJavaCode = (code: string): string => {
  let result = ''
  let indentLevel = 0
  const indentStr = '    '
  const lines = code.split('\n')

  for (let line of lines) {
    line = line.trim()
    if (!line) {
      result += '\n'
      continue
    }

    // 减少缩进的情况
    if (line.startsWith('}') || line.startsWith(')')) {
      indentLevel = Math.max(0, indentLevel - 1)
    }

    result += indentStr.repeat(indentLevel) + line + '\n'

    // 增加缩进的情况
    if (line.endsWith('{') || line.endsWith('(') && !line.includes(')')) {
      indentLevel++
    }
    // 处理 } else { 的情况
    if (line.includes('}') && line.includes('{')) {
      // 保持当前缩进
    }
  }

  return result.trimEnd()
}

// Python 代码格式化
const formatPythonCode = (code: string): string => {
  let result = ''
  let indentLevel = 0
  const indentStr = '    '
  const lines = code.split('\n')

  for (let i = 0; i < lines.length; i++) {
    let line = lines[i]
    const trimmedLine = line.trim()
    
    if (!trimmedLine) {
      result += '\n'
      continue
    }

    // 减少缩进：以 return, break, continue, pass 等结尾且下一行不是缩进内容
    // 或者当前行缩进比预期少
    const currentIndent = line.length - line.trimStart().length
    const expectedIndent = indentLevel * 4

    // 使用原始缩进或计算的缩进
    if (currentIndent < expectedIndent && indentLevel > 0) {
      indentLevel = Math.floor(currentIndent / 4)
    }

    result += indentStr.repeat(indentLevel) + trimmedLine + '\n'

    // 增加缩进的情况
    if (trimmedLine.endsWith(':')) {
      indentLevel++
    }
    // 减少缩进
    if (trimmedLine === 'return' || trimmedLine.startsWith('return ') ||
        trimmedLine === 'break' || trimmedLine === 'continue' || trimmedLine === 'pass') {
      // 下一行可能需要减少缩进，但这需要更复杂的分析
    }
  }

  return result.trimEnd()
}

// C/C++ 代码格式化
const formatCppCode = (code: string): string => {
  let result = ''
  let indentLevel = 0
  const indentStr = '    '
  const lines = code.split('\n')

  for (let line of lines) {
    line = line.trim()
    if (!line) {
      result += '\n'
      continue
    }

    // 减少缩进
    if (line.startsWith('}')) {
      indentLevel = Math.max(0, indentLevel - 1)
    }

    // 预处理指令不缩进
    if (line.startsWith('#')) {
      result += line + '\n'
      continue
    }

    result += indentStr.repeat(indentLevel) + line + '\n'

    // 增加缩进
    if (line.endsWith('{')) {
      indentLevel++
    }
  }

  return result.trimEnd()
}

// 工具栏功能
const formatCode = () => {
  if (editor) {
    editor.getAction('editor.action.formatDocument')?.run()
  }
}

const undo = () => {
  if (editor) {
    editor.trigger('keyboard', 'undo', null)
  }
}

const redo = () => {
  if (editor) {
    editor.trigger('keyboard', 'redo', null)
  }
}

const openFindReplace = () => {
  if (editor) {
    editor.getAction('editor.action.startFindReplaceAction')?.run()
  }
}

const changeFontSize = (size: number) => {
  if (editor) {
    editor.updateOptions({ fontSize: size })
  }
}

// 更新光标位置和字符数
const updateEditorInfo = () => {
  if (editor) {
    const position = editor.getPosition()
    if (position) {
      cursorPosition.line = position.lineNumber
      cursorPosition.column = position.column
    }
    charCount.value = editor.getValue().length
  }
}

onMounted(() => {
  if (!editorContainer.value) return

  // 注册代码补全和格式化
  registerCompletionProviders()
  registerFormatProviders()

  // 创建编辑器实例
  editor = monaco.editor.create(editorContainer.value, {
    value: props.modelValue,
    language: props.language,
    theme: props.theme,
    readOnly: props.readonly,
    minimap: { enabled: true, scale: 1 },
    fontSize: currentFontSize.value,
    lineNumbers: 'on',
    scrollBeyondLastLine: false,
    automaticLayout: true,
    tabSize: 4,
    wordWrap: 'on',
    // 代码补全和智能提示
    quickSuggestions: { other: true, comments: false, strings: true },
    suggestOnTriggerCharacters: true,
    acceptSuggestionOnEnter: 'on',
    snippetSuggestions: 'top',
    wordBasedSuggestions: 'currentDocument',
    parameterHints: { enabled: true, cycle: true },
    // 代码折叠
    folding: true,
    foldingHighlight: true,
    foldingStrategy: 'indentation',
    showFoldingControls: 'always',
    // 括号配对
    bracketPairColorization: { enabled: true, independentColorPoolPerBracketType: true },
    matchBrackets: 'always',
    guides: { bracketPairs: true, bracketPairsHorizontal: true, indentation: true, highlightActiveIndentation: true },
    // 光标和选择
    cursorBlinking: 'smooth',
    cursorSmoothCaretAnimation: 'on',
    smoothScrolling: true,
    mouseWheelZoom: true,
    // 渲染优化
    renderWhitespace: 'selection',
    renderLineHighlight: 'all',
    renderIndentGuides: true,
    // 其他功能
    autoClosingBrackets: 'always',
    autoClosingQuotes: 'always',
    autoSurround: 'languageDefined',
    formatOnPaste: true,
    formatOnType: true,
    linkedEditing: true,
    colorDecorators: true,
    // 滚动条
    scrollbar: { vertical: 'auto', horizontal: 'auto', verticalScrollbarSize: 10, horizontalScrollbarSize: 10 },
  })

  // 监听内容变化
  editor.onDidChangeModelContent(() => {
    if (editor) {
      emit('update:modelValue', editor.getValue())
      debouncedLint()
      updateEditorInfo()
    }
  })

  // 监听光标位置变化
  editor.onDidChangeCursorPosition(() => {
    updateEditorInfo()
  })
  
  // 初始检查和信息更新
  setTimeout(() => {
    performLinting()
    updateEditorInfo()
  }, 100)
})

// 监听语言变化
watch(() => props.language, (newLang) => {
  if (editor) {
    const model = editor.getModel()
    if (model) {
      monaco.editor.setModelLanguage(model, newLang)
      performLinting()
    }
  }
})

// 监听值变化（外部修改）
watch(() => props.modelValue, (newValue) => {
  if (editor && editor.getValue() !== newValue) {
    editor.setValue(newValue)
  }
})

onBeforeUnmount(() => {
  if (lintTimeout) clearTimeout(lintTimeout)
  completionProviders.forEach(p => p.dispose())
  editor?.dispose()
})
</script>

<style scoped>
.monaco-editor-wrapper {
  position: relative;
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
  background: #1e1e1e;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  background: #252526;
  border-bottom: 1px solid #3c3c3c;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.monaco-editor-container {
  width: 100%;
  height: v-bind(height);
  overflow: hidden;
}

.editor-statusbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
  padding: 4px 12px;
  background: #007acc;
  color: white;
  font-size: 12px;
}

.error-panel {
  position: absolute;
  bottom: 24px;
  left: 0;
  right: 0;
  max-height: 150px;
  background: #1e1e1e;
  border-top: 1px solid #454545;
  overflow-y: auto;
  z-index: 10;
}

.error-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #252526;
  border-bottom: 1px solid #454545;
  color: #cccccc;
  font-size: 12px;
}

.error-icon {
  font-size: 14px;
}

.error-list {
  padding: 4px 0;
}

.error-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 12px;
  color: #cccccc;
}

.error-item:hover {
  background: #2a2d2e;
}

.error-item.error {
  border-left: 3px solid #f14c4c;
}

.error-item.warning {
  border-left: 3px solid #cca700;
}

.error-line {
  color: #569cd6;
  min-width: 60px;
}

.error-message {
  flex: 1;
}

.error-badge {
  position: absolute;
  bottom: 32px;
  right: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(241, 76, 76, 0.9);
  color: white;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  z-index: 10;
}

.error-badge:hover {
  background: rgba(241, 76, 76, 1);
}

.badge-icon {
  font-size: 12px;
}

/* 工具栏按钮样式 */
:deep(.el-button) {
  --el-button-bg-color: #3c3c3c;
  --el-button-border-color: #3c3c3c;
  --el-button-text-color: #cccccc;
  --el-button-hover-bg-color: #4c4c4c;
  --el-button-hover-border-color: #4c4c4c;
  --el-button-hover-text-color: #ffffff;
}

:deep(.el-select) {
  --el-select-border-color-hover: #4c4c4c;
}

:deep(.el-input__wrapper) {
  background-color: #3c3c3c;
  box-shadow: none;
}

:deep(.el-input__inner) {
  color: #cccccc;
}

:deep(.el-divider--vertical) {
  border-color: #4c4c4c;
  margin: 0 8px;
}
</style>
