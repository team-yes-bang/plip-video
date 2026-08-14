package com.plip.video.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서비스 상태 확인")
@RestController
@RequestMapping("/api")
public class HealthController {

	@Operation(summary = "헬스체크")
	@GetMapping("/test")
	public String test() {
		return "video service ok";
	}
}
