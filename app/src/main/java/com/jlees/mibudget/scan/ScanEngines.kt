package com.jlees.mibudget.scan

/**
 * The scanners you can choose from (Settings → Scanner): the built-in Standard reader, or Gemini Nano, Google's
 * on-device AI that looks at the picture itself (part of some phones; Android manages its model).
 */
enum class ScanEngine(
    val title: String,
    val summary: String,
    /** Reads the picture itself (vision) vs. only the text the standard reader found. */
    val vision: Boolean = false,
    val speed: String,
) {
    STANDARD(
        title = "Standard",
        summary = "Google's text reader plus mi. Budget's rules. Built in, works on every phone.",
        speed = "about 1 s",
    ),
    GEMINI_NANO(
        title = "Gemini Nano",
        summary = "Google's on-device AI, built into Android on supported phones. Looks at the picture. mi. Budget checks automatically whether this phone has it; Android manages the model, so nothing is stored in mi. Budget.",
        vision = true,
        speed = "about 2–4 s",
    ),
}
