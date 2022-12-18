package eu.kanade.tachiyomi.multisrc.heancms

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Locale

@Serializable
data class HeanCmsSearchDto(
    val data: List<HeanCmsSeriesDto> = emptyList(),
    val meta: HeanCmsSearchMetaDto? = null
)

@Serializable
data class HeanCmsSearchMetaDto(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("last_page") val lastPage: Int
) {

    val hasNextPage: Boolean
        get() = currentPage < lastPage
}

@Serializable
data class HeanCmsSeriesDto(
    val id: Int,
    @SerialName("series_slug") val slug: String,
    @SerialName("series_type") val type: String = "Comic",
    val author: String? = null,
    val description: String? = null,
    val studio: String? = null,
    val status: String? = null,
    val thumbnail: String,
    val title: String,
    val tags: List<HeanCmsTagDto>? = emptyList(),
    val chapters: List<HeanCmsChapterDto>? = emptyList()
) {

    fun toSManga(apiUrl: String, coverPath: String): SManga = SManga.create().apply {
        val descriptionBody = this@HeanCmsSeriesDto.description?.let(Jsoup::parseBodyFragment)

        title = this@HeanCmsSeriesDto.title
        author = this@HeanCmsSeriesDto.author?.trim()
        artist = this@HeanCmsSeriesDto.studio?.trim()
        description = descriptionBody?.select("p")
            ?.joinToString("\n\n") { it.text() }
            ?.ifEmpty { descriptionBody.text().replace("\n", "\n\n") }
        genre = tags.orEmpty()
            .sortedBy(HeanCmsTagDto::name)
            .joinToString { it.name }
        thumbnail_url = "$apiUrl/$coverPath$thumbnail"
        status = when (this@HeanCmsSeriesDto.status) {
            "Ongoing" -> SManga.ONGOING
            "Hiatus" -> SManga.ON_HIATUS
            "Dropped" -> SManga.CANCELLED
            "Completed", "Finished" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        url = "/series/${slug.replace(HeanCms.TIMESTAMP_REGEX, "")}"
    }
}

@Serializable
data class HeanCmsTagDto(val name: String)

@Serializable
data class HeanCmsChapterDto(
    val id: Int,
    @SerialName("chapter_name") val name: String,
    @SerialName("chapter_slug") val slug: String,
    val index: String,
    @SerialName("created_at") val createdAt: String,
) {

    fun toSChapter(seriesSlug: String): SChapter = SChapter.create().apply {
        name = this@HeanCmsChapterDto.name.trim()
        date_upload = runCatching { DATE_FORMAT.parse(createdAt.substringBefore("."))?.time }
            .getOrNull() ?: 0L
        url = "/series/$seriesSlug/$slug#$id"
    }

    companion object {
        private val DATE_FORMAT by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        }
    }
}

@Serializable
data class HeanCmsReaderDto(
    val content: HeanCmsReaderContentDto? = null
)

@Serializable
data class HeanCmsReaderContentDto(
    val images: List<String>? = emptyList()
)

@Serializable
data class HeanCmsSearchPayloadDto(
    val order: String,
    val page: Int,
    @SerialName("order_by") val orderBy: String,
    @SerialName("series_status") val status: String,
    @SerialName("series_type") val type: String,
    @SerialName("tags_ids") val tagIds: List<Int> = emptyList()
)
