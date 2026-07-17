-- 修复 V19 种子数据中 difficulty 字段使用了非法枚举值 HARD/EASY 的问题
-- InterviewDifficulty 枚举只接受 JUNIOR/MEDIUM/SENIOR/EXPERT

UPDATE interview_question SET difficulty = 'SENIOR' WHERE difficulty = 'HARD';
UPDATE interview_question SET difficulty = 'JUNIOR' WHERE difficulty = 'EASY';
