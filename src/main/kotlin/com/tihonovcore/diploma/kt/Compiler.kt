//package com.tihonovcore.diploma.kt
//
//import com.google.gson.JsonParser
//import com.tihonovcore.diploma.kt.legacy.DiplomaConfiguration
//import com.tihonovcore.diploma.kt.legacy.actions.extractPaths
//import com.tihonovcore.diploma.kt.legacy.actions.onPredict
//import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
//import java.io.File
//
//class Compiler {
//    fun run() {
//        //todo: create environment
//        val environment = KotlinCoreEnvironment.createForProduction(
//            disposable,
//            cfg,
//            EnvironmentConfigFiles.JVM_CONFIG_FILES
//        )
//
//        //todo: register services
//
//        val requestText = File(DiplomaConfiguration.requestJson).readText()
//        val (requestType, request) = JsonParser.parseString(requestText).asJsonObject.let { Pair(it["request_type"].asString, it["request"]) }
//
//        when (requestType) {
//            "EXTRACT_PATHS" -> extractPaths(environment.project, request)
//            "ON_PREDICT" -> onPredict(environment.project, request)
//            else -> throw IllegalArgumentException("Wrong request type: $requestType")
//        }
//    }
//}