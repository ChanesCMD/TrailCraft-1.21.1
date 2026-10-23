package io.github.ChanesCMD.trailcraft.voice;

import io.github.ChanesCMD.trailcraft.groups.Groups;

import javax.annotation.Nullable;

/** Каналы голосового чата (ТЗ, раздел 7) в порядке окна. local — обычный голос рядом, без группы. */
public enum VoiceChannel {
    PARTY(Groups.Kind.PARTY), CITY(Groups.Kind.CITY), COUNTRY(Groups.Kind.COUNTRY), LOCAL(null);

    /** Префикс имени группы Simple Voice Chat: по нему клиент понимает, в каком он канале. */
    public static final String GROUP_PREFIX = "trailcraft:";
    public static final String CALL_GROUP = GROUP_PREFIX + "call";

    @Nullable
    public final Groups.Kind kind;

    VoiceChannel(@Nullable Groups.Kind kind) {
        this.kind = kind;
    }

    public String id() {
        return name().toLowerCase();
    }

    /** Нужна ли для канала рация (если это включено на сервере). */
    public boolean remote() {
        return this != LOCAL;
    }

    /** Имя группы Simple Voice Chat для группы groupId этого канала. */
    public String groupName(String groupId) {
        return GROUP_PREFIX + id() + ":" + groupId;
    }

    /** Канал по имени группы Simple Voice Chat; null — это не наш канал (звонок или чужая группа). */
    @Nullable
    public static VoiceChannel byGroupName(@Nullable String name) {
        if (name == null) {
            return LOCAL;
        }
        for (VoiceChannel channel : values()) {
            if (channel != LOCAL && name.startsWith(GROUP_PREFIX + channel.id() + ":")) {
                return channel;
            }
        }
        return null;
    }
}
