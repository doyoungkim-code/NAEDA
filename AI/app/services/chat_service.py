import json
import logging
import re

from openai import AsyncOpenAI
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings
from app.prompts.system_prompt import (
    SYSTEM_PROMPT_TEMPLATE,
    build_festival_context,
    build_store_context,
)
from app.repositories.festival_repo import search_festivals
from app.repositories.store_repo import search_stores
from app.schemas.chat import ChatRequest, ChatResponse

logger = logging.getLogger(__name__)
settings = get_settings()

# 의도 파악용 키워드 매핑
STORE_KEYWORDS: dict[str | None, list[str]] = {
    "한식": ["한식", "밥", "국밥", "찌개", "비빔밥", "김치", "백반", "한정식"],
    "양식": ["양식", "파스타", "피자", "스테이크", "햄버거", "버거"],
    "중식": ["중식", "중국", "짜장", "짬뽕", "탕수육"],
    "일식": ["일식", "일본", "초밥", "스시", "라멘", "돈까스", "돈카츠", "우동"],
    "카페": ["카페", "커피", "디저트", "빵", "베이커리", "케이크"],
    None: ["맛집", "식당", "밥집", "먹", "추천", "점심", "저녁", "아침", "야식", "배달"],
}

FESTIVAL_KEYWORDS: list[str] = ["축제", "이벤트", "행사", "페스티벌", "공연", "전시"]

# 구미 지역명 키워드 (동 단위)
LOCATION_KEYWORDS: list[str] = [
    "진평동", "인동", "구평동", "봉곡동", "도량동", "원평동", "형곡동",
    "송정동", "지산동", "신평동", "비산동", "수점동", "광평동", "상모동",
    "임수동", "남통동", "옥계동", "선산", "해평", "고아",
    "진평", "구평", "봉곡", "도량", "원평", "형곡", "송정", "지산",
    "신평", "비산", "수점", "광평", "상모", "임수", "남통", "옥계",
]


def detect_intent(message: str) -> tuple[str, str | None, str | None]:
    """메시지에서 의도(store/festival/general), 카테고리, 지역 키워드를 파악한다."""
    msg = message.lower()

    # 지역 키워드 추출
    location: str | None = None
    for loc in LOCATION_KEYWORDS:
        if loc in msg:
            location = loc
            break

    # 축제 키워드 먼저 확인
    for kw in FESTIVAL_KEYWORDS:
        if kw in msg:
            return "festival", None, location

    # 매장 카테고리별 키워드 확인
    for category, keywords in STORE_KEYWORDS.items():
        for kw in keywords:
            if kw in msg:
                return "store", category, location

    # 지역명만 있으면 매장 검색으로 처리
    if location:
        return "store", None, location

    return "general", None, None


def _extract_referenced_ids(reply: str) -> tuple[list[int], list[int], str]:
    """LLM 응답에서 JSON 블록의 referenced_stores/festivals를 추출한다."""
    store_ids: list[int] = []
    festival_ids: list[int] = []

    pattern = r"```json\s*(\{.*?\})\s*```"
    match = re.search(pattern, reply, re.DOTALL)
    if match:
        try:
            data = json.loads(match.group(1))
            store_ids = data.get("referenced_stores", [])
            festival_ids = data.get("referenced_festivals", [])
        except (json.JSONDecodeError, TypeError):
            pass
        # JSON 블록을 응답에서 제거
        clean_reply = reply[: match.start()].rstrip()
    else:
        clean_reply = reply

    return store_ids, festival_ids, clean_reply


async def chat(request: ChatRequest, db: AsyncSession) -> ChatResponse:
    """챗봇 메시지를 처리하고 응답을 생성한다."""
    intent, category, location = detect_intent(request.message)

    # DB 조회
    stores: list[dict] = []
    festivals: list[dict] = []

    if intent == "store" or intent == "general":
        stores = await search_stores(db, category_keyword=category, location_keyword=location)
    if intent == "festival" or intent == "general":
        festivals = await search_festivals(db)

    logger.info("[ChatService] intent=%s, category=%s, location=%s, stores=%d, festivals=%d",
                intent, category, location, len(stores), len(festivals))

    # 시스템 프롬프트 구성
    system_prompt = SYSTEM_PROMPT_TEMPLATE.format(
        store_context=build_store_context(stores),
        festival_context=build_festival_context(festivals),
    )

    # 대화 이력 구성 (최근 N턴만)
    messages: list[dict[str, str]] = [{"role": "system", "content": system_prompt}]
    history = request.history[-settings.chat_max_history_turns * 2 :]
    for h in history:
        messages.append({"role": h.role, "content": h.content})
    messages.append({"role": "user", "content": request.message})

    # OpenAI API 호출
    try:
        client = AsyncOpenAI(
            api_key=settings.openai_api_key,
            base_url="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
        )
        completion = await client.chat.completions.create(
            model=settings.openai_model,
            messages=messages,
            temperature=0.7,
            max_tokens=1024,
        )
        raw_reply = completion.choices[0].message.content or ""
    except Exception as e:
        logger.error("[ChatService] OpenAI API 호출 실패: %s", e)
        return ChatResponse(
            reply="죄송합니다, 잠시 후 다시 시도해주세요.",
            referenced_stores=[],
            referenced_festivals=[],
        )

    # 참조 ID 추출 및 응답 정리
    store_ids, festival_ids, clean_reply = _extract_referenced_ids(raw_reply)

    return ChatResponse(
        reply=clean_reply,
        referenced_stores=store_ids,
        referenced_festivals=festival_ids,
    )
