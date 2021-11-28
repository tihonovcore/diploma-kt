package com.tihonovcore.diploma.kt

import com.google.gson.JsonParser
import com.intellij.openapi.util.Disposer
import com.tihonovcore.diploma.kt.legacy.DiplomaConfiguration
import com.tihonovcore.diploma.kt.legacy.actions.extractPaths
import com.tihonovcore.diploma.kt.legacy.actions.onPredict
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

object Compiler {
    private val configuration: CompilerConfiguration
    private val environment: KotlinCoreEnvironment

    init {
        configuration = CompilerConfiguration()

        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        //todo: fill configuration

        environment = KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable(),
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        //todo: register services
    }

    fun doActionByRequestFile() {
        val requestText = File(DiplomaConfiguration.requestJson).readText()
        val (requestType, request) = JsonParser.parseString(requestText).asJsonObject.let { Pair(it["request_type"].asString, it["request"]) }

        when (requestType) {
            "EXTRACT_PATHS" -> extractPaths(environment.project, request)
            "ON_PREDICT" -> onPredict(environment.project, request)
            else -> throw IllegalArgumentException("Wrong request type: $requestType")
        }
    }

    fun finally() = Disposer.dispose(environment.project)
}
