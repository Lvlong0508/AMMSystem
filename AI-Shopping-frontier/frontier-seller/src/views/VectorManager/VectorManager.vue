<template>
  <div class="vector-manager" v-loading="loading">
    <div class="vector-manager__header">
      <h2 class="vector-manager__title">{{ T.PAGE_TITLE }}</h2>
      <p class="vector-manager__desc">{{ T.PAGE_DESC }}</p>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane :label="T.TAB_OVERVIEW" name="overview">
        <div class="vector-manager__stats">
          <div class="vector-manager__stat-card">
            <div class="vector-manager__stat-label">{{ T.STAT_DOCS }}</div>
            <div class="vector-manager__stat-value">{{ stats.totalDocs }}</div>
          </div>
          <div class="vector-manager__stat-card">
            <div class="vector-manager__stat-label">{{ T.STAT_CHUNKS }}</div>
            <div class="vector-manager__stat-value">{{ stats.totalChunks }}</div>
          </div>
          <div class="vector-manager__stat-card">
            <div class="vector-manager__stat-label">{{ T.STAT_COLLECTION }}</div>
            <div class="vector-manager__stat-value vector-manager__stat-value--sm">{{ stats.collectionName }}</div>
          </div>
          <div class="vector-manager__stat-card">
            <div class="vector-manager__stat-label">{{ T.STAT_DIMENSION }}</div>
            <div class="vector-manager__stat-value">{{ stats.dimension }}</div>
          </div>
        </div>

        <div class="vector-manager__section">
          <div class="vector-manager__section-header">
            <h3 class="vector-manager__section-title">{{ T.RECENT_IMPORTS }}</h3>
            <a class="vector-manager__section-link" @click="goToLibrary">{{ T.VIEW_ALL }}</a>
          </div>
          <el-table :data="recentDocs" class="vector-manager__table" stripe>
            <el-table-column prop="fileName" label="文件名" min-width="200" />
            <el-table-column prop="chunkCount" label="Chunks" width="100" />
            <el-table-column prop="importTime" label="导入时间" width="160" />
          </el-table>
          <el-empty v-if="recentDocs.length === 0" :description="T.NO_DOCUMENTS" />
        </div>
      </el-tab-pane>

      <el-tab-pane :label="T.TAB_LIBRARY" name="library">
        <div class="vector-manager__search-bar">
          <el-input
            v-model="searchQuery"
            :placeholder="T.SEARCH_PLACEHOLDER"
            class="vector-manager__search-input"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            </template>
          </el-input>
          <el-select v-model="topK" class="vector-manager__topk">
            <el-option label="Top 5" :value="5" />
            <el-option label="Top 10" :value="10" />
            <el-option label="Top 20" :value="20" />
          </el-select>
          <el-button type="primary" @click="handleSearch" :loading="isSearching">{{ T.BTN_SEARCH }}</el-button>
          <el-button v-if="searchResults" @click="clearSearch">{{ T.BTN_CLEAR }}</el-button>
        </div>

        <div v-if="searchResults" class="vector-manager__search-results">
          <div class="vector-manager__result-count">{{ T.SEARCH_RESULTS }}（{{ searchResults.length }}）</div>
          <div v-for="(item, idx) in searchResults" :key="idx" class="vector-manager__result-item">
            <div class="vector-manager__result-meta">
              <span class="vector-manager__result-source">{{ item.fileName }}</span>
              <span class="vector-manager__result-score">{{ T.SIMILARITY }} {{ (item.score * 100).toFixed(1) }}%</span>
            </div>
            <div class="vector-manager__result-content">{{ item.content }}</div>
          </div>
          <el-empty v-if="searchResults.length === 0" :description="T.NO_DOCUMENTS" />
        </div>

        <div v-else>
          <div class="vector-manager__toolbar">
            <span class="vector-manager__toolbar-title">{{ T.ALL_DOCUMENTS }}（{{ documents.length }}）</span>
            <el-button size="small" @click="refresh">{{ T.BTN_REFRESH }}</el-button>
          </div>
          <div v-if="documents.length > 0" class="vector-manager__doc-list">
            <div v-for="(doc, idx) in documents" :key="idx" class="vector-manager__doc-row">
              <div class="vector-manager__doc-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#4361ee" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              </div>
              <span class="vector-manager__doc-name">{{ doc.fileName }}</span>
              <span class="vector-manager__doc-chunks">{{ doc.chunkCount }} {{ T.LABEL_CHUNKS }}</span>
              <span class="vector-manager__doc-time">{{ doc.importTime }}</span>
              <el-button size="small" type="danger" link @click="deleteDocument(doc.fileName)">{{ T.BTN_DELETE }}</el-button>
            </div>
          </div>
          <el-empty v-else :description="T.NO_DOCUMENTS" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { useVectorManager } from './VectorManager.js'
import * as T from './Text.js'

const {
  loading, activeTab, stats, recentDocs,
  documents, searchQuery, topK, searchResults, isSearching,
  refresh, handleSearch, clearSearch, deleteDocument, goToLibrary
} = useVectorManager()
</script>

<style scoped src="./VectorManager.css"></style>
