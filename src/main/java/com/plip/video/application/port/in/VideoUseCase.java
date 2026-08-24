package com.plip.video.application.port.in;

import com.plip.video.application.port.in.dto.VideoCompleteCommand;
import com.plip.video.application.port.in.dto.VideoCompleteResult;
import com.plip.video.application.port.in.dto.VideoDestinationCommand;
import com.plip.video.application.port.in.dto.VideoDestinationResult;
import com.plip.video.application.port.in.dto.VideoDetailResult;
import com.plip.video.application.port.in.dto.VideoDownloadUrlResult;
import com.plip.video.application.port.in.dto.VideoUploadUrlCommand;
import com.plip.video.application.port.in.dto.VideoOwnershipResult;
import com.plip.video.application.port.in.dto.VideoUploadUrlResult;

import java.util.UUID;

public interface VideoUseCase {

	VideoUploadUrlResult issueUploadUrl(VideoUploadUrlCommand command);

	VideoCompleteResult complete(VideoCompleteCommand command);

	VideoDestinationResult publishDestination(VideoDestinationCommand command);

	VideoDetailResult getVideo(UUID videoUuid, UUID actorUuid);

	VideoDownloadUrlResult getDownloadUrl(UUID videoUuid, UUID actorUuid);

	VideoOwnershipResult getOwnership(UUID videoUuid);

	void updateThumbnail(UUID videoUuid, String thumbnailS3Key);

	void updateProcessed(UUID videoUuid, String processedS3Key, Integer durationSeconds);
}
