package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Quiz Header class encapsulates all of the quiz data for the identity proof test
 * Created by pmacdowell on 2017-08-04.
 */
public class QuizObject  extends TopLevelObj {

    @SerializedName("transactionID")
    private String transactionId;
    @SerializedName("responseUniqueId")
    private String responseUniqueId;
    @SerializedName("quizId")
    private String quizId;
    @SerializedName("numQuestions")
    private String numQuestions;
    @SerializedName("errorDescription")
    private String errorDescription;
    @SerializedName("question")
    private List<QuizQuestion> quizQuestions;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getResponseUniqueId() {
        return responseUniqueId;
    }

    public void setResponseUniqueId(String responseUniqueId) {
        this.responseUniqueId = responseUniqueId;
    }

    public String getQuizId() {
        return quizId;
    }

    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }

    public String getNumQuestions() {
        return numQuestions;
    }

    public void setNumQuestions(String numQuestions) {
        this.numQuestions = numQuestions;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public List<QuizQuestion> getQuizQuestions() {
        return quizQuestions;
    }

    public void setQuizQuestions(List<QuizQuestion> quizQuestions) {
        this.quizQuestions = quizQuestions;
    }

    public static class QuizQuestion {
        @SerializedName("nsquestionId")
        private String questionId;
        @SerializedName("nssequenceId")
        private String sequenceId;
        @SerializedName("nseq")
        private String eq;
        @SerializedName("type")
        private String type;
        @SerializedName("text")
        private String text;
        @SerializedName("choice")
        private Choice[] choice;

        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
        }

        public String getSequenceId() {
            return sequenceId;
        }

        public void setSequenceId(String sequenceId) {
            this.sequenceId = sequenceId;
        }

        public String getEq() {
            return eq;
        }

        public void setEq(String eq) {
            this.eq = eq;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Choice[] getChoice() {
            return choice;
        }

        public void setChoice(Choice[] choice) {
            this.choice = choice;
        }
    }

    public static class Choice {
        @SerializedName("nschoiceId")
        private String choiceId;
        @SerializedName("nssequenceId")
        private String sequenceId;
        @SerializedName("text")
        private String text;

        public String getChoiceId() {
            return choiceId;
        }

        public void setChoiceId(String choiceId) {
            this.choiceId = choiceId;
        }

        public String getSequenceId() {
            return sequenceId;
        }

        public void setSequenceId(String sequenceId) {
            this.sequenceId = sequenceId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
