package com.example.live

data class VoiceProfile(
    val id: String,
    val name: String,
    val gender: String,
    val style: String,
    val description: String,
    val tag: String,
    val accent: String
)

object VoiceRegistry {
    val VOICES = listOf(
        VoiceProfile(
            id = "Aoede",
            name = "Aoede",
            gender = "Female",
            style = "Warm & Empathetic",
            description = "Natural, expressive, and warm voice. Ideal for conversational Hindi & English.",
            tag = "Recommended • Most Natural",
            accent = "Warm Hindi / English"
        ),
        VoiceProfile(
            id = "Kore",
            name = "Kore",
            gender = "Female",
            style = "Calm & Gentle",
            description = "Soothing, gentle, and clear tone. Relaxed and polite companion.",
            tag = "Smooth & Relaxed",
            accent = "Soft Conversational"
        ),
        VoiceProfile(
            id = "Leda",
            name = "Leda",
            gender = "Female",
            style = "Bright & Cheerful",
            description = "Enthusiastic, cheerful, and vivid female voice with expressive inflections.",
            tag = "Lively & Expressive",
            accent = "Youthful & Bright"
        ),
        VoiceProfile(
            id = "Charon",
            name = "Charon",
            gender = "Male",
            style = "Deep & Confident",
            description = "Deep, resonant, mature, and confident male voice. Strong and reliable.",
            tag = "Deep Resonance",
            accent = "Mature Male"
        ),
        VoiceProfile(
            id = "Puck",
            name = "Puck",
            gender = "Male",
            style = "Upbeat & Friendly",
            description = "Modern, energetic, quick-witted, and casual companion tone.",
            tag = "Fast & Dynamic",
            accent = "Casual & Energetic"
        ),
        VoiceProfile(
            id = "Fenrir",
            name = "Fenrir",
            gender = "Male",
            style = "Crisp & Direct",
            description = "Sharp, focused, and articulate male voice. Direct and efficient responses.",
            tag = "Authoritative & Sharp",
            accent = "Articulate & Direct"
        ),
        VoiceProfile(
            id = "Orus",
            name = "Orus",
            gender = "Male",
            style = "Mellow & Natural",
            description = "Balanced, smooth, and natural male voice for everyday hands-free tasks.",
            tag = "Balanced & Mellow",
            accent = "Natural Everyday"
        )
    )

    fun getVoice(id: String): VoiceProfile {
        return VOICES.find { it.id.equals(id, ignoreCase = true) } ?: VOICES.first()
    }
}
