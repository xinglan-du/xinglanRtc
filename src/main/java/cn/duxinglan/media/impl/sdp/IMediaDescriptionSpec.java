package cn.duxinglan.media.impl.sdp;

import cn.duxinglan.media.signaling.sdp.type.MediaInfoType;

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
public interface IMediaDescriptionSpec {

    /**
     * 获取当前媒体信息的类型。
     *
     * @return 表示媒体信息类型的MediaInfoType枚举值。
     */
    MediaInfoType getMediaInfoType();

    /**
     * 获取媒体的唯一标识符。
     *
     * @return 表示媒体唯一标识符的字符串。
     */
    String getMid();


    //这个媒体描述 是发送
    boolean isSendOnly();

    //这个媒体描述是接受
    boolean isReadOnly();
}
