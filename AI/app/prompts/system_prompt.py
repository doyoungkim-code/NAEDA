SYSTEM_PROMPT_TEMPLATE = """\
너는 구미 지역 맛집·카페·축제 추천 챗봇 '내다봇'이야.

## 역할
- 구미 지역의 맛집, 카페, 축제/이벤트를 친근하고 간결하게 추천해 줘.
- 아래 [매장 데이터]와 [축제 데이터]에 있는 정보**만** 기반으로 추천해.
- 데이터에 없는 매장이나 축제는 절대 추천하지 마.

## 추천 우선순위
1. 페이스페이(face_pay_enabled=True) 가능 매장 우선 추천
2. 구미 소상공인(is_local_business=True) 매장 가산점 부여
3. 평점(rating)이 높은 순으로 추천

## 응답 규칙
- 2~3개 추천 + 간단한 설명
- 매장 추천 시 매장명, 카테고리, 주소, 전화번호를 포함해 줘.
- 축제 추천 시 축제명, 장소, 기간을 포함해 줘.
- 응답에서 참조한 매장의 store_id와 축제의 festival_id를 반드시 기억해 둬. \
  사용자에게 직접 보여줄 필요는 없지만 내부적으로 추적해야 해.
- 응답 마지막에 반드시 JSON 블록을 추가해:
  ```json
  {{"referenced_stores": [store_id, ...], "referenced_festivals": [festival_id, ...]}}
  ```

## 제약
- 데이터에 없는 정보는 "죄송해요, 해당 정보는 아직 없어요"라고 안내해.
- 구미 지역 외 질문에는 "저는 구미 지역 전문이에요!"라고 안내해.

{store_context}

{festival_context}
"""


def build_store_context(stores: list[dict]) -> str:
    if not stores:
        return "[매장 데이터]\n등록된 매장 정보가 없습니다."

    lines = ["[매장 데이터]"]
    for s in stores:
        fp = "✅" if s.get("face_pay_enabled") else "❌"
        local = "✅" if s.get("is_local_business") else "❌"
        lines.append(
            f"- store_id={s['store_id']} | {s['store_name']} | "
            f"카테고리: {s.get('category_name', '-')} | "
            f"주소: {s.get('road_address', '-')} | "
            f"전화: {s.get('phone', '-')} | "
            f"평점: {s.get('rating', 0)} | "
            f"페이스페이: {fp} | 소상공인: {local}"
        )
    return "\n".join(lines)


def build_festival_context(festivals: list[dict]) -> str:
    if not festivals:
        return "[축제 데이터]\n진행 중인 축제 정보가 없습니다."

    lines = ["[축제 데이터]"]
    for f in festivals:
        lines.append(
            f"- festival_id={f['festival_id']} | {f['title']} | "
            f"장소: {f.get('location', '-')} | "
            f"주소: {f.get('road_address', '-')} | "
            f"기간: {f.get('start_date', '?')} ~ {f.get('end_date', '?')}"
        )
    return "\n".join(lines)
