package infrastructure.external.s3

import application.port.outbound.FileAdapter
import application.port.outbound.MediaFile
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import java.util.UUID

/**
 * S3-compatible file storage adapter implementation.
 * Works with AWS S3, MinIO, and other S3-compatible services.
 */
class S3FileAdapter(
    private val endpoint: String,
    private val accessKey: String,
    private val secretKey: String,
    private val bucketName: String,
    private val region: String
) : FileAdapter {

    private val s3Client: S3Client by lazy {
        S3Client {
            this.region = this@S3FileAdapter.region
            endpointUrl = Url.parse(endpoint)
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = accessKey
                secretAccessKey = secretKey
            }
            forcePathStyle = true
        }
    }

    override suspend fun upload(folder: String, fileName: String, file: MediaFile): Result<String> {
        return try {
            val key = "$folder/${fileName}_${UUID.randomUUID()}.${file.ext}"

            val request = PutObjectRequest {
                bucket = bucketName
                this.key = key
                contentType = getContentType(file.ext)
                body = ByteStream.fromBytes(file.bytes)
            }

            s3Client.putObject(request)

            Result.success(key)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(path: String): Result<Boolean> {
        return try {
            val request = DeleteObjectRequest {
                bucket = bucketName
                key = path
            }

            s3Client.deleteObject(request)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getContentType(ext: String): String {
        return when (ext.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "ico" -> "image/x-icon"
            "pdf" -> "application/pdf"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "txt" -> "text/plain"
            "html" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
}
