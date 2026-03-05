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

### 🧩 Key Implementation
- **AI Orchestration:** 다중 LLM(OpenAI, Google Gemini) API를 활용한 컨텐츠 품질 최적화.
- **Robust Integration:** Java Spring Boot 기반의 안정적인 API 통신 모듈 및 인증(OAuth 2.0) 처리.
- **Hybrid Desktop Arch:** Electron과 Java 로컬 서버 간의 고성능 REST/IPC 통신 구조 확립.
- **Containerized Environment:** Docker 기반 빌드 표준화로 서비스 실행 및 배포 환경의 일관성 확보.
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
![Demo GIF](https://private-user-images.githubusercontent.com/117149097/558754140-f265ecfc-f625-4cd9-9e3c-5617135f9c62.gif?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3NzI3MjA5NjEsIm5iZiI6MTc3MjcyMDY2MSwicGF0aCI6Ii8xMTcxNDkwOTcvNTU4NzU0MTQwLWYyNjVlY2ZjLWY2MjUtNGNkOS05ZTNjLTU2MTcxMzVmOWM2Mi5naWY_WC1BbXotQWxnb3JpdGhtPUFXUzQtSE1BQy1TSEEyNTYmWC1BbXotQ3JlZGVudGlhbD1BS0lBVkNPRFlMU0E1M1BRSzRaQSUyRjIwMjYwMzA1JTJGdXMtZWFzdC0xJTJGczMlMkZhd3M0X3JlcXVlc3QmWC1BbXotRGF0ZT0yMDI2MDMwNVQxNDI0MjFaJlgtQW16LUV4cGlyZXM9MzAwJlgtQW16LVNpZ25hdHVyZT0zODZmMzQ5NmNlNWEyYjJlYzVmOTUxNDE1YTVjYjNlZTNjMmVkMDBhYWNmNTZmMDg2Y2I5YTJjYmY5MTJjMzQwJlgtQW16LVNpZ25lZEhlYWRlcnM9aG9zdCJ9.duooaHx8AqVVZQh5w6OART5yqGk3_p5UfzFCXcQ9S20)
