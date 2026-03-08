# 🤖 AI-Log: Content Automation System
> **엔터프라이즈 시스템 설계 역량을 기반으로 한 AI 블로그 자동화 솔루션**

![Java](https://img.shields.io/badge/Java-17-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green?logo=springboot)
![Electron](https://img.shields.io/badge/Electron-Latest-blue?logo=electron)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![OpenAI](https://img.shields.io/badge/AI-GPT--4%20%2F%20Gemini-purple?logo=openai)
---

## 📌 Project Strategy
본 프로젝트는 **PLM/ALM 환경에서 축적한 시스템 통합(SI) 및 워크플로우 설계 경험**을 퍼스널 도구로 확장한 사례입니다. 
단순한 스크립트를 넘어, 확장 가능한 **Backend-to-Desktop 아키텍처**를 지향합니다.
[소스파일이아닌 실행파일만 원하는경우는 여기를 클릭해주세요]

### 🧩 Key Implementation
- **AI Integration:** OpenAI와 Google Gemini API를 활용하여 AI 기반 콘텐츠 생성 기능 구현
- **Backend Communication:** Java Spring Boot 기반 API 통신 모듈 및 OAuth 2.0 인증 처리 구현
- **Desktop Architecture:** Electron과 Java 로컬 서버 간 REST API 기반 통신 구조 설계
- **Build Environment:** Docker를 활용한 개발 및 실행 환경 표준화
---


## 🏗️ System Data Flow

```mermaid
sequenceDiagram
    participant User
    participant Electron as Electron UI (JS)
    participant Java as Spring Boot (Java 17)
    participant AI as LLM API (GPT/Gemini)
    participant Blog as Blog Platform

    User->>Electron: 카테고리/키워드 입력
    Electron->>Java: 분석 요청 (REST API)
    Java->>AI: 컨텐츠 생성 및 최적화 요청
    AI-->>Java: 생성된 포스팅 데이터 반환
    Java->>Blog: 네이버 인증 및 업로드 준비
    Note over Java, Blog: 현재 네이버 로그인 및 인증 연동 완료
    Java-->>Electron: 최종 데이터 및 상태 반환
```

## 📺 시연 영상 (Demo)
![Demo GIF_1](https://github.com/user-attachments/assets/c5a2803f-d8e9-4c2b-a8ac-319679953e92)
![Demo GIF_2](https://github.com/user-attachments/assets/f1479f4b-7129-4b4b-ab91-cb9066421371)

