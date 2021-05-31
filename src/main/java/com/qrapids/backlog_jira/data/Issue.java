package com.qrapids.backlog_jira.data;

import java.util.List;

public class Issue {
    private String issue_summary;
    private String issue_description;
    private String issue_id;
    private List<String> acceptance_criteria;

    public Issue() {
    }

    public String getIssue_summary() {
        return issue_summary;
    }

    public void setIssue_summary(String issue_summary) {
        this.issue_summary = issue_summary;
    }

    public String getIssue_description() {
        return issue_description;
    }

    public void setIssue_description(String issue_description) {
        this.issue_description = issue_description;
    }

    public String getIssue_id() {
        return issue_id;
    }

    public void setIssue_id(String issue_id) {
        this.issue_id = issue_id;
    }


    public List<String> getAcceptance_criteria() {
        return acceptance_criteria;
    }

    public void setAcceptance_criteria(List<String> acceptance_criteria) {
        this.acceptance_criteria = acceptance_criteria;
    }
}
