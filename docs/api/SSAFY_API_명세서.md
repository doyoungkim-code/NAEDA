# SSAFY 금융 API 명세서

> NAEDA 프로젝트에서 활용하는 SSAFY 금융 Open API 정리
> 총 35개 API | 7개 분류

---

## 목차

- **관리자용**
  - 1. 앱 관리자(개발자) API KEY 발급
  - 2. 앱 API KEY 재발급
- **사용자 계정**
  - 3. 사용자 계정 생성
  - 4. 사용자 계정 조회
- **공통**
  - 5. 공통 HEADER API
  - 6. 은행코드 조회
- **수시입출금**
  - 7. 상품 등록
  - 8. 상품 조회
  - 9. 계좌 생성
  - 10. 계좌 목록 조회
  - 11. 계좌 조회 (단건)
  - 12. 예금주 조회
  - 13. 계좌 잔액 조회
  - 14. 계좌 출금
  - 15. 계좌 입금
  - 16. 계좌 이체
  - 17. 계좌 이체 한도 변경
  - 18. 계좌 거래 내역 조회
  - 19. 계좌 거래 내역 조회 (단건)
- **카드**
  - 20. 카테고리 조회
  - 21. 가맹점 등록
  - 22. 카드사 조회
  - 23. 카드 상품 등록
  - 24. 카드 상품 조회
  - 25. 카드 생성
  - 26. 내 카드 목록 조회
  - 27. 가맹점 목록 조회
  - 28. 카드 결제
  - 29. 카드 결제 내역 조회
  - 30. 카드 결제 취소
  - 31. 청구서 조회
  - 32. 카드 결제 계좌 수정
- **1원 인증**
  - 33. 1원 송금
  - 34. 1원 송금 검증
- **거래내역 메모**
  - 35. 거래내역 메모

---

## 공통 사항

### 공통 Header 구조

수시입출금, 카드, 1원 인증, 거래내역 메모 API는 Request Body 안에 `Header` 객체를 포함합니다.

```json
{
    "Header": {
        "apiName": "API명",
        "transmissionDate": "YYYYMMDD",
        "transmissionTime": "HHmmss",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "API명",
        "institutionTransactionUniqueNo": "고유거래번호(20자리)",
        "apiKey": "발급받은 API KEY",
        "userKey": "사용자 KEY"
    }
}
```

### 공통 Response Header

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "API명",
        "transmissionDate": "YYYYMMDD",
        "transmissionTime": "HHmmss",
        "institutionCode": "00100",
        "apiKey": "API KEY",
        "apiServiceCode": "API명",
        "institutionTransactionUniqueNo": "고유거래번호"
    }
}
```

> 아래 각 API에서는 Header 부분을 생략하고 핵심 필드만 표기합니다.

---

## 관리자용

### 1. 앱 관리자(개발자) API KEY 발급

> OPEN API를 사용하기 전 API KEY 발급 받는 API

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/app/issuedApiKey`
- **Method**: `POST`

**Request Body**

```json
{
    "managerId": "ssafy@ssafy.co.kr"
}
```

**Request 상세**

- `managerId : 관리자 ID (varchar(30))`

**Response Body**

```json
{
    "managerId": "ssafy@ssafy.co.kr",
    "apiKey": "84711cec18cf4d52b2dbfbf03f20f17b",
    "creationDate": "20240415",
    "expirationDate": "20250415"
}
```

**Response 상세**

- `managerId : 관리자 ID (varchar(30))`
- `apiKey : API 키 (varchar(40))`
- `creationDate : 생성일 (varchar(8))`
- `expirationDate : 만료일 (varchar(8))`

**에러 코드**

- `E3000 : 이미 존재하는 관리자 ID입니다.`
- `E3002 : 관리자 ID가 유효하지 않습니다.`
- `A1080 : 등록되지 않은 관리자 이메일입니다.`
- `Q1000 : 그 이외에 에러 메시지`
- `Q1001 : 요청 본문의 형식이 잘못되었습니다. JSON 형식 또는 데이터 타입을 확인해 주세요.`

---

### 2. 앱 API KEY 재발급

> 현재 사용 중인 API KEY 확인하거나, 필요한 경우
새로운 API KEY 생성. 재발급 이후 기존 API KEY는
더 이상 사용되지 않음.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/app/reIssuedApiKey`
- **Method**: `POST`

**Request Body**

```json
{
    "managerId": "ssafy@ssafy.co.kr"
}
```

**Request 상세**

- `managerId : 관리자 ID (varchar(30))`

**Response Body**

```json
{
    "managerId": "ssafy@ssafy.co.kr",
    "apiKey": "8644e48ee75740469ef8b5214499e5f7"
}
```

**Response 상세**

- `managerId : 관리자 ID (varchar(30))`
- `apiKey : API 키 (varchar(40))`

**에러 코드**

- `E3001 : 존재하지 않는 관리자 ID입니다.`
- `E3002 : 관리자 ID가 유효하지 않습니다.`
- `E3003 : 현재 사용중인 API KEY가 만료되어 재발급이 불가합니다.`
- `Q1000 : 그 이외에 에러 메시지`
- `Q1001 : 요청 본문의 형식이 잘못되었습니다. JSON 형식 또는 데이터 타입을 확인해 주세요.`

---

## 사용자 계정

### 3. 사용자 계정 생성

> 앱을 이용하기 위한 사용자 회원가입 API

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/member`
- **Method**: `POST`

**Request Body**

```json
{
    "apiKey": "8644e48ee75740469ef8b5214499e5f7",
    "userId": "test@ssafy.co.kr"
}
```

**Response Body**

```json
{
    "userId": "test@ssafy.co.kr",
    "userName": "test",
    "institutionCode": "00100",
    "userKey": "cf1d49ba-663b-495d-9227-fc2643aa7c5e",
    "created": "2024-03-04T12:41:30.921299+09:00",
    "modified": "2024-03-04T12:41:30.921295+09:00"
}
```

---

### 4. 사용자 계정 조회

