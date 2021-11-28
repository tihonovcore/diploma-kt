/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.tihonovcore.diploma.kt.legacy.actions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.checkers.ReferenceVariantsProvider
import org.jetbrains.kotlin.checkers.diagnostics.ActualDiagnostic
import org.jetbrains.kotlin.checkers.diagnostics.factories.DebugInfoDiagnosticFactory1
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.checkers.utils.DiagnosticsRenderingConfiguration
import org.jetbrains.kotlin.checkers.utils.TypedNode
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Severity
import com.tihonovcore.diploma.kt.legacy.stackTraceToString
import com.tihonovcore.diploma.kt.legacy.types.JsonClassSpec
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.resolve.getDataFlowValueFactory
import org.jetbrains.kotlin.idea.resolve.getLanguageVersionSettings
import org.jetbrains.kotlin.psi.KtFile
//import org.jetbrains.kotlin.test.KotlinTestUtils

fun getMapPsiToTypeId(
    class2spec: Map<ClassifierDescriptor, JsonClassSpec>,
    typedNodes: List<TypedNode>
): Map<PsiElement, Int> {
    return typedNodes.mapNotNull {
        val type = it.type ?: return@mapNotNull null
        val node = it.node
        val typeId = class2spec[type]?.id ?: return@mapNotNull null
        node to typeId
    }.associate { it }
}

fun checkFileSkipErrors(file: KtFile): Pair<MutableList<TypedNode>, Boolean> {
    return try {
        checkFile(file)
    } catch (e: Throwable) {
        println(e.message)
        println(e.stackTraceToString())
        Pair(mutableListOf(), true)
    }
}

fun checkFile(file: KtFile): Pair<MutableList<TypedNode>, Boolean> {
    val resolutionFacade = file.getResolutionFacade()
    val (bindingContext, moduleDescriptor) = resolutionFacade.analyzeWithAllCompilerChecks(listOf(file))

    //TODO: try to remove ":compiler:tests-common" dependency and copy nessessary code to utils
//    val directives = KotlinTestUtils.parseDirectives(file.text)
//    val diagnosticsFilter = BaseDiagnosticsTest.parseDiagnosticFilterDirective(directives, allowUnderscoreUsage = false)

    ReferenceVariantsProvider.registerInstance(
        ReferenceVariantsHelper(
            bindingContext,
            resolutionFacade,
            moduleDescriptor,
            { _ -> true }
        )
    )

    val extractedTypes = mutableListOf<TypedNode>()
    val actualDiagnostics = CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(
        bindingContext,
        file,
        markDynamicCalls = false,
        dynamicCallDescriptors = mutableListOf(),
        configuration = DiagnosticsRenderingConfiguration(
            platform = null, // we don't need to attach platform-description string to diagnostic here
            withNewInference = false,
            languageVersionSettings = resolutionFacade.getLanguageVersionSettings()
        ),
        dataFlowValueFactory = resolutionFacade.getDataFlowValueFactory(),
        moduleDescriptor = moduleDescriptor as ModuleDescriptorImpl,
        typedNodes = extractedTypes
    ) //.filter { diagnosticsFilter.value(it.diagnostic) }

    print("")

//        val actualTextWithDiagnostics = CheckerTestUtil.addDiagnosticMarkersToText(
//            file,
//            actualDiagnostics,
//            diagnosticToExpectedDiagnostic = emptyMap(),
//            getFileText = { it.text },
//            uncheckedDiagnostics = emptyList(),
//            withNewInferenceDirective = false,
//            renderDiagnosticMessages = true,
//            range2type
//        ).toString()

//        fun write(suffix: String, text: String) {
//            val outputDirName = expectedFile.parentFile.absolutePath
//            val outputFileName = expectedFile.nameWithoutExtension + ".txt"
//
//            File("$outputDirName$suffix/$outputFileName").apply {
//                parentFile.mkdirs()
//                writeText(text)
//            }
//        }
//
//        write("_dumped_types", actualTextWithDiagnostics)
//        write("_ti", DebugInfoDiagnosticFactory1.recordedTypes.map { (type, info) -> "${type}: ${info.first}, ${info.second}" }.joinToString("\n"))

    DebugInfoDiagnosticFactory1.recordedTypes.clear()

    return Pair(extractedTypes, hasCompileErrors(actualDiagnostics))
}

fun hasCompileErrors(diagnostics: List<ActualDiagnostic>): Boolean {
    return diagnostics.any { it.diagnostic.severity === Severity.ERROR }
}
