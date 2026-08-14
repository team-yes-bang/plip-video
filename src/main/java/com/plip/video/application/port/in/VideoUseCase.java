package com.plip.video.application.port.in;

import com.plip.video.application.port.in.dto.VideoRegisterCommand;
import com.plip.video.application.port.in.dto.VideoRegisterResult;

import java.util.UUID;

public interface VideoUseCase {

	VideoRegisterResult register(VideoRegisterCommand command);

	void requestDownloadProcessing(UUID videoUuid);
}
