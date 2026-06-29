<template>
  <div class="knowledge-base" v-loading="loading">
    <div class="knowledge-base__header">
      <h2 class="knowledge-base__title">{{ T.PAGE_TITLE }}</h2>
    </div>

    <el-tabs v-model="activeTab" @tab-change="refresh">
      <el-tab-pane :label="T.TAB_UPLOAD" name="upload">
        <div class="knowledge-base__toolbar">
          <el-button type="primary" @click="uploadDialogVisible = true">
            {{ T.BTN_UPLOAD }}
          </el-button>
          <el-button v-if="!batchMode" @click="toggleBatch">
            {{ T.BTN_BATCH }}
          </el-button>
          <template v-if="batchMode">
            <el-button @click="toggleBatch">
              {{ T.BTN_CANCEL_BATCH }}
            </el-button>
            <el-button type="primary" :disabled="selectedFiles.length === 0" @click="importSelected">
              {{ T.BTN_IMPORT_SELECTED }}
            </el-button>
            <el-button type="danger" :disabled="selectedFiles.length === 0" @click="deleteSelected">
              {{ T.BTN_DELETE_SELECTED }}
            </el-button>
          </template>
          <div class="knowledge-base__toolbar-spacer" />
          <el-button @click="refresh">{{ T.BTN_REFRESH }}</el-button>
        </div>

        <div class="knowledge-base__list">
          <div v-for="file in uploadFiles" :key="file" class="knowledge-base__file-row">
            <el-checkbox
              v-if="batchMode"
              class="knowledge-base__file-checkbox"
              @change="(val) => val ? selectedFiles.push(file) : (selectedFiles = selectedFiles.filter(f => f !== file))"
            />
            <div class="knowledge-base__file-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
              </svg>
            </div>
            <span class="knowledge-base__file-name" :title="file">{{ file }}</span>
            <div class="knowledge-base__file-actions">
              <el-button size="small" type="primary" plain @click="importSingle(file)">
                {{ T.BTN_IMPORT }}
              </el-button>
              <el-button size="small" type="danger" plain @click="deleteSingle(file)">
                {{ T.BTN_DELETE }}
              </el-button>
            </div>
          </div>
          <el-empty v-if="uploadFiles.length === 0" :description="T.NO_FILES_UPLOAD" />
        </div>
      </el-tab-pane>

      <el-tab-pane :label="T.TAB_FINISH" name="finish">
        <div class="knowledge-base__toolbar">
          <el-button v-if="!batchMode" @click="toggleBatch">
            {{ T.BTN_BATCH }}
          </el-button>
          <template v-if="batchMode">
            <el-button @click="toggleBatch">
              {{ T.BTN_CANCEL_BATCH }}
            </el-button>
            <el-button type="danger" :disabled="selectedFiles.length === 0" @click="deleteSelected">
              {{ T.BTN_DELETE_SELECTED }}
            </el-button>
          </template>
          <div class="knowledge-base__toolbar-spacer" />
          <el-button @click="refresh">{{ T.BTN_REFRESH }}</el-button>
        </div>

        <div class="knowledge-base__list">
          <div v-for="file in finishFiles" :key="file" class="knowledge-base__file-row">
            <el-checkbox
              v-if="batchMode"
              class="knowledge-base__file-checkbox"
              @change="(val) => val ? selectedFiles.push(file) : (selectedFiles = selectedFiles.filter(f => f !== file))"
            />
            <div class="knowledge-base__file-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
              </svg>
            </div>
            <span class="knowledge-base__file-name" :title="file">{{ file }}</span>
            <div class="knowledge-base__file-actions">
              <el-button size="small" type="danger" plain @click="deleteSingle(file)">
                {{ T.BTN_DELETE }}
              </el-button>
            </div>
          </div>
          <el-empty v-if="finishFiles.length === 0" :description="T.NO_FILES_FINISH" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Upload Dialog -->
    <el-dialog v-model="uploadDialogVisible" :title="T.UPLOAD_DIALOG_TITLE" width="900px" :close-on-click-modal="false" :teleported="false" @closed="onDialogClose" class="knowledge-base__upload-dialog">
      <div class="knowledge-base__upload-drag">
        <el-upload
          ref="uploadRef"
          drag
          multiple
          :auto-upload="false"
          :on-change="onUploadFileChange"
          :show-file-list="false"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,.md"
        >
          <el-icon class="el-icon--upload" style="font-size: 48px; margin-bottom: 8px;">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
          </el-icon>
          <div class="el-upload__text">{{ T.UPLOAD_DRAG_TEXT }}</div>
          <template #tip>
            <div class="el-upload__tip">{{ T.UPLOAD_DRAG_HINT }}</div>
          </template>
        </el-upload>
      </div>

      <div v-if="uploadingFiles.length > 0" class="knowledge-base__selected-files">
        <div class="knowledge-base__selected-files-header">
          <p class="knowledge-base__selected-files-title">{{ T.SELECTED_FILES }}</p>
          <el-button size="small" type="danger" link @click="removeAll">
            {{ T.BTN_REMOVE_ALL }}
          </el-button>
        </div>
        <div class="knowledge-base__selected-files-list">
          <div v-for="(file, idx) in uploadingFiles" :key="idx" class="knowledge-base__selected-file-item">
            <span>{{ file.name || file }}</span>
            <el-button size="small" type="danger" link @click="removeFile(idx)" style="margin-left: auto;">
              {{ T.BTN_REMOVE }}
            </el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="uploadDialogVisible = false">{{ T.BTN_CANCEL }}</el-button>
        <el-button type="primary" :loading="uploading" @click="confirmUpload">
          {{ T.BTN_CONFIRM_UPLOAD }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useKnowledgeBase } from './KnowledgeBase.js'
import * as T from './Text.js'

const {
  loading, activeTab, uploadFiles, finishFiles,
  batchMode, selectedFiles, uploadingFiles,
  uploadDialogVisible, uploading,
  refresh, toggleBatch,
  uploadRef, onUploadFileChange, removeFile, removeAll, onDialogClose,
  confirmUpload,
  importSingle, importSelected,
  deleteSingle, deleteSelected
} = useKnowledgeBase()
</script>

<style scoped src="./KnowledgeBase.css"></style>
