public class SuccessResponse {
    private String issue_id;
    private String issue_url;

    public SuccessResponse(String issue_id,String issue_url) {
        this.issue_id = issue_id;
        this.issue_url = issue_url;
    }

    public String getIssue_id() {
        return issue_id;
    }
    public void setIssue_id(String issue_id) {
        this.issue_id = issue_id;
    }
    public String getIssue_url() {
        return issue_url;
    }
    public void setIssue_url(String issue_url) {
        this.issue_url = issue_url;
    }

}
