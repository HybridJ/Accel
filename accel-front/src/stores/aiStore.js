import axios from "axios";
import { defineStore } from "pinia";
import { computed, ref, watch } from "vue";
import { surveyList } from "@/constants/carbti";
import { checkList } from "@/constants/carsulting";
import { API_BASE_URL } from "@/constants/api";

export const useAiStore = defineStore("ai", () => {
    const REST_URL = `${API_BASE_URL}/ai/`
    
    const agent = ref("");
    const questions = ref([]);
    const result = ref([]);
    const aiResponse = ref(null);

    const questionMap = {
        carbti: surveyList,
        carsulting: checkList,
    };

    const parseAiResponse = (data) => {
        const rawAnswer = typeof data === "string" ? data : data?.answer;
        
        if (typeof rawAnswer !== "string") {
            return data;
        }

        try {
            return JSON.parse(rawAnswer);
        } catch {
            const isCarbtiResponse = agent.value === "carbti";

            return {
                brandId: null,
                vehicleId: null,
                model: isCarbtiResponse ? "ACCEL CarBTI" : "ACCEL Recommended Car",
                summary: "",
                resaon: rawAnswer,
            };
        }
    }

    const fetchAiService = async () => {
        const response = await axios.post(`${REST_URL}${agent.value}`, result.value)
        aiResponse.value = parseAiResponse(response.data);
        return aiResponse.value;
    }

    const isEmptyAnswer = (answer) => {
        if (Array.isArray(answer)) {
            return answer.length === 0;
        }

        if (typeof answer === "string") {
            return answer.trim() === "";
        }

        return answer === null || answer === undefined;
    }

    const getMissingQuestionNumbers = () => {
        return questions.value
            .map((question, index) => {
                const answer = result.value[index]?.answer;
                return isEmptyAnswer(answer) ? question.id : null;
            })
            .filter((questionId) => questionId !== null);
    }

    const answeredCount = computed(() => {
        return questions.value.filter((_, index) => !isEmptyAnswer(result.value[index]?.answer)).length;
    });

    const surveyProgress = computed(() => {
        if (questions.value.length === 0) {
            return 0;
        }

        return Math.round((answeredCount.value / questions.value.length) * 100);
    });

    const setResult = (questionIndex, answer) => {
        const question = questions.value[questionIndex];

        if (!question) {
            return;
        }

        result.value[questionIndex] = {
            id: question.id,
            question: question.question,
            type: question.type,
            answer,
        };
    };

    const resetResult = () => {
        result.value = [];
        aiResponse.value = null;
    };

    watch(
        agent,
        (agentType) => {
            questions.value = questionMap[agentType] ?? [];
            resetResult();
        },
        { immediate: true }
    );

    return {
        agent,
        questions,
        result,
        answeredCount,
        surveyProgress,
        aiResponse,
        fetchAiService,
        getMissingQuestionNumbers,
        setResult,
        resetResult,
    };
});
