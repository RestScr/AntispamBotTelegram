package net.gamedoctor.hakaton.data;

public enum CheckType {
    LINK("Ссылки/Нарушения в сообщении/юзернейме"),
    LLM("LLM-сообщение"),
    FLOOD("Флуд"),
    PRE_MODERATION("Пре-Модерация"),
    SPAM("Спам-контент");

    private final String name;

    CheckType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
