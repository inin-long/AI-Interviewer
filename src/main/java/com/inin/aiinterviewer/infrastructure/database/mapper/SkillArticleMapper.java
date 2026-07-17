package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.SkillArticleEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface SkillArticleMapper {

    @Insert("""
            INSERT INTO skill_article(user_id, category, title, summary, content_markdown, tags_json,
                                     create_time, deleted)
            VALUES(#{userId}, #{category}, #{title}, #{summary}, #{contentMarkdown}, #{tagsJson},
                    CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SkillArticleEntity entity);

    @Select("""
            SELECT id, user_id, category, title, summary, content_markdown, tags_json,
                   create_time, deleted
            FROM skill_article
            WHERE id = #{id} AND deleted = 0
            LIMIT 1
            """)
    Optional<SkillArticleEntity> findById(long id);

    @Select("""
            SELECT id, user_id, category, title, summary, content_markdown, tags_json,
                   create_time, deleted
            FROM skill_article
            WHERE (user_id IS NULL OR user_id = #{userId}) AND deleted = 0
            ORDER BY user_id IS NULL, create_time DESC, id DESC
            """)
    List<SkillArticleEntity> findAllVisible(long userId);

    @Update("""
            UPDATE skill_article
            SET category = #{category}, title = #{title}, summary = #{summary},
                content_markdown = #{contentMarkdown}, tags_json = #{tagsJson}
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int updateOwner(long id, long userId, String category, String title,
                    String summary, String contentMarkdown, String tagsJson);

    @Update("""
            UPDATE skill_article
            SET deleted = 1
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int logicalDelete(long id, long userId);
}
