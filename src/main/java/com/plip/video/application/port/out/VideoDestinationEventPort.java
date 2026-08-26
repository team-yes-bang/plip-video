package com.plip.video.application.port.out;

import com.plip.video.application.port.out.dto.DiaryVideoUploadedMessage;
import com.plip.video.application.port.out.dto.TopicVideoUploadedMessage;

public interface VideoDestinationEventPort {

	void publishTopicVideoUploaded(TopicVideoUploadedMessage message);

	void publishDiaryVideoUploaded(DiaryVideoUploadedMessage message);
}
