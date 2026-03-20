from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings

settings = get_settings()


async def search_stores(
    db: AsyncSession,
    category_keyword: str | None = None,
    limit: int | None = None,
) -> list[dict]:
    """카테고리 키워드로 매장을 검색한다. 평점순 정렬, 활성 매장만."""
    if limit is None:
        limit = settings.chat_max_store_results

    conditions = ["is_active = TRUE"]
    params: dict = {"limit": limit}

    if category_keyword:
        conditions.append(
            "(category_name ILIKE :kw OR store_name ILIKE :kw)"
        )
        params["kw"] = f"%{category_keyword}%"

    where = " AND ".join(conditions)
    query = text(
        f"SELECT store_id, store_name, category_name, road_address, "
        f"phone, is_local_business, face_pay_enabled, rating, description "
        f"FROM store WHERE {where} "
        f"ORDER BY rating DESC LIMIT :limit"
    )

    result = await db.execute(query, params)
    rows = result.mappings().all()
    return [dict(r) for r in rows]
