package com.sowarapp.android.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads the catalog's "items" list but keeps only types this version of the app can show.
 * That way, adding videos to the catalog later won't break older installed versions.
 */
object ImageListSerializer : KSerializer<List<SowarImage>> {
    private val listSerializer = ListSerializer(SowarImage.serializer())

    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<SowarImage> {
        val jsonDecoder = decoder as JsonDecoder
        val array = jsonDecoder.decodeJsonElement() as JsonArray
        return array
            .filter { element ->
                val type = (element as? JsonObject)?.get("type")?.jsonPrimitive?.contentOrNull
                type == null || type == SowarImage.TYPE_IMAGE
            }
            .map { jsonDecoder.json.decodeFromJsonElement(SowarImage.serializer(), it) }
    }

    override fun serialize(encoder: Encoder, value: List<SowarImage>) =
        listSerializer.serialize(encoder, value)
}
