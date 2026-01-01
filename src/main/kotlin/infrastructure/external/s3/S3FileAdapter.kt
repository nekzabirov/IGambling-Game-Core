package infrastructure.external.s3

import application.port.outbound.FileAdapter
import application.port.outbound.MediaFile
import java.util.UUID

/**
 * S3 file storage adapter implementation.
 *
 * Configure with your S3 credentials and bucket settings.
 */
class S3FileAdapter(
    private val bucketName: String,
    private val baseUrl: String
) : FileAdapter {

    override suspend fun upload(folder: String, fileName: String, file: MediaFile): Result<String> {
        return try {
            val key = "$folder/${fileName}_${UUID.randomUUID()}.${file.ext}"

            // TODO: Implement actual S3 upload using AWS SDK
            // Example:
            // s3Client.putObject(PutObjectRequest.builder()
            //     .bucket(bucketName)
            //     .key(key)
            //     .contentType(getContentType(file.ext))
            //     .build(),
            //     RequestBody.fromBytes(file.bytes))

            val path = "$baseUrl/$key"
            Result.success(path)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(path: String): Result<Boolean> {
        return try {
            val key = path.removePrefix("$baseUrl/")

            // TODO: Implement actual S3 delete using AWS SDK
            // Example:
            // s3Client.deleteObject(DeleteObjectRequest.builder()
            //     .bucket(bucketName)
            //     .key(key)
            //     .build())

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
