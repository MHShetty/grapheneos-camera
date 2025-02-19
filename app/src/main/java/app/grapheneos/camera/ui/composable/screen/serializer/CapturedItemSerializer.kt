package app.grapheneos.camera.ui.composable.screen.serializer

import android.net.Uri
import app.grapheneos.camera.CapturedItem
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object CapturedItemSerializer : KSerializer<CapturedItem> {

    val ListSerializer = ListSerializer(CapturedItemSerializer)

    private const val ITEM_TYPE_ELEMENT_NAME = "ITEM_TYPE"
    private const val DATE_STRING_ELEMENT_NAME = "DATE_STRING"
    private const val URI_ELEMENT_NAME = "URI"

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(
            javaClass.name
        ) {
            element<Int>(ITEM_TYPE_ELEMENT_NAME)
            element<String>(DATE_STRING_ELEMENT_NAME)
            element(URI_ELEMENT_NAME, UriSerializer.descriptor)
        }

    override fun deserialize(decoder: Decoder): CapturedItem {
        return decoder.decodeStructure(
            descriptor
        ) {
            var itemType = 0
            var dateString = ""
            var uri = Uri.EMPTY

            while(true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> itemType = decodeIntElement(descriptor, 0)
                    1 -> dateString = decodeStringElement(descriptor, 1)
                    2 -> uri =  decodeSerializableElement(descriptor, 2, UriSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }

            CapturedItem(
                type = itemType,
                dateString = dateString,
                uri = uri
            )
        }
    }

    override fun serialize(encoder: Encoder, value: CapturedItem) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.type)
            encodeStringElement(descriptor, 1, value.dateString)
            encodeSerializableElement(descriptor, 2, UriSerializer, value.uri)
        }
    }
}