> 앱에 등록된 사용자의 정보 조회.
입력 파라미터와 정확히 일치하는 결과만 조회.
금융망에 등록된 사용자 계정 정보(email)은 고유하기에,
동일 email로 중복 계정 생성 불가능.
앱에서 사용자로부터 입력받은 email이 금융망에 존재하는 것으로
확인될 경우, 다른 email을 사용하도록 안내하시기 바람.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/member/search`
- **Method**: `POST`

**Request Body**

```json
{
    "apiKey": "8644e48ee75740469ef8b5214499e5f7",
    "userId": "test@ssafy.co.kr"
}
```

**Response Body**

```json
{
    "userId": "test@ssafy.co.kr",
    "userName": "test",
    "institutionCode": "00100",
    "userKey": "cf1d49ba-663b-495d-9227-fc2643aa7c5e",
    "created": "2024-03-04T12:41:30.921299+09:00",
    "modified": "2024-03-04T12:41:30.921295+09:00"
}
```

---

## 공통

### 5. 공통 HEADER API

> API 요청 / 응답 시, BODY에 공통으로 사용하는 데이터.
BODY 안에 Header 라는 키로 들어가며 공통부를 포함하여
API들의 요청, 응답값을 전송함.

**Request Body**

```json
{
    "Header": {
        "apiName": "drawingTransfer",
        "transmissionDate": "20240101",
        "transmissionTime": "121212",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "drawingTransfer",
        "institutionTransactionUniqueNo": "20240215121212123453",
        "apiKey": "8644e48ee75740469ef8b5214499e5f7",
        "userKey": "cf1d49ba-663b-495d-9227-fc2643aa7c5e"
    }
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inqureBankCodes",
        "transmissionDate": "20240207",
        "transmissionTime": "133415",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inqureBankCodes",
        "institutionTransactionUniqueNo": "20191129000000000001"
    }
}
```

---

### 6. 은행코드 조회

> 수시입출금, 예금, 적금, 대출 상품 등록 시 필요한
은행코드를 조회하는 API. 은행코드를 조회하여
각 은행의 상품을 만들수 있음.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/bank/inquireBankCodes`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "inquireBankCodes",
        "transmissionDate": "20240401",
        "transmissionTime": "135500",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireBankCodes",
        "institutionTransactionUniqueNo": "20240215121212123557",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29"
    }
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireBankCodes",
        "transmissionDate": "20240401",
        "transmissionTime": "135500",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "inquireBankCodes",
        "institutionTransactionUniqueNo": "20240215121212123557"
    },
    "REC": [
        {
            "bankCode": "001",
            "bankName": "한국은행"
        },
        {
            "bankCode": "002",
            "bankName": "산업은행"
        },
        {
            "bankCode": "003",
            "bankName": "기업은행"
        },
        {
            "bankCode": "004",
            "bankName": "국민은행"
        },
        {
            "bankCode": "011",
            "bankName": "농협은행"
        },
        {
            "bankCode": "020",
            "bankName": "우리은행"
        },
        {
            "bankCode": "023",
            "bankName": "SC제일은행"
        },
        {
            "bankCode": "027",
            "bankName": "시티은행"
        },
        {
            "bankCode": "032",
            "bankName": "대구은행"
        },
        {
            "bankCode": "034",
            "bankName": "광주은행"
        },
        {
            "bankCode": "035",
            "bankName": "제주은행"
        },
        {
            "bankCode": "037",
            "bankName": "전북은행"
        },
        {
            "bankCode": "039",
            "bankName": "경남은행"
        },
        {
            "bankCode": "045",
            "bankName": "새마을금고"
        },
        {
            "bankCode": "081",
            "bankName": "KEB하나은행"
        },
        {
            "bankCode": "088",
            "bankName": "신한은행"
        },
        {
            "bankCode": "090",
            "bankName": "카카오뱅크"
        },
        {
            "bankCode": "999",
            "bankName": "싸피은행"
        }
    ]
}
```

---

## 수시입출금

### 7. 상품 등록

> 은행별 수시입출금 상품을 등록함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/createDemandDeposit`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "createDemandDeposit",
        "transmissionDate": "20240401",
        "transmissionTime": "095500",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "createDemandDeposit",
        "institutionTransactionUniqueNo": "20240215121212123560",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29" 
    },
    "bankCode": "001",
    "accountName": "한국은행 수시입출금 상품명",
    "accountDescription": "한국은행 수시입출금 상품설명"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "createDemandDeposit",
        "transmissionDate": "20240401",
        "transmissionTime": "095500",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "createDemandDeposit",
        "institutionTransactionUniqueNo": "20240215121212123560"
    },
    "REC": {
        "accountTypeUniqueNo": "001-1-ffa4253081d540",
        "bankCode": "001",
        "bankName": "한국은행",
        "accountTypeCode": "1",
        "accountTypeName": "수시입출금",
        "accountName": "한국은행 수시입출금 상품명",
        "accountDescription": "한국은행 수시입출금 상품설명",
        "accountType": "DOMESTIC"
    }
}
```

---

### 8. 상품 조회

> 은행별 계좌 상품을 조회함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositList`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "inquireDemandDepositList",
        "transmissionDate": "20240401",
        "transmissionTime": "100100",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireDemandDepositList",
        "institutionTransactionUniqueNo": "20240215121212123561",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29"
    }
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireDemandDepositList",
        "transmissionDate": "20240401",
        "transmissionTime": "100100",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "inquireDemandDepositList",
        "institutionTransactionUniqueNo": "20240215121212123561"
    },
    "REC": [
        {
            "accountTypeUniqueNo": "001-1-ffa4253081d540",
            "bankCode": "001",
            "bankName": "한국은행",
            "accountTypeCode": "1",
            "accountTypeName": "수시입출금",
            "accountName": "한국은행 수시입출금 상품명",
            "accountDescription": "한국은행 수시입출금 상품설명",
            "accountType": "DOMESTIC"
        },
        {
            "accountTypeUniqueNo": "020-1-5f3eb083664848",
            "bankCode": "020",
            "bankName": "우리은행",
            "accountTypeCode": "1",
            "accountTypeName": "수시입출금",
            "accountName": "우리은행 수시입출금 상품명",
            "accountDescription": "우리은행 수시입출금 상품설명",
            "accountType": "DOMESTIC"
        },
        {
            "accountTypeUniqueNo": "032-1-72012237b27b4c",
            "bankCode": "032",
            "bankName": "대구은행",
            "accountTypeCode": "1",
            "accountTypeName": "수시입출금",
            "accountName": "대구은행 수시입출금 상품명",
            "accountDescription": "대구은행 수시입출금 상품설명",
            "accountType": "DOMESTIC"
        }
    ]
}
```

---

### 9. 계좌 생성

> 계좌를 생성함. 상품을 조회한 사용자는
상품 고유번호를 통해 계좌를 생성할 수 있음.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/createDemandDepositAccount`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "createDemandDepositAccount",
        "transmissionDate": "20240401",
        "transmissionTime": "100500",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "createDemandDepositAccount",
        "institutionTransactionUniqueNo": "20240215121212123457",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "userKey": "2695628f-11a1-418e-b533-9ae19e0650ec"
    },
    "accountTypeUniqueNo": "001-1-ffa4253081d540"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "createDemandDepositAccount",
        "transmissionDate": "20240401",
        "transmissionTime": "100500",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "createDemandDepositAccount",
        "institutionTransactionUniqueNo": "20240215121212123457"
    },
    "REC": {
        "bankCode": "001",
        "accountNo": "0016174648358792",
        "currency": {
            "currency": "KRW",
            "currencyName": "원화"
        }
    }
}
```

---

### 10. 계좌 목록 조회

> 사용자의 계좌 목록 전체를 조회함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccountList`
- **Method**: `POST`

**Request Body**

