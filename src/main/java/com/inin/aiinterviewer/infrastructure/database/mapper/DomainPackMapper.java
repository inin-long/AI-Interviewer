package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.DomainPackEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface DomainPackMapper {

    @Insert("""
            INSERT INTO domain_pack(id, role_code, industry_code, display_name, version,
                                    content_json, enabled, source, create_time, update_time)
            VALUES(#{id}, #{roleCode}, #{industryCode}, #{displayName}, #{version},
                   #{contentJson}, 1, #{source}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT(id) DO UPDATE SET
                role_code = excluded.role_code,
                industry_code = excluded.industry_code,
                display_name = excluded.display_name,
                version = excluded.version,
                content_json = excluded.content_json,
                enabled = 1,
                source = excluded.source,
                update_time = CURRENT_TIMESTAMP
            """)
    int upsert(DomainPackEntity entity);

    @Update("UPDATE domain_pack SET enabled = 0, update_time = CURRENT_TIMESTAMP WHERE source = 'BUILTIN'")
    int disableBuiltIns();

    @Delete("DELETE FROM domain_pack WHERE id = #{id} AND source = 'USER'")
    int deleteUserPack(String id);

    @Select("""
            SELECT id, role_code, industry_code, display_name, version, content_json,
                   enabled, source, create_time, update_time
            FROM domain_pack
            WHERE enabled = 1
            ORDER BY display_name, version DESC, id
            """)
    List<DomainPackEntity> findAllEnabled();

    @Select("""
            SELECT id, role_code, industry_code, display_name, version, content_json,
                   enabled, source, create_time, update_time
            FROM domain_pack
            WHERE id = #{id} AND enabled = 1
            LIMIT 1
            """)
    Optional<DomainPackEntity> findEnabledById(String id);
}
