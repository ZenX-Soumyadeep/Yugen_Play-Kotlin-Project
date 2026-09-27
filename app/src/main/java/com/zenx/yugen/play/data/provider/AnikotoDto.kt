package com.zenx.yugen.play.data.provider

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable
data class MapperServerDto(
    val sub: MapperLinkDto? = null,
    val dub: MapperLinkDto? = null
)

@Serializable
data class MapperLinkDto(
    val url: String? = null
)

@Serializable
data class MegaPlaySourcesDto(
    val enc: String? = null,
    @Serializable(with = MegaPlaySourcesFieldSerializer::class)
    val sources: String? = null,
    val tracks: List<MegaPlayTrackDto>? = null,
    val intro: MegaPlaySkipDto? = null,
    val outro: MegaPlaySkipDto? = null
)

@Serializable
data class MegaPlayTrackDto(
    val file: String,
    val label: String = ""
)

@Serializable
data class MegaPlaySkipDto(
    val start: Int = 0,
    val end: Int = 0
)

object MegaPlaySourcesFieldSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): String? {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return when (element) {
            is JsonObject -> element["file"]?.jsonPrimitive?.content
            is JsonArray -> element.firstOrNull()?.let {
                when (it) {
                    is JsonObject -> it["file"]?.jsonPrimitive?.content
                    is JsonPrimitive -> it.content
                    else -> null
                }
            }
            is JsonPrimitive -> element.content
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: String?): Unit =
        throw UnsupportedOperationException("Serialization not supported")
}