```json
{
     "Header": {
        "apiName": "inquireDemandDepositAccountList",
        "transmissionDate": "20240401",
        "transmissionTime": "101000",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireDemandDepositAccountList",
        "institutionTransactionUniqueNo": "20240215121212123473",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "userKey": "2695628f-11a1-418e-b533-9ae19e0650ec"
    }
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireDemandDepositAccountList",
        "transmissionDate": "20240401",
        "transmissionTime": "101000",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "inquireDemandDepositAccountList",
        "institutionTransactionUniqueNo": "20240215121212123473"
    },
    "REC": [
        {
            "bankCode": "001",
            "bankName": "한국은행",
            "userName": "USER",
            "accountNo": "0016174648358792",
            "accountName": "한국은행 수시입출금 상품명",
            "accountTypeCode": "1",
            "accountTypeName": "수시입출금",
            "accountCreatedDate": "20240401",
            "accountExpiryDate": "20290401",
            "dailyTransferLimit": "100000000",
            "oneTimeTransferLimit": "20000000",
            "accountBalance": "0",
            "lastTransactionDate": "",
            "currency": "KRW"
        },
        {
            "bankCode": "020",
            "bankName": "우리은행",
            "userName": "USER",
            "accountNo": "0204667768182760",
            "accountName": "우리은행 수시입출금 상품명",
            "accountTypeCode": "1",
            "accountTypeName": "수시입출금",
            "accountCreatedDate": "20240320",
            "accountExpiryDate": "20290320",
            "dailyTransferLimit": "100000000",
            "oneTimeTransferLimit": "20000000",
            "accountBalance": "8003477",
            "lastTransactionDate": "20240323",
            "currency": "KRW"
        },
        {
            "bankCode": "020",
            "bankName": "우리은행",
            "userName": "USER",
            "accountNo": "0205782816344769",
            "accountName": "우리은행 수시입출금 상품명",
            "accountTypeCode": "1",
            "accountTypeName": "수시입출금",
            "accountCreatedDate": "20240320",
            "accountExpiryDate": "20290320",
            "dailyTransferLimit": "100000000",
            "oneTimeTransferLimit": "20000000",
            "accountBalance": "98516155",
            "lastTransactionDate": "20240325",
            "currency": "KRW"
        },
        {
            "bankCode": "032",
            "bankName": "대구은행",
            "userName": "USER",
            "accountNo": "0324003842129948",
            "accountName": "대구은행 수시입출금 상품명",
            "accountTypeCode": "1",
            "accountTypeName": "수시입출금",
            "accountCreatedDate": "20240320",
            "accountExpiryDate": "20290320",
            "dailyTransferLimit": "100000000",
            "oneTimeTransferLimit": "20000000",
            "accountBalance": "809008344",
            "lastTransactionDate": "20240329",
            "currency": "KRW"
        }
    ]
}
```

---

### 11. 계좌 조회 (단건)

> 특정 계좌에 대한 정보를 조회함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccount`
- **Method**: `POST`

**Request Body**

```json
{     
    "Header": {
        "apiName": "inquireDemandDepositAccount",
        "transmissionDate": "20240401",
        "transmissionTime": "101500",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireDemandDepositAccount",
        "institutionTransactionUniqueNo": "20240215121212123455",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "userKey": "2695628f-11a1-418e-b533-9ae19e0650ec"
    }, 
    "accountNo": "0016174648358792"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireDemandDepositAccount",
        "transmissionDate": "20240401",
        "transmissionTime": "101500",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "inquireDemandDepositAccount",
        "institutionTransactionUniqueNo": "20240215121212123455"
    },
    "REC": {
        "bankCode": "001",
        "bankName": "한국은행",
        "userName": "USER",
        "accountNo": "0016174648358792",
        "accountName": "한국은행 수시입출금 상품명",
        "accountTypeCode": "1",
        "accountTypeName": "수시입출금",
        "accountCreatedDate": "20240401",
        "accountExpiryDate": "20290401",
        "dailyTransferLimit": "100000000",
        "oneTimeTransferLimit": "20000000",
        "accountBalance": "0",
        "lastTransactionDate": "",
        "currency": "KRW"
    }
}
```

---

### 12. 예금주 조회

> 계좌에 대한 예금주명을 조회함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccountHolderName`
- **Method**: `POST`

**Request Body**

```json
{
   "Header": {
        "apiName": "inquireDemandDepositAccountHolderName",
        "transmissionDate": "20240401",
        "transmissionTime": "102000",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireDemandDepositAccountHolderName",
        "institutionTransactionUniqueNo": "20240215121212123451",
         "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "userKey": "2695628f-11a1-418e-b533-9ae19e0650ec"
    },
    "accountNo": "0016174648358792"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireDemandDepositAccountHolderName",
        "transmissionDate": "20240401",
        "transmissionTime": "102000",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "inquireDemandDepositAccountHolderName",
        "institutionTransactionUniqueNo": "20240215121212123451"
    },
    "REC": {
        "bankCode": "001",
        "bankName": "한국은행",
        "accountNo": "0016174648358792",
        "userName": "USER",
        "currency": "KRW"
    }
}
```

---

### 13. 계좌 잔액 조회

> 특정 계좌의 잔액을 조회함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccountBalance`
- **Method**: `POST`

**Request Body**

```json
{
   "Header": {
        "apiName": "inquireDemandDepositAccountBalance",
        "transmissionDate": "20240401",
        "transmissionTime": "102500",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireDemandDepositAccountBalance",
        "institutionTransactionUniqueNo": "20240215121212123463",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "userKey": "2695628f-11a1-418e-b533-9ae19e0650ec"
    },
    "accountNo": "0016174648358792"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireDemandDepositAccountBalance",
        "transmissionDate": "20240401",
        "transmissionTime": "102500",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "inquireDemandDepositAccountBalance",
        "institutionTransactionUniqueNo": "20240215121212123463"
    },
    "REC": {
        "bankCode": "001",
        "accountNo": "0016174648358792",
        "accountBalance": "0",
        "accountCreatedDate": "20240401",
        "accountExpiryDate": "20290401",
        "lastTransactionDate": "",
        "currency": "KRW"
    }
}
```

---

### 14. 계좌 출금

> 이용기관이 사용자의 계좌로부터 대금을 출금함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/updateDemandDepositAccountWithdrawal`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "updateDemandDepositAccountWithdrawal",
        "transmissionDate": "20240401",
        "transmissionTime": "102500",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "updateDemandDepositAccountWithdrawal",
        "institutionTransactionUniqueNo": "20240215121212123456",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "userKey": "2695628f-11a1-418e-b533-9ae19e0650ec"
    },
    "accountNo": "0016174648358792",
    "transactionBalance": "100000",
    "transactionSummary": "(수시입출금) : 출금"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "updateDemandDepositAccountWithdrawal",
        "transmissionDate": "20240401",
        "transmissionTime": "102500",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "updateDemandDepositAccountWithdrawal",
        "institutionTransactionUniqueNo": "20240215121212123456"
    },
    "REC": {
        "transactionUniqueNo": "60",
        "transactionDate": "20240401"
    }
}
```

---

### 15. 계좌 입금

> 이용기관이 사용자의 계좌로부터 대금을 입금함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/updateDemandDepositAccountDeposit`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "updateDemandDepositAccountDeposit",
        "transmissionDate": "20240401",
        "transmissionTime": "102500",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "updateDemandDepositAccountDeposit",
        "institutionTransactionUniqueNo": "20240215121212123463",
         "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "userKey": "2695628f-11a1-418e-b533-9ae19e0650ec"
    },
    "accountNo": "0016174648358792",
    "transactionBalance": "100000000",
    "transactionSummary": "(수시입출금) : 입금"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "updateDemandDepositAccountDeposit",
        "transmissionDate": "20240401",
        "transmissionTime": "102500",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "updateDemandDepositAccountDeposit",
        "institutionTransactionUniqueNo": "20240215121212123463"
    },
    "REC": {
        "transactionUniqueNo": "59",
        "transactionDate": "20240401"
    }
}
```

---

### 16. 계좌 이체

> 한 계좌로부터 다른 계좌로 대금을 이체함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/updateDemandDepositAccountTransfer`
- **Method**: `POST`

