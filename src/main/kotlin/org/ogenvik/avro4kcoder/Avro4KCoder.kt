package org.ogenvik.avro4kcoder

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.io.AvroBinaryOutputStream
import com.github.avrokotlin.avro4k.io.SchemalessAvroInputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializerOrNull
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.BinaryDecoder
import org.apache.avro.io.DecoderFactory
import org.apache.beam.sdk.coders.CustomCoder
import java.io.InputStream
import java.io.OutputStream


class AvroDirectBinaryInputStream<T>(
    input: InputStream,
    converter: (Any) -> T,
    writerSchema: Schema,
    readerSchema: Schema
) :
    SchemalessAvroInputStream<T>(input, converter, writerSchema, readerSchema) {

    //Must be a DirectBinaryDecoder to avoid us consuming the whole stream; we only want to advance it for our specific value.
    override val decoder: BinaryDecoder = DecoderFactory.get().directBinaryDecoder(input, null)
}


/**
 * An Avro Coder which understands Kotlin classes, allowing us to use non-nullability and val fields, and removing the need to
 * have an empty constructor.
 */
@ExperimentalSerializationApi
class Avro4KCoder<T>(private val klass: Class<T>) : CustomCoder<T>() {


    /**
     * Schema is not serializable with Avro 1.8 (but is with 1.11).
     */
    @Transient
    private var _schema: Schema? = null
    private val schema: Schema
        get() {
            //No thread lock here; we can live with multiple threads creating instances at once.
            if (_schema == null) {
                _schema = Avro.default.schema(serializer)
            }
            return _schema!!
        }

    /**
     * KSerializer is not serializable.
     */
    @Transient
    private var _serializer: KSerializer<T>? = null
    private val serializer: KSerializer<T>
        get() {
            //No thread lock here; we can live with multiple threads creating instances at once.
            if (_serializer == null) {
                _serializer = serializerOrNull(klass) as KSerializer<T>?
                    ?: throw IllegalArgumentException("The class '$klass' is marked with @Avro4KCoded but has no serialization generated. Make sure to both mark it with @Serializable and that the Kotlin Serialization plugin is enabled in Gradle.")
            }
            return _serializer!!
        }

//    /**
//     * Schema is not serializable, so we'll fetch it lazily and store it transiently.
//     */
//    @delegate:Transient
//    private val schema by lazy { Avro.default.schema(serializer) }
//
//    /**
//     * KSerializer is not serializable, so we'll fetch it lazily and store it transiently.
//     */
//    @delegate:Transient
//    private val serializer by lazy { getSerializer(klass) }

    private val converter = { record: Any ->
        Avro.default.fromRecord(serializer, record as GenericRecord)
    }

    override fun encode(value: T?, outStream: OutputStream) {
        val outputStream = AvroBinaryOutputStream(outStream, serializer, schema)
        if (value != null) {
            outputStream.write(value)
        } else {
            //This really doesn't do anything...
            outputStream.encoder.writeNull()
        }
        outputStream.flush()
    }

    override fun decode(inStream: InputStream): T? {
        return AvroDirectBinaryInputStream(inStream, converter, schema, schema).next()
    }

    override fun verifyDeterministic() {
        //No op.

        //TODO: actually look up this
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is Avro4KCoder<*>) {
            return false
        }
        return (this.klass == other.klass)
    }

    override fun hashCode(): Int {
        return klass.hashCode()
    }

}