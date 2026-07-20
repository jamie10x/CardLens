package uz.cardlens.core.ocr

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import uz.cardlens.core.common.AppResult
import uz.cardlens.core.common.DataError
import uz.cardlens.core.domain.ContactDraft
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitOcrProcessor(
    private val context: Context,
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun readCard(uri: Uri): AppResult<OcrResult, DataError.Local> {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val text = recognizer.process(image).awaitTask().text
            AppResult.Success(
                OcrResult(
                    rawText = text,
                    draft = CardTextParser.parse(text).copy(cardImageUri = uri.toString())
                )
            )
        } catch (_: Exception) {
            AppResult.Error(DataError.Local.UNKNOWN)
        }
    }
}

data class OcrResult(
    val rawText: String,
    val draft: ContactDraft
)

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        continuation.resume(result)
    }
    addOnFailureListener { error ->
        continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}

object CardTextParser {
    private val emailRegex = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    private val phoneRegex = Regex("(\\+?\\d[\\d\\s().-]{7,}\\d)")
    private val websiteRegex = Regex("((https?://)?(www\\.)?[a-z0-9-]+\\.[a-z]{2,}(/[\\w./-]*)?)", RegexOption.IGNORE_CASE)

    fun parse(text: String): ContactDraft {
        val lines = text.lines()
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val email = emailRegex.find(text)?.value.orEmpty()
        val phone = phoneRegex.find(text)?.value.orEmpty()
        val website = websiteRegex.findAll(text)
            .asSequence()
            .map { it.value }
            .firstOrNull { !it.contains("@") }
            .orEmpty()

        val contactLines = lines.filterNot { line ->
            line.contains(email, ignoreCase = true) ||
                line.contains(phone, ignoreCase = true) ||
                line.contains(website, ignoreCase = true)
        }

        val name = contactLines.firstOrNull { line ->
            (line.count { it.isWhitespace() } <= 3) &&
                line.any { it.isLetter() } &&
                (!line.contains("LLC", ignoreCase = true)) &&
                (!line.contains("Inc", ignoreCase = true))
        }.orEmpty()

        val titleIndex = contactLines.indexOfFirst { it == name } + 1
        val title = contactLines.getOrNull(titleIndex)
            ?.takeIf { it.length < 50 && !it.contains(",") }
            .orEmpty()
        val company = contactLines
            .drop(titleIndex + 1)
            .firstOrNull { it.length < 60 }
            .orEmpty()

        val address = lines.lastOrNull { line ->
            line.contains(",") && line.any { it.isDigit() || it.isLetter() }
        }.orEmpty()

        return ContactDraft(
            fullName = name,
            jobTitle = title,
            company = company,
            email = email,
            phone = phone,
            website = website,
            address = address
        )
    }
}
