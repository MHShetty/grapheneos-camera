package app.grapheneos.camera.ui.composable.screen.serializer

import app.grapheneos.camera.ui.composable.model.VideoUri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object VideoUriSerializer : KSerializer<VideoUri> {

    private const val VIDEO_URI_ELEMENT_NAME = "VIDEO_URI"

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(javaClass.name) {
            element(VIDEO_URI_ELEMENT_NAME, UriSerializer.descriptor)
        }

    override fun deserialize(decoder: Decoder): VideoUri {
        return decoder.decodeStructure(descriptor) {
            check(decodeElementIndex(descriptor) == 0)
            val videoUri = decodeSerializableElement(descriptor, 0, UriSerializer)
            check(decodeElementIndex(descriptor) == CompositeDecoder.DECODE_DONE)
            VideoUri(videoUri)
        }
    }

    override fun serialize(encoder: Encoder, value: VideoUri) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, UriSerializer, value.uri)
        }
    }
}