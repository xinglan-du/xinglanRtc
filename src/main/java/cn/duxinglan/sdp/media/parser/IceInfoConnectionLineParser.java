package cn.duxinglan.sdp.media.parser;

import cn.duxinglan.media.impl.sdp.IceInfo;
import cn.duxinglan.media.signaling.sdp.MediaDescription;
import cn.duxinglan.sdp.media.MediaLineParser;

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
public class IceInfoConnectionLineParser extends MediaLineParser {

    public static final String UFRAG_KEY = "ice-ufrag";
    public static final String PWS = "ice-pwd";
    public static final String OPTIONS = "ice-options";


    @Override
    public String[] getLineStartWith() {
        return new String[]{UFRAG_KEY, PWS, OPTIONS};
    }

    @Override
    protected boolean parse(MediaDescription mediaDescription, String key, String value) {
        IceInfo iceInfo = mediaDescription.getIceInfo();
        if (iceInfo == null) {
            iceInfo = new IceInfo();
            mediaDescription.setIceInfo(iceInfo);
        }
        switch (key) {
            case UFRAG_KEY -> iceInfo.setUfrag(value);
            case PWS -> iceInfo.setPwd(value);
            case OPTIONS -> iceInfo.setOptions(value);
        }
        return true;
    }

}
