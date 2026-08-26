package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "영상 부착 대상 종류", enumAsRef = true)
public enum VideoDestinationKind {
	TOPIC,
	DIARY
}
