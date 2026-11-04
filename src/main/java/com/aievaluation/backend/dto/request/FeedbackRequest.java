package com.aievaluation.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FeedbackRequest {

    @NotNull(message = "useful is required (true or false)")
    private Boolean useful;

    @Size(max = 500, message = "Comment must be at most 500 characters")
    private String comment;

    public Boolean getUseful() { return useful; }
    public void setUseful(Boolean useful) { this.useful = useful; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}