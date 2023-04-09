package com.banno.gordon

import arrow.core.Either
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.dexbacked.DexBackedAnnotationElement
import org.jf.dexlib2.iface.Annotatable
import org.jf.dexlib2.iface.BasicAnnotation
import org.jf.dexlib2.iface.reference.TypeReference
import java.io.File

internal fun loadTestSuite(instrumentationApk: File): Either<Throwable, List<TestCase>> = Either.catch {
    var testSuitsWithTestClasses = instrumentationApk
        .loadClasses()
        .filter { it.isTestSuite }
        .flatMap { classDef ->
            classDef.annotations.map { classDef to it }
        }
        .filter { (_, annotation) ->
            annotation.name.endsWith("\$SuiteClasses")
        }
        .flatMap { (classDef, annotation) ->
            annotation.elements.flatMap { element ->
                element.parameterClasses.map { parameterClassName ->
                    classDef.name to parameterClassName
                }
            }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.toSet() }

    while (testSuitsWithTestClasses.values.flatten().any { testSuitsWithTestClasses.contains(it) }) {
        testSuitsWithTestClasses.mapValues { (_, testClasses) ->
            testClasses.flatMap { testClass ->
                testSuitsWithTestClasses[testClass] ?: listOf(testClass)
            }.toSet()
        }.let { testSuitsWithTestClasses = it.toMap() }
    }

    instrumentationApk
        .loadClasses()
        .filter {
            val isKotlinInterfaceDefaultImplementation = it.name.endsWith("\$DefaultImpls")
            val isInterface = (it.accessFlags and AccessFlags.INTERFACE.value != 0)
            val isAbstract = (it.accessFlags and AccessFlags.ABSTRACT.value != 0)

            !isInterface &&
                !isAbstract &&
                !isKotlinInterfaceDefaultImplementation
        }
        .flatMap { classDef ->
            classDef.methods
                .mapNotNull { method ->
                    if (method.isTestMethod) {
                        TestCase(
                            fullyQualifiedClassName = classDef.name,
                            methodName = method.name,
                            isIgnored = method.isIgnored || classDef.isIgnored,
                            annotations = classDef.annotationNames + method.annotationNames + testSuitsWithTestClasses.keysContainingValue(classDef.name)
                        )
                    } else null
                }
        }
}

private fun File.loadClasses() =
    DexFileFactory
        .loadDexContainer(this, null)
        .run { dexEntryNames.map { getEntry(it)!!.dexFile } }
        .flatMap { it.classes }

private val TypeReference.name
    get() = type.classNameFromTypeDescriptor()

private fun String.classNameFromTypeDescriptor() =
    drop(1).dropLast(1).replace('/', '.')

private val BasicAnnotation.name
    get() = type.drop(1).dropLast(1).replace('/', '.')

private val Annotatable.annotationNames
    get() = annotations.map { it.name }.toSet() - "kotlin.Metadata" - "dalvik.annotation.MemberClasses"

private val Annotatable.isIgnored
    get() = annotationNames.any { it == "org.junit.Ignore" }

private val Annotatable.isTestSuite
    get() = annotationNames.any { it == "org.junit.runners.Suite\$SuiteClasses" }

private val Annotatable.isTestMethod
    get() = annotationNames.any { it == "org.junit.Test" }

private val DexBackedAnnotationElement.parameterClasses
    get() = value.toString()
        .replace("Array[", "")
        .replace("]", "")
        .split(", ")
        .filterNot { it.isBlank() }
        .map { it.classNameFromTypeDescriptor() }

private fun Map<String, Set<String>>.keysContainingValue(value: String) =
    filterValues { it.contains(value) }.keys