**Request Body**

```json
{
     "Header": {
        "apiName": "updateDemandDepositAccountTransfer",
        "transmissionDate": "20240401",
        "transmissionTime": "103500",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "updateDemandDepositAccountTransfer",
        "institutionTransactionUniqueNo": "20240215121212123453",
         "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "userKey": "2695628f-11a1-418e-b533-9ae19e0650ec"
    }, 
    "depositAccountNo": "0204667768182760",
    "depositTransactionSummary": "(수시입출금) : 입금(이체)",
    "transactionBalance": "10000000", 
    "withdrawalAccountNo": "0016174648358792", 
    "withdrawalTransactionSummary": "(수시입출금) : 출금(이체)"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "updateDemandDepositAccountTransfer",
        "transmissionDate": "20240401",
        "transmissionTime": "103500",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "updateDemandDepositAccountTransfer",
        "institutionTransactionUniqueNo": "20240215121212123453"
    },
    "REC": [
        {
            "transactionUniqueNo": "61",
            "accountNo": "0016174648358792",
            "transactionDate": "20240401",
            "transactionType": "2",
            "transactionTypeName": "출금(이체)",
            "transactionAccountNo": "0204667768182760"
        },
        {
            "transactionUniqueNo": "62",
            "accountNo": "0204667768182760",
            "transactionDate": "20240401",
            "transactionType": "1",
            "transactionTypeName": "입금(이체)",
            "transactionAccountNo": "0016174648358792"
        }
    ]
}
```

---

### 17. 계좌 이체 한도 변경

> 계좌에 대한 이체한도를 변경함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/updateTransferLimit`
- **Method**: `POST`

**Request Body**

```json
{
     "Header": {
        "apiName": "updateTransferLimit",
        "transmissionDate": "20240401",
        "transmissionTime": "104000",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "updateTransferLimit",
        "institutionTransactionUniqueNo": "20240215121212123452",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "userKey": "2695628f-11a1-418e-b533-9ae19e0650ec"
    }, 
    "accountNo": "0016174648358792", 
    "oneTimeTransferLimit": "20000000", 
    "dailyTransferLimit": "100000000"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "updateTransferLimit",
        "transmissionDate": "20240401",
        "transmissionTime": "104000",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "updateTransferLimit",
        "institutionTransactionUniqueNo": "20240215121212123452"
    },
    "REC": {
        "bankCode": "001",
        "bankName": "한국은행",
        "userName": "USER",
        "accountNo": "0016174648358792",
        "accountName": "한국은행 수시입출금 상품명",
        "accountTypeCode": "1",
        "accountTypeName": "수시입출금",
        "accountCreatedDate": "20240401",
        "accountExpiryDate": "20290401",
        "dailyTransferLimit": "100000000",
        "oneTimeTransferLimit": "20000000",
        "accountBalance": "89900000",
        "lastTransactionDate": "20240401",
        "currency": "KRW"
    }
}
```

---

### 18. 계좌 거래 내역 조회

> 계좌 거래 내역 목록을 조회함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireTransactionHistoryList`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "inquireTransactionHistoryList",
        "transmissionDate": "20240401",
        "transmissionTime": "105000",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireTransactionHistoryList",
        "institutionTransactionUniqueNo": "20240215121212123459",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "userKey": "2695628f-11a1-418e-b533-9ae19e0650ec"
    },
    "accountNo": "0016174648358792",
    "startDate": "20240101",
    "endDate": "20241231",
    "transactionType": "A",
    "orderByType": "ASC"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireTransactionHistoryList",
        "transmissionDate": "20240401",
        "transmissionTime": "105000",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "inquireTransactionHistoryList",
        "institutionTransactionUniqueNo": "20240215121212123459"
    },
    "REC": {
        "totalCount": "3",
        "list": [
            {
                "transactionUniqueNo": "59",
                "transactionDate": "20240401",
                "transactionTime": "102447",
                "transactionType": "1",
                "transactionTypeName": "입금",
                "transactionAccountNo": "",
                "transactionBalance": "100000000",
                "transactionAfterBalance": "100000000",
                "transactionSummary": "(수시입출금) : 입금",
                "transactionMemo": ""
            },
            {
                "transactionUniqueNo": "60",
                "transactionDate": "20240401",
                "transactionTime": "102452",
                "transactionType": "2",
                "transactionTypeName": "출금",
                "transactionAccountNo": "",
                "transactionBalance": "100000",
                "transactionAfterBalance": "99900000",
                "transactionSummary": "(수시입출금) : 출금",
                "transactionMemo": ""
            },
            {
                "transactionUniqueNo": "61",
                "transactionDate": "20240401",
                "transactionTime": "103229",
                "transactionType": "2",
                "transactionTypeName": "출금(이체)",
                "transactionAccountNo": "0204667768182760",
                "transactionBalance": "10000000",
                "transactionAfterBalance": "89900000",
                "transactionSummary": "(수시입출금) : 출금(이체)",
                "transactionMemo": ""
            }
        ]
    }
}
```

---

### 19. 계좌 거래 내역 조회 (단건)

> 계좌 거래 내역 (단건)을 조회함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireTransactionHistory`
- **Method**: `POST`

**Request Body**

```json
{
   "Header": {
        "apiName": "inquireTransactionHistory",
        "transmissionDate": "20240401",
        "transmissionTime": "105500",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireTransactionHistory",
        "institutionTransactionUniqueNo": "20240215121212123452",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "userKey": "2695628f-11a1-418e-b533-9ae19e0650ec"    
    },
    "accountNo": "0016174648358792",
    "transactionUniqueNo": "61"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireTransactionHistory",
        "transmissionDate": "20240401",
        "transmissionTime": "105500",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "inquireTransactionHistory",
        "institutionTransactionUniqueNo": "20240215121212123452"
    },
    "REC": {
        "transactionUniqueNo": "61",
        "transactionDate": "20240401",
        "transactionTime": "103229",
        "transactionType": "2",
        "transactionTypeName": "출금(이체)",
        "transactionAccountNo": "0204667768182760",
        "transactionBalance": "10000000",
        "transactionAfterBalance": "89900000",
        "transactionSummary": "(수시입출금) : 출금(이체)",
        "transactionMemo": ""
    }
}
```

