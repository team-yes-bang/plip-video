package com.plip.video.application.port.in.dto;

public sealed interface VideoDownloadUrlResult permits VideoDownloadUrlReady, VideoDownloadUrlProcessing {
}
