package com.plip.video.adapter.out.storage;

import org.springframework.core.io.InputStreamResource;

public record LocalObjectReadResult(String objectKey, InputStreamResource resource) {
}