---

## 카드

### 20. 카테고리 조회

> 가맹점 등록을 위한 카테고리를 조회하는 API

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCategoryList`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "inquireCategoryList",
        "transmissionDate": "20240409",
        "transmissionTime": "094600",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireCategoryList",
        "institutionTransactionUniqueNo": "20240215121212123555",
        "apiKey": "91841f63099c434e8190e992c56d2fe1"
    }
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireCategoryList",
        "transmissionDate": "20240409",
        "transmissionTime": "094600",
        "institutionCode": "00100",
        "apiKey": "91841f63099c434e8190e992c56d2fe1",
        "apiServiceCode": "inquireCategoryList",
        "institutionTransactionUniqueNo": "20240215121212123555"
    },
    "REC": [
        {
            "categoryId": "CG-3fa85f6425e811e",
            "categoryName": "주유",
            "categoryDescription": ""
        },
        {
            "categoryId": "CG-4fa85f6425ad1d3",
            "categoryName": "대형마트",
            "categoryDescription": ""
        },
        {
            "categoryId": "CG-4fa85f6455cad4a",
            "categoryName": "교통",
            "categoryDescription": "(버스, 지하철, 택시)"
        },
        {
            "categoryId": "CG-6dd85f6425ez11o",
            "categoryName": "교육/육아",
            "categoryDescription": ""
        },
        {
            "categoryId": "CG-7fa85f6425bc311",
            "categoryName": "통신",
            "categoryDescription": "(전화요금, 인터넷 이용료, 케이블TV 업종)"
        },
        {
            "categoryId": "CG-8fa85f6425e1123",
            "categoryName": "해외",
            "categoryDescription": "(해외직구)"
        },
        {
            "categoryId": "CG-9ca85f66311a23d",
            "categoryName": "생활",
            "categoryDescription": "(음식점, 커피전문점, 편의점, 약국 ..)"
        }
    ]
}
```

---

### 21. 가맹점 등록

> 카테고리 조회 후 가맹점 등록.
카테고리 하위로 다양한 가맹점을 생성할 수 있으며,
추후 등록된 가맹점 목록 내에서 카드 결제를 할 수 있음.
가맹점 목록 조회 API를 통해 샘플 데이터를 참고하여
가맹점을 등록할 수 있음.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/createMerchant`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "createMerchant",
        "transmissionDate": "20240409",
        "transmissionTime": "094800",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "createMerchant",
        "institutionTransactionUniqueNo": "20240215121212123553",
        "apiKey": "91841f63099c434e8190e992c56d2fe1"
    },
    "categoryId": "CG-4fa85f6425ad1d3",
    "merchantName": "코스트코"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "createMerchant",
        "transmissionDate": "20240409",
        "transmissionTime": "094800",
        "institutionCode": "00100",
        "apiKey": "91841f63099c434e8190e992c56d2fe1",
        "apiServiceCode": "createMerchant",
        "institutionTransactionUniqueNo": "20240215121212123553"
    },
    "REC": [
        {
            "categoryId": "CG-9ca85f66311a23d",
            "categoryName": "생활",
            "merchantId": "1",
            "merchantName": "스타벅스"
        },
        {
            "categoryId": "CG-4fa85f6455cad4a",
            "categoryName": "교통",
            "merchantId": "2",
            "merchantName": "지하철"
        },
        {
            "categoryId": "CG-4fa85f6425ad1d3",
            "categoryName": "대형마트",
            "merchantId": "3",
            "merchantName": "코스트코"
        }
    ]
}
```

---

### 22. 카드사 조회

> 카드 상품 등록 시 필요한 카드사를 조회하는 API
카드사를 조회하여 각 카드 상품을 만들 수 있음.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCardIssuerCodesList`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "inquireCardIssuerCodesList",
        "transmissionDate": "20240409",
        "transmissionTime": "095900",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireCardIssuerCodesList",
        "institutionTransactionUniqueNo": "20240215121212123553",
        "apiKey": "91841f63099c434e8190e992c56d2fe1"
    }
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireCardIssuerCodesList",
        "transmissionDate": "20240409",
        "transmissionTime": "095900",
        "institutionCode": "00100",
        "apiKey": "91841f63099c434e8190e992c56d2fe1",
        "apiServiceCode": "inquireCardIssuerCodesList",
        "institutionTransactionUniqueNo": "20240215121212123553"
    },
    "REC": [
        {
            "cardIssuerCode": "1001",
            "cardIssuerName": "KB국민카드"
        },
        {
            "cardIssuerCode": "1002",
            "cardIssuerName": "삼성카드"
        }
    ]
}
```

---

### 23. 카드 상품 등록

> 카드사별 카드 상품을 등록함.
카테고리별 카드 혜택을 설정하여 추후 실적에 따라 금액을 할인받을 수 있음.
카드 혜택은 반드시 하나 이상 지정되어야 하며, 기준실적이 0일 경우 조건없이 혜택 적용.
카드 상품 조회 API를 통해 샘플 데이터를 참고하여 상품을 등록 가능.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/createCreditCardProduct`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "createCreditCardProduct",
        "transmissionDate": "20240409",
        "transmissionTime": "095900",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "createCreditCardProduct",
        "institutionTransactionUniqueNo": "20240215121212123579",
        "apiKey": "91841f63099c434e8190e992c56d2fe1"
    },
    "cardIssuerCode": "1003",
    "cardName": "디지로카 London",
    "baselinePerformance": "700000",
    "maxBenefitLimit": "130000",
    "cardDescription": "생활 20%할인, 교통 10% 할인, 대형마트 5% 할인",
    "cardBenefits": [
        {
            "categoryId": "CG-9ca85f66311a23d",
            "discountRate": "20"
        },
        {
            "categoryId": "CG-4fa85f6455cad4a",
            "discountRate": "10"
        },
        {
            "categoryId": "CG-4fa85f6425ad1d3",
            "discountRate": "5"
        }
    ] 
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "createCreditCardProduct",
        "transmissionDate": "20240409",
        "transmissionTime": "095900",
        "institutionCode": "00100",
        "apiKey": "91841f63099c434e8190e992c56d2fe1",
        "apiServiceCode": "createCreditCardProduct",
        "institutionTransactionUniqueNo": "20240215121212123579"
    },
    "REC": {
        "cardUniqueNo": "1003-a139e9f23f1a4cc",
        "cardIssuerCode": "1003",
        "cardIssuerName": "롯데카드",
        "cardName": "디지로카 London",
        "cardTypeCode": "1",
        "cardTypeName": "신용카드",
        "baselinePerformance": "700000",
        "maxBenefitLimit": "130000",
        "cardDescription": "생활 20%할인",
        "cardBenefitsInfo": [
            {
                "categoryId": "CG-9ca85f66311a23d",
                "categoryName": "생활",
                "discountRate": "20.0"
            },
            {
                "categoryId": "CG-4fa85f6455cad4a",
                "categoryName": "교통",
                "discountRate": "10.0"
            },
            {
                "categoryId": "CG-4fa85f6425ad1d3",
                "categoryName": "대형마트",
                "discountRate": "5.0"
            }
        ]
    }
}
```

