package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.UserNameUpdateDto
import com.onda.mju.student.data.remote.dto.UserProfileDto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.Instant

class UserProfileRepository {

    fun currentUserId(): String? =
        runCatching {
            SupabaseClientProvider.client.auth.currentUserOrNull()?.id
        }.getOrNull()

    suspend fun getMine(): UserProfileDto? {
        val uid = currentUserId() ?: return null
        return SupabaseClientProvider.client
            .from("users")
            .select(columns = Columns.raw("id, name, email, student_no")) {
                filter { eq("id", uid) }
                limit(count = 1)
            }
            .decodeSingleOrNull()
    }

    suspend fun updateName(name: String): UserProfileDto {
        val uid = currentUserId() ?: error("로그인이 필요합니다.")
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "이름을 입력해주세요." }
        return SupabaseClientProvider.client
            .from("users")
            .update(
                UserNameUpdateDto(
                    name = trimmed,
                    updatedAt = Instant.now().toString(),
                ),
            ) {
                filter { eq("id", uid) }
                select(columns = Columns.raw("id, name, email, student_no"))
            }
            .decodeSingle()
    }
}
