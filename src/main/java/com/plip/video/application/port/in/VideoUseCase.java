package com.plip.video.application.port.in;

import com.plip.video.application.port.in.dto.VideoCompleteCommand;
import com.plip.video.application.port.in.dto.VideoCompleteResult;
import com.plip.video.application.port.in.dto.VideoRegisterCommand;
import com.plip.video.application.port.in.dto.VideoRegisterResult;
import com.plip.video.application.port.in.dto.VideoUploadUrlCommand;
import com.plip.video.application.port.in.dto.VideoUploadUrlResult;

import java.util.UUID;

public interface VideoUseCase {

	VideoUploadUrlResult issueUploadUrl(VideoUploadUrlCommand command);

	VideoCompleteResult complete(VideoCompleteCommand command);

	VideoRegisterResult register(VideoRegisterCommand command);

	void requestDownloadProcessing(UUID videoUuid);
}
