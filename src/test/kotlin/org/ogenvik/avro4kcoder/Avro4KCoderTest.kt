package org.ogenvik.avro4kcoder

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be instance of`
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.Coder
import org.apache.beam.sdk.util.CoderUtils
import org.apache.beam.sdk.util.SerializableUtils
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.TypeDescriptor
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Avro4KCoded
@Serializable
data class TestClassWithAnnotation4K(val aValue: String, val couldBeNull: Int?)

@ExperimentalSerializationApi
object Avro4KCoderTest : Spek({

    fun <T> Coder<T>.encodeAndDecode(entry: T): T {
        val outStream = ByteArrayOutputStream()
        encode(entry, outStream)
        val inStream = ByteArrayInputStream(outStream.toByteArray())
        return decode(inStream)
    }


    describe("Avro4KCoder") {
        it("should register codec") {
            val p = Pipeline.create().registerAvro4KCodec("org.ogenvik.avro4kcoder")
            p.coderRegistry.getCoder(TestClassWithAnnotation4K::class.java)
                .`should be instance of`(Avro4KCoder::class.java)
        }

        it("should serialize and deserialize") {
            val p = Pipeline.create().registerAvro4KCodec("org.ogenvik.avro4kcoder")
            val coder = p.coderRegistry.getCoder(TestClassWithAnnotation4K::class.java)
            TestClassWithAnnotation4K("the value being tested", null).also { tested ->
                val decoded = coder.encodeAndDecode(tested)
                tested `should be equal to` decoded
            }
            TestClassWithAnnotation4K("another value being tested", 10).also { tested ->
                val decoded = coder.encodeAndDecode(tested)

                tested `should be equal to` decoded
            }

        }
        it("should be serializable") {
            val coder = Avro4KCoder(TestClassWithAnnotation4K::class.java)
            SerializableUtils.ensureSerializable(coder)
        }
        it("should clone nested") {
            val p = Pipeline.create().registerAvro4KCodec("org.ogenvik.avro4kcoder")
            val tested =
                KV.of(TestClassWithAnnotation4K("key", 1), TestClassWithAnnotation4K("data", null))
            val coder = p.coderRegistry.getCoder(object :
                TypeDescriptor<KV<TestClassWithAnnotation4K, TestClassWithAnnotation4K>>() {})
            val cloned = CoderUtils.clone(coder, tested)
            tested `should be equal to` cloned
        }
        it("should work after serialization") {
            val coder = Avro4KCoder(TestClassWithAnnotation4K::class.java)
            val coderCopy = SerializableUtils.clone(coder)
            TestClassWithAnnotation4K("the value being tested", null).also { tested ->
                val decoded = coderCopy.encodeAndDecode(tested)
                tested `should be equal to` decoded
            }

        }
    }
})