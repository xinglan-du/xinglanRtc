package cn.duxinglan.sdp.parse.media.parser;

import cn.duxinglan.sdp.entity.Candidate;
import cn.duxinglan.sdp.entity.MediaDescription;
import cn.duxinglan.sdp.entity.type.CandidateAddressType;
import cn.duxinglan.sdp.entity.type.CandidateTransportType;
import cn.duxinglan.sdp.parse.media.MediaLineParser;

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
public class CandidateLineParser extends MediaLineParser {

    public static final String KEY = "candidate";

    @Override
    public String[] getLineStartWith() {
        return new String[]{KEY};
    }

    @Override
    protected boolean parse(MediaDescription mediaDescription, String key, String value) {
        String[] s = value.split(" ");
        Candidate candidate = new Candidate();
        candidate.setFoundation(s[0]);
        candidate.setComponentId(Integer.parseInt(s[1]));
        candidate.setCandidateTransportType(CandidateTransportType.formValue(s[2]));
        candidate.setPriority(Integer.parseInt(s[3]));
        candidate.setConnectionAddress(s[4]);
        candidate.setPort(Integer.parseInt(s[5]));
        for (int i = 6; i < s.length; i += 2) {
            if ("typ".equals(s[i])) {
                candidate.setCandidateAddressType(CandidateAddressType.formValue(s[i + 1]));
            } else if ("generation".equals(s[i])) {
                candidate.setGeneration(Integer.parseInt(s[i + 1]));
            } else if ("network-id".equals(s[i])) {
                candidate.setNetworkId(Integer.parseInt(s[i + 1]));
            } else if ("network-cost".equals(s[i])) {
                candidate.setNetworkCost(Integer.parseInt(s[i + 1]));
            } else if ("tcptype".equals(s[i])) {
                candidate.setTcpType(s[i + 1]);
            }else if ("raddr".equals(s[i])) {
                candidate.setRelatedAddress(s[i + 1]);
            }else if ("rport".equals(s[i])) {
                candidate.setRelatedPort(Integer.parseInt(s[i + 1]));
            }

        }
        mediaDescription.addCandidate(candidate);
        return true;
    }

}
