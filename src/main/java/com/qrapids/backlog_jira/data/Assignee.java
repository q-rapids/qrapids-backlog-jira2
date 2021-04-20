package com.qrapids.backlog_jira.data;

public class Assignee {
    String name;
    String id;

    public String getName(){
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
