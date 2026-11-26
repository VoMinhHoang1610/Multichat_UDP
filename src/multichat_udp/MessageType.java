package multichat_udp;

/**
 * Ma loai thong diep trong giao thuc UDP Multichat.
 */
public enum MessageType {
    JOIN((byte) 1),
    LEAVE((byte) 2),
    CHAT((byte) 3),
    HEARTBEAT((byte) 4),
    WHISPER((byte) 5),
    FILE((byte) 6),
    FOLDER((byte) 7),
    LINK((byte) 8),
    STICKER((byte) 9),
    PROFILE_UPDATE((byte) 10),
    SYSTEM((byte) 11),
    RECALL((byte) 12),
    REACTION((byte) 13);

    private final byte code;

    MessageType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static MessageType fromCode(byte code) {
        for (MessageType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return CHAT;
    }
}
