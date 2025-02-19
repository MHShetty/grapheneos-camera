package app.grapheneos.camera.ui.composable.screen.serializer

import android.net.Uri

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object UriSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor
        get() =
            buildClassSerialDescriptor(javaClass.name) {
                element<String>("scheme")
                element<String>("host")
                element<Int>("port")
                element<String>("path")
                element<String>("query")
                element<String>("fragment")
            }

    override fun deserialize(decoder: Decoder): Uri {
        var scheme: String? = null
        var host: String? = null
        var port = -1
        var path: String? = null
        var query: String? = null
        var fragment: String? = null

        decoder.decodeStructure(
            descriptor
        ) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> scheme = decodeStringElement(descriptor, 0)
                    1 -> host = decodeStringElement(descriptor, 1)
                    2 -> port = decodeIntElement(descriptor, 2)
                    3 -> path = decodeStringElement(descriptor, 3)
                    4 -> query = decodeStringElement(descriptor, 4)
                    5 -> fragment = decodeStringElement(descriptor, 5)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected error at $index")
                }
            }
        }

        val uri = Uri.Builder()
            .apply {
                if (scheme != null) scheme(scheme)
                if (host != null) {
                    if (port != -1) authority("$host:$port")
                    else authority(host)
                }
                if (path != null) path(path)
                if (query != null) query(query)
                if (fragment != null) fragment(fragment)
            }
            .build()

        return uri
    }

    override fun serialize(encoder: Encoder, value: Uri) {
        encoder.encodeStructure(descriptor) {
            value.scheme?.let {
                encodeStringElement(descriptor, 0, it)
            }

            value.host?.let {
                encodeStringElement(descriptor, 1, it)
            }

            if (value.port != -1) {
                encodeIntElement(descriptor, 2, value.port)
            }

            value.path?.let {
                encodeStringElement(descriptor, 3, it)
            }

            value.query?.let {
                encodeStringElement(descriptor, 4, it)
            }

            value.fragment?.let {
                encodeStringElement(descriptor,5, it)
            }
        }
    }
}