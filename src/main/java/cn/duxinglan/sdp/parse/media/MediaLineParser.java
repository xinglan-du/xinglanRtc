package cn.duxinglan.sdp.parse.media;

import cn.duxinglan.sdp.entity.MediaDescription;

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
public abstract class MediaLineParser {

    /**
     * 获取该解析器支持处理的 SDP 行的起始标识符。
     *
     * @return 一个字符串，表示此解析器用来识别和处理的 SDP 行的前缀标识符。
     */
    abstract public String[] getLineStartWith();

    /**
     * 解析给定的SDP行信息，并将其对应的键值解析后交由具体的处理方法。
     *
     * @param sessionDescription 表示会话描述的对象，用于存储或更新解析后的SDP信息
     * @param line               SDP中的一行内容，包含键和值的完整字符串
     */
    public boolean onParse(MediaDescription mediaDescription, String line) {
        String key = line.substring(0, 2).trim();
        String value = line.substring(2).trim();
        return parse(mediaDescription, key, value);
    }

    public boolean onParse(MediaDescription mediaDescription, String key, String value) {
        return parse(mediaDescription, key, value);
    }

    /**
     * 解析指定的 SDP 属性键和值，并将它们应用到给定的会话描述对象中。
     *
     * @param sessionDescription 表示会话描述的对象，用于存储或更新解析后的 SDP 信息
     * @param key                表示 SDP 属性的键（例如 SDP 行的前缀标识符，如 "v=" 或 "o="）
     * @param value              表示键对应的值（例如 SDP 行中键的具体内容）
     */
    abstract protected boolean parse(MediaDescription mediaDescription, String key, String value);

}
