/*
 * Copyright (c) 2021 scrazzz
 * Licensed under the MIT License
 */

package com.scruzism.plugins

import android.content.Context

import com.aliucord.Utils
import com.aliucord.Logger
import com.aliucord.api.CommandsAPI.CommandResult
import com.aliucord.entities.Plugin
import com.aliucord.Http
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.MessageEmbedBuilder

import com.discord.api.commands.ApplicationCommandType

private fun makeReq(query: String): Result? {
    val q = query.replace("\\s".toRegex(), "") // remove whitespaces

    val url = Http.QueryBuilder("https://www.wolframalpha.com/input/apiExplorer.jsp")
            .append("input", query).append("format", "plaintext").append("output", "JSON").append("type", "full")
            .toString()
    val resp = Http.Request(url, "GET")
            .setHeader("Accept", "*/*")
            .setHeader("Accept-Encoding", "gzip, deflate, br")
            .setHeader("Accept-Language", "en-US,en;q=0.9")
            .setHeader("Connection", "keep-alive")
            .setHeader("Host", "www.wolframalpha.com")
            .setHeader("Origin", "https://products.wolframalpha.com")
            .setHeader("Referer", "https://products.wolframalpha.com/")
            .setHeader("User-Agent", "Mozilla/5.0")
            .execute().json(Result::class.java)
    return resp
}

@AliucordPlugin
class Calc : Plugin() {

    private val LOGGER = Logger("Calc")

    override fun start(ctx: Context) {
        val args = listOf(
                Utils.createCommandOption(
                        ApplicationCommandType.STRING,
                        "input",
                        "Type your question",
                        required = true),
                Utils.createCommandOption(
                        ApplicationCommandType.BOOLEAN,
                        "send",
                        "Send to chat"),
                Utils.createCommandOption(
                        ApplicationCommandType.BOOLEAN,
                        "output only",
                        "Send response with output only")
        )

        commands.registerCommand(
                "calc",
                "Calculate your question using wolfram API",
                args
        ) { ctx ->
            val input = ctx.getRequiredString("input")
            val send = ctx.getBoolOrDefault("send", false)
            val isOutputOnly = ctx.getBoolOrDefault("output only", false)

            val http = try { makeReq(input) } catch (t: Throwable) {
                LOGGER.error(t)
                return@registerCommand CommandResult("an unknown error occured.", null, false)
            }

            val userInput = http!!.queryresult.inputstring
            val output = http.queryresult.pods[1].subpods[0].plaintext

            if (!send) {
                val embed = MessageEmbedBuilder()
                        .setAuthor("Wolfram")
                        .addField("Input", "`$userInput`", false)
                        .addField("Output", "`$output`", false)
                        .setRandomColor().build()
                return@registerCommand CommandResult(null, mutableListOf(embed), false)
            }

            if (!isOutputOnly) {
                val fmt = "Input: `$userInput`\n\nOutput: `$output`"
                return@registerCommand CommandResult(fmt, null, send)
            }

            CommandResult(output, null, send)

        }
    }

    override fun stop(ctx: Context) = commands.unregisterAll()

}
