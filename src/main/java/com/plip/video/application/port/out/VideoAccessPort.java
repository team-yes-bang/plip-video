package com.plip.video.application.port.out;

import java.util.UUID;

public interface VideoAccessPort {

	boolean canView(UUID actorUuid, UUID videoUuid);
}
