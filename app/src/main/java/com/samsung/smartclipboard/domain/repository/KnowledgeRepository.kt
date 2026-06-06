package com.samsung.smartclipboard.domain.repository

interface KnowledgeRepository {
    suspend fun organize(): List<String>
}