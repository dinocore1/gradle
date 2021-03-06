/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.instantexecution.serialization.codecs

import org.gradle.api.internal.GeneratedSubclasses
import org.gradle.instantexecution.serialization.Codec
import org.gradle.instantexecution.serialization.IsolateContext
import org.gradle.instantexecution.serialization.PropertyTrace
import org.gradle.instantexecution.serialization.ReadContext
import org.gradle.instantexecution.serialization.WriteContext
import org.gradle.instantexecution.serialization.readClass
import org.gradle.instantexecution.serialization.withPropertyTrace
import org.gradle.instantexecution.serialization.writeClass


internal
class BeanCodec : Codec<Any> {

    override fun WriteContext.encode(value: Any) {
        val id = isolate.identities.getId(value)
        if (id != null) {
            writeSmallInt(id)
        } else {
            writeSmallInt(isolate.identities.putInstance(value))
            val beanType = GeneratedSubclasses.unpackType(value)
            writeClass(beanType)
            withBeanTrace(beanType) {
                beanPropertyWriterFor(beanType).run {
                    writeFieldsOf(value)
                }
            }
        }
    }

    override fun ReadContext.decode(): Any? {
        val id = readSmallInt()
        val previousValue = isolate.identities.getInstance(id)
        if (previousValue != null) {
            return previousValue
        }
        val beanType = readClass()
        return withBeanTrace(beanType) {
            beanPropertyReaderFor(beanType).run {
                val bean = newBean()
                isolate.identities.putInstance(id, bean)
                readFieldsOf(bean)
                bean
            }
        }
    }

    private
    inline fun <T : IsolateContext, R> T.withBeanTrace(beanType: Class<*>, action: () -> R): R =
        withPropertyTrace(PropertyTrace.Bean(beanType, trace)) {
            action()
        }
}
