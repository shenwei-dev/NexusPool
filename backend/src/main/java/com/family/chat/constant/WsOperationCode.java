package com.family.chat.constant;

/**
 * Upstream operation codes sent by WebSocket clients.
 *
 * <p>Message type codes agreed with the client. Magic numbers are strictly
 * forbidden in business code; always reference these constants instead.
 */
public final class WsOperationCode {

    /** Plain text message */
    public static final int TEXT_MESSAGE = 0;

    /** Invite a voice call */
    public static final int VOICE_CALL_INVITE = 1;

    /** Accept a voice call */
    public static final int VOICE_CALL_ACCEPT = 2;

    /** Hang up a voice call */
    public static final int VOICE_CALL_HANGUP = 3;

    /** Refuse a voice call invitation */
    public static final int VOICE_CALL_REFUSE = 4;

    /** Cancel an outgoing voice call */
    public static final int VOICE_CALL_CANCEL = 5;

    /** Invite a video call */
    public static final int VIDEO_CALL_INVITE = 6;

    /** Accept a video call */
    public static final int VIDEO_CALL_ACCEPT = 7;

    /** Hang up a video call */
    public static final int VIDEO_CALL_HANGUP = 8;

    /** Refuse a video call invitation */
    public static final int VIDEO_CALL_REFUSE = 9;

    /** Cancel an outgoing video call */
    public static final int VIDEO_CALL_CANCEL = 10;

    /** Location message */
    public static final int LOCATION_MESSAGE = 11;

    /** Image message */
    public static final int IMAGE_MESSAGE = 12;

    /** AI assistant message */
    public static final int AI_MESSAGE = 13;

    /** Client heartbeat */
    public static final int HEARTBEAT = 99;

    private WsOperationCode() {
    }
}
