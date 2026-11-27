package com.jlees.mibudget.scan

import android.net.Uri
import com.jlees.mibudget.data.SettingsRepository
import java.time.LocalDate

/**
 * Reads the line items of a receipt for the check splitter. Uses Gemini Nano when it's picked (in Settings, or
 * by Advanced Rescan) and ready on this phone (much better at messy receipts), otherwise Google's text reader plus
 * [ReceiptItems.fromRows]. If Gemini Nano doesn't answer, the text reader's result is used.
 */
class ItemScanner(
    private val ocr: OcrEngine,
    private val nano: NanoScanner,
    private val settings: SettingsRepository,
) {
    data class Outcome(
        val receipt: ItemizedReceipt,
        /** Which reader produced it, for the "Read with …" note. */
        val readWith: String,
        /** Something worth telling the user (e.g. Gemini Nano didn't answer). */
        val note: String? = null,
        /** What the text reader found (rows, top to bottom). */
        val ocrText: String = "",
        /** The AI model's raw answer, if one was used. */
        val aiReply: String? = null,
    )

    /** [engine] = read with this scanner (rescans); null = the one picked in Settings. */
    suspend fun scan(uri: Uri, engine: ScanEngine? = null): Outcome {
        val engine = engine ?: settings.current().scanEngine
        val today = LocalDate.now()
        var note: String? = null
        var aiReply: String? = null

        if (engine == ScanEngine.GEMINI_NANO && nano.status() == NanoStatus.READY) {
            val image = ocr.prepareModelImage(uri)
            if (image != null) try {
                val reply = nano.read(image, today, ReceiptItems.AI_PROMPT, maxTokens = 768) // room for long checks
                aiReply = reply
                ReceiptItems.fromAiReply(reply.orEmpty())?.takeIf { it.items.isNotEmpty() }?.let {
                    return Outcome(it, engine.title, aiReply = reply)
                }
                // Say so instead of quietly showing a weaker result.
                note = "Read with Standard: ${engine.title} didn't answer"
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Fall through to the text reader.
            } finally {
                image.delete()
            }
        }

        // Google's reader reads the photo a few ways; keep the reading that found the most.
        val pages = ocr.scan(uri).pages
        val best = pages.map { p -> p to ReceiptItems.fromRows(p.lines.map { it.text }) }
            .maxByOrNull { (_, r) -> r.items.size * 2 + listOfNotNull(r.subtotalCents, r.totalCents, r.taxCents).size }
        return Outcome(
            best?.second ?: ItemizedReceipt(),
            ScanEngine.STANDARD.title,
            note,
            ocrText = best?.first?.lines.orEmpty().joinToString("\n") { it.text },
            aiReply = aiReply,
        )
    }
}
