import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // ============================================
      // 配置A：原单体应用后端 (端口8080)
      // ============================================
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api')
      }

      // ============================================
      // 配置B：Eureka微服务版后端 (端口8081-8085)
      // 如需使用，请注释上面的'/api'配置，并取消下面注释
      // ============================================
      // '/api/product': { target: 'http://localhost:8081', changeOrigin: true },
      // '/api/order': { target: 'http://localhost:8082', changeOrigin: true },
      // '/api/contact': { target: 'http://localhost:8083', changeOrigin: true },
      // '/api/logistics': { target: 'http://localhost:8084', changeOrigin: true },
      // '/api/chat': { target: 'http://localhost:8085', changeOrigin: true },
    }
  }
})