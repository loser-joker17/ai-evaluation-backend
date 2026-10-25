package com.aievaluation.backend.enums;

/** UPSC marks and the word limit that goes with them. The student never types the word limit. */
public enum MarksOption {

    TEN(10, 150),
    FIFTEEN(15, 250),
    TWENTY(20, 250);

    private final int marks;
    private final int wordLimit;

    MarksOption(int marks, int wordLimit) {
        this.marks = marks;
        this.wordLimit = wordLimit;
    }

    public int getMarks() { return marks; }

    public int getWordLimit() { return wordLimit; }

    /** Returns null when the marks are not 10, 15 or 20. */
    public static MarksOption fromMarks(int marks) {
        for (MarksOption option : values()) {
            if (option.marks == marks) {
                return option;
            }
        }
        return null;
    }

    /** GS1-GS3: 10 or 15 marks. GS4: 10, or 20 for case studies. */
    public boolean isAllowedFor(Paper paper) {
        if (this == TWENTY) {
            return paper == Paper.GS4;
        }
        if (this == FIFTEEN) {
            return paper != Paper.GS4;
        }
        return true;
    }
}