from unittest.mock import AsyncMock, patch

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.services.chat_service import detect_intent, _extract_referenced_ids

client = TestClient(app)


# ── 의도 파악 단위 테스트 ──


class TestDetectIntent:
    def test_korean_food(self):
        intent, cat = detect_intent("점심에 한식 먹고 싶어")
        assert intent == "store"
        assert cat == "한식"

    def test_cafe(self):
        intent, cat = detect_intent("카페 추천해줘")
        assert intent == "store"
        assert cat == "카페"

    def test_western_food(self):
        intent, cat = detect_intent("파스타 먹을 곳 있어?")
        assert intent == "store"
        assert cat == "양식"

    def test_festival(self):
        intent, cat = detect_intent("구미에서 하는 축제 알려줘")
        assert intent == "festival"
        assert cat is None

    def test_general_store(self):
        intent, cat = detect_intent("맛집 추천 좀")
        assert intent == "store"
        assert cat is None

    def test_general_conversation(self):
        intent, cat = detect_intent("안녕하세요")
        assert intent == "general"
        assert cat is None


# ── 참조 ID 추출 테스트 ──


class TestExtractReferencedIds:
    def test_extract_from_json_block(self):
        reply = (
            "여기 맛집 추천이에요!\n\n"
            "```json\n"
            '{"referenced_stores": [1, 5], "referenced_festivals": [3]}\n'
            "```"
        )
        store_ids, fest_ids, clean = _extract_referenced_ids(reply)
        assert store_ids == [1, 5]
        assert fest_ids == [3]
        assert "```json" not in clean

    def test_no_json_block(self):
        reply = "그냥 일반 대화 응답이에요."
        store_ids, fest_ids, clean = _extract_referenced_ids(reply)
        assert store_ids == []
        assert fest_ids == []
        assert clean == reply

    def test_malformed_json(self):
        reply = "추천!\n\n```json\n{broken json}\n```"
        store_ids, fest_ids, clean = _extract_referenced_ids(reply)
        assert store_ids == []
        assert fest_ids == []


# ── API 엔드포인트 통합 테스트 ──


class TestChatEndpoint:
    @patch("app.api.chat.chat", new_callable=AsyncMock)
    def test_post_chat_success(self, mock_chat):
        from app.schemas.chat import ChatResponse

        mock_chat.return_value = ChatResponse(
            reply="구미 한식 맛집 추천드려요!",
            referenced_stores=[1, 2],
            referenced_festivals=[],
        )

        response = client.post(
            "/api/chat",
            json={
                "user_no": 1,
                "message": "한식 맛집 추천해줘",
                "history": [],
            },
        )

        assert response.status_code == 200
        data = response.json()
        assert "추천" in data["reply"]
        assert data["referenced_stores"] == [1, 2]
        assert data["referenced_festivals"] == []

    def test_post_chat_empty_message(self):
        response = client.post(
            "/api/chat",
            json={
                "user_no": 1,
                "message": "",
                "history": [],
            },
        )
        assert response.status_code == 400

    def test_post_chat_missing_fields(self):
        response = client.post("/api/chat", json={})
        assert response.status_code == 400
