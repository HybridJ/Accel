package com.accel.api.storage;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

@Service
public class GcsStorageService {

	private static final long MAX_PROFILE_IMAGE_SIZE = 10L * 1024L * 1024L;

	@Value("${gcs.bucket-name:}")
	private String bucketName;

	@Value("${gcs.profile-image-prefix:profile-images}")
	private String profileImagePrefix;

	@Value("${gcs.article-image-prefix:article-images}")
	private String articleImagePrefix;

	public String uploadProfileImage(MultipartFile file) throws IOException {
		return uploadImage(file, profileImagePrefix, "Profile image");
	}

	public String uploadArticleImage(MultipartFile file) throws IOException {
		return uploadImage(file, articleImagePrefix, "Article image");
	}

	private String uploadImage(MultipartFile file, String imagePrefix, String label) throws IOException {
		if (file == null || file.isEmpty()) {
			return null;
		}

		if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
			throw new IllegalArgumentException(label + " must be 10MB or smaller.");
		}

		String contentType = file.getContentType();

		if (contentType == null || !contentType.startsWith("image/")) {
			throw new IllegalArgumentException(label + " must be an image file.");
		}

		if (!StringUtils.hasText(bucketName)) {
			throw new IllegalStateException("GCS bucket name is not configured.");
		}

		String objectName = buildObjectName(file.getOriginalFilename(), contentType, imagePrefix);
		BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
				.setContentType(contentType)
				.build();
		Storage storage = StorageOptions.getDefaultInstance().getService();

		storage.create(blobInfo, file.getBytes());
		return objectName;
	}

	public StoredImage downloadProfileImage(String objectName) {
		if (!StringUtils.hasText(objectName) || "profile_default.png".equals(objectName)) {
			return null;
		}

		Storage storage = StorageOptions.getDefaultInstance().getService();
		Blob blob = storage.get(bucketName, objectName);

		if (blob == null || !blob.exists()) {
			return null;
		}

		return new StoredImage(blob.getContent(), blob.getContentType());
	}

	public StoredImage downloadImage(String objectName) {
		if (!StringUtils.hasText(objectName)) {
			return null;
		}

		Storage storage = StorageOptions.getDefaultInstance().getService();
		Blob blob = storage.get(bucketName, objectName);

		if (blob == null || !blob.exists()) {
			return null;
		}

		return new StoredImage(blob.getContent(), blob.getContentType());
	}

	public void deleteProfileImage(String objectName) {
		if (!StringUtils.hasText(objectName) || "profile_default.png".equals(objectName)) {
			return;
		}

		Storage storage = StorageOptions.getDefaultInstance().getService();
		storage.delete(bucketName, objectName);
	}

	public void deleteImage(String objectName) {
		if (!StringUtils.hasText(objectName)) {
			return;
		}

		Storage storage = StorageOptions.getDefaultInstance().getService();
		storage.delete(bucketName, objectName);
	}

	private String buildObjectName(String originalFilename, String contentType, String imagePrefix) {
		String extension = getExtension(originalFilename, contentType);
		String prefix = StringUtils.trimTrailingCharacter(imagePrefix, '/');
		return prefix + "/" + UUID.randomUUID() + extension;
	}

	private String getExtension(String originalFilename, String contentType) {
		String extension = StringUtils.getFilenameExtension(originalFilename);

		if (StringUtils.hasText(extension)) {
			return "." + extension.toLowerCase(Locale.ROOT);
		}

		return switch (contentType) {
			case "image/jpeg" -> ".jpg";
			case "image/png" -> ".png";
			case "image/gif" -> ".gif";
			case "image/webp" -> ".webp";
			default -> "";
		};
	}

	public record StoredImage(byte[] content, String contentType) {
	}
}
