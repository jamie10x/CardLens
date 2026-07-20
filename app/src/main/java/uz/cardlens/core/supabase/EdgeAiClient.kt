package uz.cardlens.core.supabase

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uz.cardlens.core.domain.Contact

class EdgeAiClient(
    private val functionBaseUrl: String
) {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    suspend fun generateFollowUp(contact: Contact): String {
        if (functionBaseUrl.isBlank()) return LocalFollowUpGenerator.generate(contact)
        return runCatching {
            client.post("${functionBaseUrl.trimEnd('/')}/generate-follow-up") {
                contentType(ContentType.Application.Json)
                setBody(
                    GenerateFollowUpRequest(
                        fullName = contact.fullName,
                        company = contact.company,
                        jobTitle = contact.jobTitle,
                        notes = contact.notes,
                        locationMet = contact.locationMet
                    )
                )
            }.body<GenerateFollowUpResponse>().message
        }.getOrElse {
            LocalFollowUpGenerator.generate(contact)
        }
    }
}

private object LocalFollowUpGenerator {
    fun generate(contact: Contact): String {
        val firstName = contact.fullName.split(" ").firstOrNull()?.ifBlank { contact.fullName } ?: contact.fullName
        val context = when {
            contact.locationMet.isNotBlank() -> " at ${contact.locationMet}"
            else -> ""
        }
        val topic = contact.notes.takeIf { it.isNotBlank() }
            ?: "your work at ${contact.company.ifBlank { "your company" }}"
        return "Hi $firstName, great meeting you$context. I enjoyed learning about $topic. " +
            "I would be happy to share a few ideas and continue the conversation. Are you available this week for a quick call?"
    }
}

@Serializable
private data class GenerateFollowUpRequest(
    val fullName: String,
    val company: String,
    val jobTitle: String,
    val notes: String,
    val locationMet: String
)

@Serializable
private data class GenerateFollowUpResponse(
    val message: String
)
