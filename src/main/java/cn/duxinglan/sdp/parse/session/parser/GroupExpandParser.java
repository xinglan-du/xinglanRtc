package cn.duxinglan.sdp.parse.session.parser;

import cn.duxinglan.sdp.entity.SessionDescription;
import cn.duxinglan.sdp.entity.session.Bundle;
import cn.duxinglan.sdp.parse.session.SessionLineParser;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

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
@Slf4j
public class GroupExpandParser extends SessionLineParser {

    public static final String KEY = "group";

    private static final String BUNDLE_KEY = "BUNDLE";

    @Override
    public String getLineStartWith() {
        return KEY;
    }

    @Override
    protected boolean parse(SessionDescription sessionDescription, String key, String value) {
        String[] split = value.split(" ");
        if (!split[0].equals(BUNDLE_KEY)) {
            log.error("无法解析:{}", value);
            return false;
        }
        Bundle bundle = new Bundle();
        bundle.setMid(Arrays.stream(split).skip(1).toList());
        sessionDescription.setBundle(bundle);
        return true;
    }
}
