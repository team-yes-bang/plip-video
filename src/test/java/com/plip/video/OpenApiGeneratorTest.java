package com.plip.video;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiGeneratorTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void generateOpenApiYaml() throws Exception {
		byte[] openApiYaml = mockMvc.perform(get("/v3/api-docs.yaml"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsByteArray();

		Path docsDir = Path.of("docs");
		Files.createDirectories(docsDir);
		Files.writeString(docsDir.resolve("openapi.yaml"), new String(openApiYaml, StandardCharsets.UTF_8));
	}
}
