package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

/**
 * The answers chosen by a user to be sent back on the verifyQuiz endpoint
 * Created by pmacdowell on 2017-08-03.
 */

public class VerifyQuizObj  {

    @SerializedName("accessCode")
    private String accessCode;
    @SerializedName("quizId")
    private String quizId;
    @SerializedName("transactionID")
    private String transactionID;
    @SerializedName("responseUniqueId")
    private String responseUniqueId;
    @SerializedName("answers")
    private Answer[] answers;

    protected String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    protected String getQuizId() {
        return quizId;
    }

    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }


    protected String getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    protected String getResponseUniqueId() {
        return responseUniqueId;
    }

    public void setResponseUniqueId(String responseUniqueId) {
        this.responseUniqueId = responseUniqueId;
    }

    protected Answer[] getAnswers() {
        return answers;
    }

    public void setAnswers(Answer[] answers) {
        this.answers = answers;
    }

    public static class Answer{
        @SerializedName("questionId")
        private String questionId;
        @SerializedName("choiceId")
        private String choiceId;

        protected String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
        }

        protected String getChoiceId() {
            return choiceId;
        }

        public void setChoiceId(String choiceId) {
            this.choiceId = choiceId;
        }
    }
}
