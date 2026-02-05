package ru.bre.storage.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MinioConfig {

    @Value("${minio.access.key}")
    private String accessKey;

    @Value("${minio.access.secret}")
    private String secretKey;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.bucket.screenshot}")
    private String screenshotBucket;

    @Value("${minio.bucket.log}")
    private String logBucket;

    @Bean
    @Primary
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .credentials(accessKey, secretKey)
                .endpoint(minioUrl)
                .build();

        try {
            createBucketIfNotExists(client, screenshotBucket);
            createBucketIfNotExists(client, logBucket);
        } catch (Exception e) {
            throw new RuntimeException("Error creating MinIO buckets", e);
        }

        return client;
    }

    private void createBucketIfNotExists(MinioClient client, String bucketName) throws Exception {
        boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );

        if (!exists) {
            client.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
        }

        setPublicReadPolicy(client, bucketName);
    }

    private void setPublicReadPolicy(MinioClient client, String bucketName) throws Exception {
        String policy = """
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Effect": "Allow",
              "Principal": "*",
              "Action": "s3:GetObject",
              "Resource": "arn:aws:s3:::%s/*"
            }
          ]
        }
        """.formatted(bucketName);

        client.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build()
        );
    }
}
