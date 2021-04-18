package com.qrapids.backlog_jira.data;

public class QualityRequirement {
    private String issue_summary;
    private String issue_description;
    private String issue_type;
    private String project_id;
    private String decision_rationale;
    private String due_date;
    private String priority;

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
    public String getIssue_type() {
        return issue_type;
    }
    public void setIssue_type(String issue_type) {
        this.issue_type = issue_type;
    }
    public String getProject_id() {
        return project_id;
    }
    public void setProject_id(String project_id) {
        this.project_id = project_id;
    }
    public String getDue_date(){return due_date; };
    public void setDue_date(String due_date) { this.due_date = due_date; }
    public String getDecision_rationale() {
        return decision_rationale;
    }
    public String  getPriority() {return priority;};
    public void setPriority(String priority) { this.priority = priority; }
    public void setDecision_rationale(String decision_rationale) {
        this.decision_rationale = decision_rationale;
    }
}
