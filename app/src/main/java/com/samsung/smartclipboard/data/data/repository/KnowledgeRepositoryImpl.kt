package com.samsung.smartclipboard.data.repository

import com.samsung.smartclipboard.database.dao.KnowledgeDao
import com.samsung.smartclipboard.domain.repository.KnowledgeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgeRepositoryImpl @Inject constructor(
    private val knowledgeDao: KnowledgeDao
) : KnowledgeRepository {

    override suspend fun organize(): List<String> {
        val data = knowledgeDao.getAll()

        val grouped = data.groupBy { it.groupKey }

        return grouped.map { (key, items) ->
            buildString {
                append("📌 $key\n")
                append("(${items.size}개)\n\n")

                items.forEach { item ->
                    append("- ${item.title}\n")
                    append("  ${item.summary}\n\n")
                }
            }
        }
    }
}