/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.internal.declarativedsl.schemaBuilder

import org.gradle.internal.declarativedsl.analysis.DataTypeRefImpl
import org.gradle.internal.declarativedsl.analysis.FqNameImpl
import org.gradle.internal.declarativedsl.analysis.ref
import org.gradle.internal.declarativedsl.language.DataTypeImpl
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty
import kotlin.reflect.KType


fun KClassifier.toDataTypeRef(): DataTypeRefImpl =
    when (this) {
        Unit::class -> DataTypeImpl.UnitType.ref
        Int::class -> DataTypeImpl.IntType.ref
        String::class -> DataTypeImpl.StringType.ref
        Boolean::class -> DataTypeImpl.BooleanType.ref
        Long::class -> DataTypeImpl.LongType.ref
        is KClass<*> -> DataTypeRefImpl.Name(FqNameImpl.parse(checkNotNull(qualifiedName)))
        else -> error("unexpected type")
    }


internal
fun checkInScope(
    type: KType,
    typeScope: DataSchemaBuilder.PreIndex
) {
    if (type.classifier?.isInScope(typeScope) != true) {
        error("type $type used in a function is not in schema scope")
    }
}


private
fun KClassifier.isInScope(typeScope: DataSchemaBuilder.PreIndex) =
    isBuiltInType || this is KClass<*> && typeScope.hasType(this)


private
val KClassifier.isBuiltInType: Boolean
    get() = when (this) {
        Int::class, String::class, Boolean::class, Long::class, Unit::class -> true
        else -> false
    }


val KCallable<*>.annotationsWithGetters: List<Annotation>
    get() = this.annotations + if (this is KProperty) this.getter.annotations else emptyList()


fun KType.toDataTypeRefOrError() =
    toDataTypeRef() ?: error("failed to convert type $this to data type")


private
fun KType.toDataTypeRef(): DataTypeRefImpl? = when {
    // isMarkedNullable -> TODO: support nullable types
    arguments.isNotEmpty() -> null // TODO: support for some particular generic types
    else -> when (val classifier = classifier) {
        null -> null
        else -> classifier.toDataTypeRef()
    }
}
