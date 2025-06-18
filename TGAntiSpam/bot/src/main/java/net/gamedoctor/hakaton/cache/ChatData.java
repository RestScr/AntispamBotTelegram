package net.gamedoctor.hakaton.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ChatData {
    private final long chatID;
    private final String name;
    private final long regDate;
    private final int usersCount;
    private final List<ChatAdmin> admins;
}
