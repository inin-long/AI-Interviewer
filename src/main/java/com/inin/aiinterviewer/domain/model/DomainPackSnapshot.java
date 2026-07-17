package com.inin.aiinterviewer.domain.model;

public record DomainPackSnapshot(
        String id,
        String version,
        DomainPack content
) {
    public static DomainPackSnapshot from(DomainPack pack) {
        return new DomainPackSnapshot(pack.id(), pack.version(), pack);
    }
}
