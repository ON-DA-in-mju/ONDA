package com.onda.mju.student.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * public.notices
 * 실제 DB: id,title,content,author_id,created_at,updated_at,type,starts_at,ends_at,status,audience,view_count
 * (is_push 없는 DB 대비 select 에서 제외)
 */
@Serializable
data class NoticeDto(
    val id: String,
    val title: String = "",
    val content: String = "",
    val type: String = "GENERAL",
    val status: String = "PUBLISHED",
    @Serializable(with = AudienceListSerializer::class)
    val audience: List<String>? = null,
    @SerialName("starts_at")
    val startsAt: String? = null,
    @SerialName("ends_at")
    val endsAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("view_count")
    val viewCount: Int? = null,
)

/** text[] / JSON 배열 / "{STUDENT,DRIVER}" 문자열 모두 수용 */
object AudienceListSerializer : KSerializer<List<String>?> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("AudienceList")

    override fun deserialize(decoder: Decoder): List<String>? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        return parseAudience(jsonDecoder.decodeJsonElement())
    }

    override fun serialize(encoder: Encoder, value: List<String>?) {
        error("audience serialize not used")
    }

    private fun parseAudience(element: JsonElement): List<String>? {
        return when (element) {
            is JsonNull -> null
            is JsonArray -> element.mapNotNull { el ->
                (el as? JsonPrimitive)?.contentOrNull
            }
            is JsonPrimitive -> {
                val raw = element.contentOrNull?.trim().orEmpty()
                if (raw.isEmpty()) {
                    null
                } else {
                    raw.replace("[", "")
                        .replace("]", "")
                        .replace("{", "")
                        .replace("}", "")
                        .replace("\"", "")
                        .split(',', '|')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }
            }
            else -> null
        }
    }
}
