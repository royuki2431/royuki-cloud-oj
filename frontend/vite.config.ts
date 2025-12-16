import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
    plugins: [
        vue(),
        AutoImport({
            resolvers: [ElementPlusResolver()],
            imports: ['vue', 'vue-router', 'pinia'],
        }),
        Components({
            resolvers: [ElementPlusResolver()],
        }),
    ],
    optimizeDeps: {
        // 强制预构建 Element Plus 所有子依赖，避免逐次优化
        include: [
            'element-plus',
            'element-plus/es/components/loading/style/css',
            'element-plus/es/components/alert/style/css',
            'element-plus/es/components/form/style/css',
            'element-plus/es/components/form-item/style/css',
            'element-plus/es/components/input/style/css',
            'element-plus/es/components/dialog/style/css',
            'element-plus/es/components/collapse/style/css',
            'element-plus/es/components/collapse-item/style/css',
            'element-plus/es/components/table/style/css',
            'element-plus/es/components/table-column/style/css',
            'element-plus/es/components/link/style/css',
            'element-plus/es/components/divider/style/css',
            'element-plus/es/components/descriptions/style/css',
            'element-plus/es/components/descriptions-item/style/css',
            'element-plus/es/components/card/style/css',
            'element-plus/es/components/empty/style/css',
            'element-plus/es/components/row/style/css',
            'element-plus/es/components/col/style/css',
            'element-plus/es/components/progress/style/css',
            'element-plus/es/components/pagination/style/css',
            'element-plus/es/components/select/style/css',
            'element-plus/es/components/option/style/css',
            'element-plus/es/components/tabs/style/css',
            'element-plus/es/components/tab-pane/style/css',
            'element-plus/es/components/tooltip/style/css'
        ]
    },
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src'),
        },
    },
    css: {
        preprocessorOptions: {
            scss: {
                api: 'modern-compiler', // 使用现代编译器API
            },
        },
    },
    server: {
        port: 3000,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
})
