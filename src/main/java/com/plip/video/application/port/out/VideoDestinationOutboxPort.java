package com.plip.video.application.port.out;

import com.plip.video.application.port.out.dto.DiaryVideoUploadedMessage;
import com.plip.video.application.port.out.dto.TopicVideoUploadedMessage;

public interface VideoDestinationOutboxPort {

	void enqueueTopicVideoUploaded(TopicVideoUploadedMessage message);

	void enqueueDiaryVideoUploaded(DiaryVideoUploadedMessage message);

}
