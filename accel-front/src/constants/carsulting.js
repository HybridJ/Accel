export const checkList = [
    {
        id: 1,
        question: '구매 예산이 얼마인가요?',                 // 질문
        help: '차량 가격의 상한선을 만원 단위로 입력해주세요!', // 응답 도움말
        type: 'number'                                   // 입력 타입 (숫자, 서술형, 객관식 등)
    },
    {
        id: 2,
        question: '주 사용 용도는 어떻게 되시나요?',
        help: '차량을 사용하는 주된 목적을 골라주세요! (중복 선택 가능)',
        type: 'multipleOption',  // 출퇴근, 가족 이동, 장거리, 업무, 여행&캠핑, 드라이브, 마실용
        options: [
            { id: 'COMMUTE', answer: '🚗출퇴근' },
            { id: 'DAILY', answer: '🛒장보기·근거리 이동' },
            { id: 'FAMILY', answer: '👨‍👩‍👧‍👦가족 이동' },
            { id: 'CHILDCARE', answer: '🎒자녀 등하원·육아' },
            { id: 'LONG_DISTANCE', answer: '🛣️장거리 이동' },
            { id: 'BUSINESS', answer: '💼업무·영업용' },
            { id: 'TRAVEL', answer: '🧳여행' },
            { id: 'CAMPING', answer: '🏕️캠핑·차박' },
            { id: 'LEISURE', answer: '🚴레저·스포츠 활동' },
            { id: 'DRIVE', answer: '🏎️취미 드라이브' },
            { id: 'CARGO', answer: '📦짐 운반' },
            { id: 'PET', answer: '🐶반려동물 동반' },
            { id: 'PICKUP', answer: '🙋가족·지인 픽업' },
            { id: 'SECOND_CAR', answer: '🚙세컨드카' }
        ]
    },
    {
        id: 3,
        question: '라이프 스타일이 어떤가요?',
        help: '나이, 동거 가족, 자녀 유무, 평소 탑승 인원 등을 기입해주시면 더 정확한 결과를 받으실 수 있습니다!',
        type: 'descriptive'
    },
    {
        id: 4,
        question: '주행 환경에 대해 설명해주세요.',
        help: '도심ㆍ고속도로 비중, 출퇴근 거리 혹은 월 평균 주행거리, 주차 공간에 대해 설명해주시면 더 정확한 결과를 받으실 수 있습니다!',
        type: 'descriptive',
    },
    {
        id: 5,
        question: '최대 납부 가능한 유지비는 얼마인가요?',
        help: '월단위 소비 계획에 포함 가능한 비용을 차량 구매 비용을 제외하고 만원 단위로 입력해주세요!',
        type: 'number'
    },
    {
        id: 6,
        question: '전기차도 추천 받으시겠어요?',
        type: 'yesNo'
    }
]