---

### 24. 카드 상품 조회

> 카드사별 카드 상품을 조회함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCreditCardList`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "inquireCreditCardList",
        "transmissionDate": "20240409",
        "transmissionTime": "100100",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireCreditCardList",
        "institutionTransactionUniqueNo": "20240215121212123553",
        "apiKey": "91841f63099c434e8190e992c56d2fe1"
    }
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireCreditCardList",
        "transmissionDate": "20240409",
        "transmissionTime": "100100",
        "institutionCode": "00100",
        "apiKey": "91841f63099c434e8190e992c56d2fe1",
        "apiServiceCode": "inquireCreditCardList",
        "institutionTransactionUniqueNo": "20240215121212123553"
    },
    "REC": [
        {
            "cardUniqueNo": "1003-a139e9f23f1a4cc",
            "cardIssuerCode": "1003",
            "cardIssuerName": "롯데카드",
            "cardName": "디지로카 London",
            "cardTypeCode": "1",
            "cardTypeName": "신용카드",
            "baselinePerformance": "700000",
            "maxBenefitLimit": "130000",
            "cardDescription": "생활 20%할인, 교통 10% 할인, 대형마트 5% 할인",
            "cardBenefitsInfo": [
                {
                    "categoryId": "CG-9ca85f66311a23d",
                    "categoryName": "생활",
                    "discountRate": "20.0"
                },
                {
                    "categoryId": "CG-4fa85f6455cad4a",
                    "categoryName": "교통",
                    "discountRate": "10.0"
                },
                {
                    "categoryId": "CG-4fa85f6425ad1d3",
                    "categoryName": "대형마트",
                    "discountRate": "5.0"
                }
            ]
        },
        {
            "cardUniqueNo": "1005-2d29fc2343024a4",
            "cardIssuerCode": "1005",
            "cardIssuerName": "신한카드",
            "cardName": "신한 TRAVEL 카드",
            "cardTypeCode": "1",
            "cardTypeName": "신용카드",
            "baselinePerformance": "100000",
            "maxBenefitLimit": "100000",
            "cardDescription": "해외 결제시 15% 할인",
            "cardBenefitsInfo": [
                {
                    "categoryId": "CG-8fa85f6425e1123",
                    "categoryName": "해외",
                    "discountRate": "15.0"
                }
            ]
        }
    ]
}
```

---

### 25. 카드 생성

> 카드를 생성함. 카드 상품을 조회한 사용자는
카드 고유번호를 통해 카드를 생성할 수 있음.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/createCreditCardProduct`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "createCreditCard",
        "transmissionDate": "20240409",
        "transmissionTime": "103500",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "createCreditCard",
        "institutionTransactionUniqueNo": "20240215121212123556",
        "apiKey": "91841f63099c434e8190e992c56d2fe1",
        "userKey": "6816c0c0-ec9e-4f62-8092-5a99d84f02cd"
    },
    "cardUniqueNo": "1003-a139e9f23f1a4cc",
    "withdrawalAccountNo": "032355504232351",
    "withdrawalDate": "4"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "createCreditCard",
        "transmissionDate": "20240409",
        "transmissionTime": "103500",
        "institutionCode": "00100",
        "apiKey": "91841f63099c434e8190e992c56d2fe1",
        "apiServiceCode": "createCreditCard",
        "institutionTransactionUniqueNo": "20240215121212123556"
    },
    "REC": {
        "cardNo": "1003622654847049",
        "cvc": "713",
        "cardUniqueNo": "1003-a139e9f23f1a4cc",
        "cardIssuerCode": "1003",
        "cardIssuerName": "롯데카드",
        "cardName": "디지로카 SEOUL",
        "baselinePerformance": "0",
        "maxBenefitLimit": "200000",
        "cardDescription": "생활 20%할인",
        "cardExpiryDate": "20290409",
        "withdrawalAccountNo": "032355504232351",
        "withdrawalDate": "4"
    }
}
```

---

### 26. 내 카드 목록 조회

> 사용자의 카드 목록 전체를 조회함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireSignUpCreditCardList`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "inquireSignUpCreditCardList",
        "transmissionDate": "20240409",
        "transmissionTime": "103600",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireSignUpCreditCardList",
        "institutionTransactionUniqueNo": "20240215121212123557",
        "apiKey": "91841f63099c434e8190e992c56d2fe1",
        "userKey": "6816c0c0-ec9e-4f62-8092-5a99d84f02cd"
    }
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireSignUpCreditCardList",
        "transmissionDate": "20240409",
        "transmissionTime": "103600",
        "institutionCode": "00100",
        "apiKey": "91841f63099c434e8190e992c56d2fe1",
        "apiServiceCode": "inquireSignUpCreditCardList",
        "institutionTransactionUniqueNo": "20240215121212123557"
    },
    "REC": [
        {
            "cardNo": "1003198565339181",
            "cvc": "149",
            "cardUniqueNo": "1003-a139e9f23f1a4cc",
            "cardIssuerCode": "1003",
            "cardIssuerName": "롯데카드",
            "cardName": "디지로카 SEOUL",
            "baselinePerformance": "0",
            "maxBenefitLimit": "200000",
            "cardDescription": "생활 20%할인",
            "cardExpiryDate": "20290409",
            "withdrawalAccountNo": "032355504232351",
            "withdrawalDate": "4"
        },
        {
            "cardNo": "1005518816096479",
            "cvc": "725",
            "cardUniqueNo": "1005-992db475fbb944c",
            "cardIssuerCode": "1005",
            "cardIssuerName": "신한카드",
            "cardName": "신한 TRAVEL 카드",
            "baselinePerformance": "100000",
            "maxBenefitLimit": "100000",
            "cardDescription": "해외 결제시 15% 할인",
            "cardExpiryDate": "20290403",
            "withdrawalAccountNo": "032355504232351",
            "withdrawalDate": "1"
        }
    ]
}
```

---

### 27. 가맹점 목록 조회

