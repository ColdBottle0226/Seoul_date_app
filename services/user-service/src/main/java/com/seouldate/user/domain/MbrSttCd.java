package com.seouldate.user.domain;

/**
 * 회원상태코드 (mbr_stt_cd)
 *
 * <p>VARCHAR(5) 로 DB에 저장되며, 코드 확장 시 ENUM 재배포 없이 DB 코드 값 추가만으로 대응 가능.
 *
 * <ul>
 *   <li>ACT   : 정상 (Active)</li>
 *   <li>SUS   : 정지 (Suspended)</li>
 *   <li>DEL   : 탈퇴 (Deleted)</li>
 *   <li>DOR   : 휴면 (Dormant) — 장기미사용 전환</li>
 * </ul>
 */
public enum MbrSttCd {

    ACT("ACT", "정상"),
    SUS("SUS", "정지"),
    DEL("DEL", "탈퇴"),
    DOR("DOR", "휴면");

    private final String code;
    private final String label;

    MbrSttCd(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
