package net.gamedoctor.hakaton.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ChatAdmin {
    private final long userID;
    private final String prefix;
    private final boolean isOwner;
}