> 카드 결제에 필요한 가맹점 목록을 조회함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireMerchantList`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "inquireMerchantList",
        "transmissionDate": "20240408",
        "transmissionTime": "135600",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireMerchantList",
        "institutionTransactionUniqueNo": "20240215121212123551",
        "apiKey": "21d5e78661d7490895eaebb24f1dfc42"
    }
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireMerchantList",
        "transmissionDate": "20240408",
        "transmissionTime": "135600",
        "institutionCode": "00100",
        "apiKey": "21d5e78661d7490895eaebb24f1dfc42",
        "apiServiceCode": "inquireMerchantList",
        "institutionTransactionUniqueNo": "20240215121212123551"
    },
    "REC": [
        {
            "categoryId": "CG-4fa85f6425ad1d3",
            "categoryName": "대형마트",
            "merchantId": "1",
            "merchantName": "코스트코"
        },
        {
            "categoryId": "CG-4fa85f6425ad1d3",
            "categoryName": "대형마트",
            "merchantId": "2",
            "merchantName": "홈플러스"
        },
        {
            "categoryId": "CG-8fa85f6425e1123",
            "categoryName": "해외",
            "merchantId": "3",
            "merchantName": "알리 익스프레스"
        },
        {
            "categoryId": "CG-8fa85f6425e1123",
            "categoryName": "해외",
            "merchantId": "4",
            "merchantName": "아마존 익스프레스"
        },
        {
            "categoryId": "CG-7fa85f6425bc311",
            "categoryName": "통신",
            "merchantId": "5",
            "merchantName": "SKT"
        },
        {
            "categoryId": "CG-7fa85f6425bc311",
            "categoryName": "통신",
            "merchantId": "6",
            "merchantName": "LG 유플러스"
        },
        {
            "categoryId": "CG-9ca85f66311a23d",
            "categoryName": "생활",
            "merchantId": "7",
            "merchantName": "스타벅스"
        },
        {
            "categoryId": "CG-9ca85f66311a23d",
            "categoryName": "생활",
            "merchantId": "8",
            "merchantName": "김밥천국"
        },
        {
            "categoryId": "CG-9ca85f66311a23d",
            "categoryName": "생활",
            "merchantId": "9",
            "merchantName": "뚜레쥬르"
        }
    ]
}
```

---

### 28. 카드 결제

> 조회한 가맹점에서 카드 결제함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/createCreditCardTransaction`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "createCreditCardTransaction",
        "transmissionDate": "20240408",
        "transmissionTime": "135600",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "createCreditCardTransaction",
        "institutionTransactionUniqueNo": "20240215121212123571",
        "apiKey": "21d5e78661d7490895eaebb24f1dfc42",
        "userKey": "4dfb0125-27c9-4ab1-9c72-28772c59894a"
    },
    "cardNo": "1005518816096479",
    "cvc": "725",
    "merchantId": "1",
    "paymentBalance": "500000"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "createCreditCardTransaction",
        "transmissionDate": "20240408",
        "transmissionTime": "135600",
        "institutionCode": "00100",
        "apiKey": "21d5e78661d7490895eaebb24f1dfc42",
        "apiServiceCode": "createCreditCardTransaction",
        "institutionTransactionUniqueNo": "20240215121212123571"
    },
    "REC": {
        "transactionUniqueNo": "12",
        "categoryId": "CG-4fa85f6425ad1d3",
        "categoryName": "대형마트",
        "merchantId": "1",
        "merchantName": "코스트코",
        "transactionDate": "20240408",
        "transactionTime": "135242",
        "paymentBalance": "500000"
    }
}
```

---

### 29. 카드 결제 내역 조회

> 카드 결제한 내역을 조회함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireCreditCardTransactionList`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "inquireCreditCardTransactionList",
        "transmissionDate": "20240418",
        "transmissionTime": "131500",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireCreditCardTransactionList",
        "institutionTransactionUniqueNo": "20240215121212123507",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "userKey": "2695628f-11a1-418e-b533-9ae19e0650ec"
    },
    "cardNo": "1005518816096479",
    "cvc": "725",
    "startDate": "20240401",
    "endDate": "20240502"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireCreditCardTransactionList",
        "transmissionDate": "20240418",
        "transmissionTime": "131500",
        "institutionCode": "00100",
        "apiKey": "6a028e66ddbf42a6b783d78963163e29",
        "apiServiceCode": "inquireCreditCardTransactionList",
        "institutionTransactionUniqueNo": "20240215121212123507"
    },
    "REC": {
        "cardIssuerCode": "1005",
        "cardIssuerName": "신한카드",
        "cardName": "신한 TRAVEL 카드",
        "cardNo": "1005518816096479",
        "estimatedBalance": "2000000",
        "transactionList": [
            {
                "transactionUniqueNo": "20",
                "categoryId": "CG-3fa85f6425e811e",
                "categoryName": "주유",
                "merchantId": "1",
                "merchantName": "SK 에너지",
                "transactionDate": "20240418",
                "transactionTime": "094431",
                "transactionBalance": "1000000",
                "cardStatus": "승인",
                "billStatementsYn": "N",
                "billStatementsStatus": "미결제"
            },
            {
                "transactionUniqueNo": "19",
                "categoryId": "CG-3fa85f6425e811e",
                "categoryName": "주유",
                "merchantId": "1",
                "merchantName": "SK 에너지",
                "transactionDate": "20240418",
                "transactionTime": "094421",
                "transactionBalance": "500000",
                "cardStatus": "승인",
                "billStatementsYn": "N",
                "billStatementsStatus": "미결제"
            }
        ]
    }
}
```

---

### 30. 카드 결제 취소

> 거래 고유번호를 이용하여 결제를 취소함.
청구서가 발행된 거래의 경우 취소가 불가함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/deleteTransaction`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "deleteTransaction",
        "transmissionDate": "20240408",
        "transmissionTime": "140200",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "deleteTransaction",
        "institutionTransactionUniqueNo": "20240215121212123561",
        "apiKey": "21d5e78661d7490895eaebb24f1dfc42",
        "userKey": "4dfb0125-27c9-4ab1-9c72-28772c59894a"
    },
    "cardNo": "1005518816096479",
    "cvc": "725",
    "transactionUniqueNo": "33"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "deleteTransaction",
        "transmissionDate": "20240408",
        "transmissionTime": "161900",
        "institutionCode": "00100",
        "apiKey": "21d5e78661d7490895eaebb24f1dfc42",
        "apiServiceCode": "deleteTransaction",
        "institutionTransactionUniqueNo": "20240215121212123563"
    },
    "REC": {
          "transactionUniqueNo": "33",
          "categoryId": "CG-4fa85f6425ad1d3",
          "categoryName": "대형마트",
          "merchantId": "2",
          "merchantName": "홈플러스",
          "transactionDate": "20240408",
          "transactionTime": "162147",
          "transactionBalance": "30000",
          "status": "CANCEL"
    }
}
```

---

### 31. 청구서 조회

