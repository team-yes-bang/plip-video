package com.plip.video.adapter.out.topic;

import com.plip.video.application.port.out.VideoAccessPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class StubTopicVideoAccessAdapter implements VideoAccessPort {

	private final Set<String> grants = ConcurrentHashMap.newKeySet();

	public void grant(UUID actorUuid, UUID videoUuid) {
		if (actorUuid != null && videoUuid != null) {
			grants.add(key(actorUuid, videoUuid));
		}
	}

	public void reset() {
		grants.clear();
	}

	@Override
	public boolean canView(UUID actorUuid, UUID videoUuid) {
		return actorUuid != null && videoUuid != null && grants.contains(key(actorUuid, videoUuid));
	}

	private static String key(UUID actorUuid, UUID videoUuid) {
		return actorUuid + ":" + videoUuid;
	}
}
