from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings

settings = get_settings()


async def search_festivals(
    db: AsyncSession,
    keyword: str | None = None,
    limit: int | None = None,
) -> list[dict]:
    """진행 중인 축제를 검색한다. 종료일이 지난 축제는 제외."""
    if limit is None:
        limit = settings.chat_max_festival_results

    conditions = ["start_date <= CURRENT_DATE AND end_date >= CURRENT_DATE"]
    params: dict = {"limit": limit}

    if keyword:
        conditions.append(
            "(title ILIKE :kw OR description ILIKE :kw OR location ILIKE :kw)"
        )
        params["kw"] = f"%{keyword}%"

    where = " AND ".join(conditions)
    query = text(
        f"SELECT festival_id, title, description, location, road_address, "
        f"start_date, end_date, link_url, image_url "
        f"FROM festival WHERE {where} "
        f"ORDER BY start_date ASC LIMIT :limit"
    )

    result = await db.execute(query, params)
    rows = result.mappings().all()
    return [dict(r) for r in rows]
