FROM postgres:16

# SQL 파일을 이미지 안으로 직접 복사 (Windows volume 권한 문제 우회)
# build context: infra/ 기준
COPY init/01_schema.sql /docker-entrypoint-initdb.d/01_schema.sql
COPY init/02_dummy_user.sql /docker-entrypoint-initdb.d/02_dummy_user.sql
COPY init/03_face_embeddings_index.sql /docker-entrypoint-initdb.d/03_face_embeddings_index.sql
