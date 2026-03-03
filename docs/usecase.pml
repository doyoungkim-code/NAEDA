@startuml
left to right direction
skinparam packageStyle rectangle
skinparam usecase {
  BackgroundColor #F9FBFF
  BorderColor #2F4F6F
  ArrowColor #2F4F6F
}
skinparam actor {
  BorderColor #333333
  BackgroundColor #FFFFFF
}

actor "사용자" as User
actor "가맹점 단말기" as Terminal
actor "관리자" as Admin
actor "SSAFY 금융 API" as FinanceApi
actor "AI 서버(FastAPI)" as AiServer

rectangle "NAEDA 서비스" {
  usecase "회원가입/로그인" as UC1
  usecase "계좌 조회" as UC2
  usecase "이체" as UC3
  usecase "거래 내역 조회" as UC4
  usecase "얼굴 등록" as UC5
  usecase "페이스페이 결제" as UC6
  usecase "Liveness 검증" as UC7
  usecase "얼굴 매칭" as UC8
  usecase "고액 결제 2차 인증" as UC9
  usecase "포인트 적립/사용" as UC10
  usecase "소비 리포트 조회" as UC11
  usecase "구미 지역 매장 추천" as UC12
  usecase "FDS 이상거래 탐지" as UC13
  usecase "가맹점/이벤트 관리" as UC14
}

User --> UC1
User --> UC2
User --> UC3
User --> UC4
User --> UC5
User --> UC11
User --> UC12

Terminal --> UC6
Terminal --> UC9

Admin --> UC14

UC6 .> UC7 : <<include>>
UC6 .> UC8 : <<include>>
UC6 .> UC10 : <<include>>
UC6 .> UC13 : <<include>>
UC9 .> UC6 : <<extend>>

UC2 --> FinanceApi
UC3 --> FinanceApi
UC4 --> FinanceApi
UC6 --> FinanceApi

UC5 --> AiServer
UC7 --> AiServer
UC8 --> AiServer
UC11 --> AiServer
UC12 --> AiServer
UC13 --> AiServer

note bottom of UC9
5만원 이상 결제 시
전자서명/PIN 등 2차 인증 수행
end note

note right of UC12
후원 기능 제외 버전
(현재 범위 기준)
end note

@enduml
