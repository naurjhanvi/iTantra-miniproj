package com.itantra.transceiver

/** Boundaries for locally provisioned, open-source model runtimes. No network inference is permitted. */
interface VoiceActivityDetector { fun isSpeech(pcm16k: ShortArray): Boolean }
interface SpeechRecognizer { suspend fun transcribe(pcm16k: ShortArray, language: String): String }
interface SpeechSynthesizer { suspend fun synthesize(text: String, language: String): ShortArray }
interface KeywordSpotter { fun detect(pcm16k: ShortArray): Boolean }

data class ModelPack(val language: String, val sttPath: String, val ttsPath: String, val installed: Boolean)
class ModelPackRegistry {
    private val packs = mutableMapOf<String, ModelPack>()
    fun register(pack: ModelPack) { packs[pack.language] = pack }
    fun installed(): List<ModelPack> = packs.values.filter { it.installed }
}
