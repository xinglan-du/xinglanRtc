/*
 *
 * 版权所有 (c) 2025 www.duxinglan.cn
 * <p>
 * 项目名称：xinglanRtc
 * <p>
 * 本文件属于 xinglanRtc 项目的一部分。
 * <p>
 * 本软件依据 XinglanRtc 非商业许可证（XNCL）授权，仅限个人非商业使用。
 * 禁止任何形式的商业用途，包括但不限于：收费安装、收费部署、
 * 收费运维、收费技术支持等行为。
 * <p>
 * 详情请参阅项目根目录下的 LICENSE 文件。
 */
package cn.duxinglan.sdp.entity.type;

/**
 * 表示媒体信息的类型枚举类。
 * 包含两种类型："video" 表示视频，"audio" 表示音频。
 */
public enum MediaInfoType {

    /**
     * 视频媒体类型的枚举值。
     * 表示媒体信息中的视频类型，常用于区分音视频流的分类。
     * 该枚举常量定义为 "video"。
     */
    VIDEO("video"),
    /**
     * 音频媒体类型的枚举值。
     * 表示媒体信息中的音频类型，常用于区分音视频流的分类。
     * 该枚举常量定义为 "audio"。
     */
    AUDIO("audio");

    public final String value;


    MediaInfoType(String value) {
        this.value = value;
    }

    public static MediaInfoType fromValue(String string) {
        return switch (string) {
            case "video" -> VIDEO;
            case "audio" -> AUDIO;
            default -> null;
        };
    }
}
