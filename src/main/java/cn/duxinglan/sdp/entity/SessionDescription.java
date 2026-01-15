package cn.duxinglan.sdp.entity;

import cn.duxinglan.sdp.entity.session.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
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
 **/
@Data
public class SessionDescription {

    /**
     * 表示会话描述中的版本号。
     * 用于标识会话描述的数据结构版本。
     */
    private Version version;

    /**
     * 表示会话描述中的源信息。
     * 包含会话的用户标识符(Session ID)、会话版本、网络类型、地址类型及单播地址等信息。
     */
    private Origin origin;

    /**
     * 表示会话描述中的会话名称。
     * 用于标识会话的显示名或名称信息。
     */
    private SessionName sessionName;

    /**
     * 表示会话描述中的时间范围。
     * 包含会话的起始时间和结束时间，用于定义会话的有效时间区间。
     */
    private Timing timing;

    /**
     * 表示会话描述中的BUNDLE组信息。
     * BUNDLE是一种RTP/RTCP多路复用的机制，用于在单个UDP端口上传输多个媒体流。
     * 该对象包含了会话中所有媒体流的唯一标识符列表。
     */
    private Bundle bundle;

    /**
     * 表示会话描述中是否允许使用混合的扩展映射的设置。
     * 该字段用于确定是否在会话描述协议(SDP)中启用特定的扩展映射混合功能。
     */
    private ExtMapAllowMixed extMapAllowMixed;

    /**
     * 表示会话描述中的MSID信息。
     * MSID是媒体流标识符(Media Stream Identifier)的缩写，用于标识媒体流的唯一性。
     * 它包含流的类型、流ID以及轨道ID等信息。
     */
    private MSid mSid;


    /**
     * 表示会话描述中的视频媒体描述列表。
     * 用于存储会话中所有的视频媒体描述信息，每个媒体描述包含视频流的各种属性，如编码器信息、媒体方向等。
     */
    private List<MediaDescription> mediaDescriptions = new ArrayList<>();


    /**
     * 添加一个媒体描述到视频媒体描述列表中。
     *
     * @param mediaDescription 要添加的媒体描述对象
     */
    public void addMediaDescription(MediaDescription mediaDescription) {
        if (mediaDescription == null) {
            return;
        }
        mediaDescriptions.add(mediaDescription);
    }

}
