package com.accel.api.ai;

import java.util.List;
import java.util.stream.Collectors;

public class AiRequest {

    private String message;
    private String msg;
    private List<SurveyAnswer> answers;

    public AiRequest() {
    }

    public AiRequest(List<SurveyAnswer> answers) {
        this.answers = answers;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<SurveyAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<SurveyAnswer> answers) {
        this.answers = answers;
    }

    public String message() {
        if (message != null) {
            return message;
        }

        if (msg != null) {
            return msg;
        }

        if (answers == null || answers.isEmpty()) {
            return null;
        }

        return answers.stream()
                .map(SurveyAnswer::toPromptText)
                .collect(Collectors.joining("\n"));
    }

    public static class SurveyAnswer {
        private int id;
        private String question;
        private String type;
        private Object answer;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getAnswer() {
            return answer;
        }

        public void setAnswer(Object answer) {
            this.answer = answer;
        }

        private String toPromptText() {
            return "%d. 질문: %s / 답변: %s".formatted(id, question, answer);
        }
    }
}