> 카드 청구서를 조회함.
카드 청구서는 월~일(일주일)에 해당하는 카드 거래 내역을 반영하여
차주 월요일 07:30에 발행되며, 출금은 설정한 출금 날짜 16:00에
출금 연결 계좌(수시입출금)에서 자동 출금됨.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/inquireBillingStatements`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "inquireBillingStatements",
        "transmissionDate": "20240408",
        "transmissionTime": "140400",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "inquireBillingStatements",
        "institutionTransactionUniqueNo": "20240215121212123501",
        "apiKey": "21d5e78661d7490895eaebb24f1dfc42",
        "userKey": "4dfb0125-27c9-4ab1-9c72-28772c59894a"
    },
    "cardNo": "1005518816096479",
    "cvc": "725",
    "startMonth":"202401",
    "endMonth":"202403"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "inquireBillingStatements",
        "transmissionDate": "20240408",
        "transmissionTime": "140400",
        "institutionCode": "00100",
        "apiKey": "21d5e78661d7490895eaebb24f1dfc42",
        "apiServiceCode": "inquireBillingStatements",
        "institutionTransactionUniqueNo": "20240215121212123501"
    },
    "REC": [
        {
            "billingMonth": "202403",
            "billingList": [
                {
                    "billingWeek": "5",
                    "billingDate":"20240326",
                    "totalBalance": "285000",
                    "status": "미결제",
                    "paymentDate": "",
                    "paymentTime": ""
                }
            ]
        }
    ]
}
```

---

### 32. 카드 결제 계좌 수정

> 청구 금액을 자동이체하는 출금 연결계좌(수시입출금)와
출금 날짜를 변경함.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/creditCard/updateWithdrawalAccount`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "updateWithdrawalAccount",
        "transmissionDate": "20240408",
        "transmissionTime": "140400",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "updateWithdrawalAccount",
        "institutionTransactionUniqueNo": "20240215121212123561",
        "apiKey": "21d5e78661d7490895eaebb24f1dfc42",
        "userKey": "4dfb0125-27c9-4ab1-9c72-28772c59894a"
    },
    "cardNo": "1005518816096479",
    "cvc": "725",
    "withdrawalAccountNo": "0011541149756547",
    "withdrawalDate": "1"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "updateWithdrawalAccount",
        "transmissionDate": "20240408",
        "transmissionTime": "140400",
        "institutionCode": "00100",
        "apiKey": "21d5e78661d7490895eaebb24f1dfc42",
        "apiServiceCode": "updateWithdrawalAccount",
        "institutionTransactionUniqueNo": "20240215121212123561"
    },
    "REC": {
        "cardNo": "1005518816096479",
        "cvc": "725",
        "cardUniqueNo": "1005-992db475fbb944c",
        "cardIssuerCode": "1005",
        "cardIssuerName": "신한카드",
        "cardName": "신한 TRAVEL 카드",
        "baselinePerformance": "10",
        "maxBenefitLimit": "100000",
        "cardDescription": "해외 결제시 15% 할인",
        "cardExpiryDate": "20290403",
        "withdrawalAccountNo": "0011541149756547",
        "withdrawalDate": "1"
    }
}
```

---

## 1원 인증

### 33. 1원 송금

> 회원의 실명 계좌를 확인하기 위해 기업명과 인증코드를 포함하여 1원을 송금함.
인증코드는 4자리 숫자로 랜덤 생성되며, 기업명은 앱 식별을 위해 앱 관리자가(교육생) 입력함.
인증코드는 거래 내역에서 '기업명 인증코드' 형식으로 조회됨.

예시)
기업명: SSAFY
인증코드: 1234
거래 내역: SSAFY 1234
📌 원화 수시입출금 상품에만 사용 가능합니다.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/accountAuth/openAccountAuth`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "openAccountAuth",
        "transmissionDate": "20240723",
        "transmissionTime": "152345",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "openAccountAuth",
        "institutionTransactionUniqueNo": "20240723152345666098",
        "apiKey": "e8fb2ac291804bc98834ff7bcef7e340",
        "userKey": "633d65a0-67c4-48b2-9bbf-94b948d8e141"
    },
    "accountNo" : "0011214764051239",
    "authText": "SSAFY"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "openAccountAuth",
        "transmissionDate": "20240723",
        "transmissionTime": "152345",
        "institutionCode": "00100",
        "apiKey": "e8fb2ac291804bc98834ff7bcef7e340",
        "apiServiceCode": "openAccountAuth",
        "institutionTransactionUniqueNo": "20240723152345666098"
    },
    "REC": {
        "transactionUniqueNo": "7",
        "accountNo": "0011214764051239"
    }
}
```

---

### 34. 1원 송금 검증

> 송금 내역에서 조회되는 기업명, 인증코드와 입력한 기업명, 인증코드가 일치하는지 검증.
계좌거래내역조회에서 인증코드를 확인할 수 있음.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/accountAuth/checkAuthCode`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "checkAuthCode",
        "transmissionDate": "20240723",
        "transmissionTime": "152415",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "checkAuthCode",
        "institutionTransactionUniqueNo": "20240723152415461262",
        "apiKey": "e8fb2ac291804bc98834ff7bcef7e340",
        "userKey": "633d65a0-67c4-48b2-9bbf-94b948d8e141"
    },
    "accountNo" : "0011214764051239",
    "authText": "SSAFY",
    "authCode": "8212"
}
```

**Response Body**

```json
{
    "Header": {
        "responseCode": "H0000",
        "responseMessage": "정상처리 되었습니다.",
        "apiName": "checkAuthCode",
        "transmissionDate": "20240723",
        "transmissionTime": "152415",
        "institutionCode": "00100",
        "apiKey": "e8fb2ac291804bc98834ff7bcef7e340",
        "apiServiceCode": "checkAuthCode",
        "institutionTransactionUniqueNo": "20240723152415461262"
    },
    "REC": {
        "status": "SUCCESS",
        "transactionUniqueNo": "7",
        "accountNo": "0011214764051239"
    }
}
```

---

## 거래내역 메모

### 35. 거래내역 메모

> 원화 및 외화 수시입출금 거래내역에 대한 메모를 작성하고
수정할 수 있음.

- **URL**: `https://finopenapi.ssafy.io/ssafy/api/v1/edu/transactionMemo`
- **Method**: `POST`

**Request Body**

```json
{
    "Header": {
        "apiName": "transactionMemo",
        "transmissionDate": "20240723",
        "transmissionTime": "152545",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "transactionMemo",
        "institutionTransactionUniqueNo": "20240723152545874018",
        "apiKey": "e8fb2ac291804bc98834ff7bcef7e340",
        "userKey": "633d65a0-67c4-48b2-9bbf-94b948d8e141"
    },
    "accountNo": "0011214764051239",
    "transactionUniqueNo": "6",
    "transactionMemo": "적금 만기"
}
```

**Response Body**

```json
{
    "Header": {
        "apiName": "createTransactionMemo",
        "transmissionDate": "20240723",
        "transmissionTime": "152545",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "createTransactionMemo",
        "institutionTransactionUniqueNo": "20240723152545874018",
        "apiKey": "e8fb2ac291804bc98834ff7bcef7e340",
        "userKey": "633d65a0-67c4-48b2-9bbf-94b948d8e141"
    },
    "REC": {
        "memoUniqueNo": "2",
        "accountNo": "0011214764051239",
        "transactionUniqueNo": 6,
        "transactionMemo": "적금 만기",
        "created": "2024-07-23T15:25:45.382886700+09:00[Asia/Seoul]"
    }
}
```

---