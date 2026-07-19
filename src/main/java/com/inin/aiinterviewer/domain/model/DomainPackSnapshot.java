package com.inin.aiinterviewer.domain.model;

public record DomainPackSnapshot(
        String id,
        String version,
        DomainPack content
) {
    /** 哨兵值：表示「不使用任何岗位知识包」这一模式。 */
    public static final String NONE_PACK_ID = "none";

    /** 生成一个「无知识包」快照（id 为哨兵值，content 为 null）。 */
    public static DomainPackSnapshot none() {
        return new DomainPackSnapshot(NONE_PACK_ID, "", null);
    }

    public static DomainPackSnapshot from(DomainPack pack) {
        return new DomainPackSnapshot(pack.id(), pack.version(), pack);
    }
}
