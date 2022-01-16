package org.ogenvik.avro4kcoder

import kotlinx.serialization.ExperimentalSerializationApi
import org.apache.beam.sdk.Pipeline
import org.reflections.Reflections

/**
 * Applies on classes that should be serialized in Apache Beam using the Avro4KCoder.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Avro4KCoded


/**
 * Registers all classes that have the @Avro4KCoded annotations to use Avro4KCoder.
 */
@ExperimentalSerializationApi
fun Pipeline.registerAvro4KCodec(packageName: String): Pipeline {

    Reflections(packageName).getTypesAnnotatedWith(Avro4KCoded::class.java).forEach {
        // logger.info { "Registering Avro4K codec for class ${it.name}" }

        this.coderRegistry.registerCoderForClass(
            it,
            Avro4KCoder(it)
        )
    }

    return this
}