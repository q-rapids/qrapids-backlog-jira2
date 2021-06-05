package com.qrapids.backlog_jira.services;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qrapids.backlog_jira.data.*;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
public class BacklogService {

    @Value("${jira.url}")
    private String jiraURL;

    @Value("${jira.secret}")
    private String token;

    @GetMapping("/api/milestones")
    public ResponseEntity<Object> getMilestones(@RequestParam String project_id,
                                                @RequestParam(value = "date_from", required = false) String date_from) {
        try {

            //Creating the request authentication by username and apiKey
            ClientResponse con;
            String auth = new String(Base64.encode(jiraURL + ":" + token ));
            final String headerAuthorization = "Authorization";
            final String headerAuthorizationValue = "Basic " + auth;
            final String headerType = "application/json";
            Client client = Client.create();

            //GET CALL
            WebResource webResource = client.resource("https://ariadnavinets.atlassian.net/rest/api/2/search?jql=project%3D" + project_id + "%20AND%20issuetype%3DMilestone");
            con = webResource.header(headerAuthorization , headerAuthorizationValue).type(headerType).accept(headerType).get(ClientResponse.class);
            int status = con.getStatus();


            System.out.println(status);
            // Creating a Request with authentication by token
            // Reading the Response
            if (status != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + status);
            } else {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getEntityInputStream(), "utf-8"));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                System.out.println(content.toString());
                in.close();
                con.close();

                JsonParser parser = new JsonParser();
                JsonObject obj = parser.parse(content.toString()).getAsJsonObject();

                JsonArray data = obj.getAsJsonArray("issues");

                int size = obj.getAsJsonObject().get("total").getAsInt();

                List<Milestone> milestones = new ArrayList<>();

                for (int i = 0; i < size; ++i) {
                    JsonObject object = data.get(i).getAsJsonObject(); //first element of milestones
                    JsonObject aux = object.getAsJsonObject().get("fields").getAsJsonObject(); //getting all object fields
                    if (!aux.get("duedate").isJsonNull()) { // check if milestone have due_date
                        String date = aux.get("duedate").getAsString();
                        if(date_from != null && !date_from.isEmpty()) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            Date from = sdf.parse(date_from);
                            Date due = sdf.parse(date);
                            if (due.equals(from) || due.after(from)) { // only add milestones which will finish after date_from
                                Milestone newMilestone = new Milestone();
                                newMilestone.setName(aux.get("summary").getAsString());
                                newMilestone.setDate(date);
                                newMilestone.setDescription(aux.get("description").getAsString());
                                newMilestone.setType("Milestone");
                                System.out.println(newMilestone.toString());
                                milestones.add(newMilestone);
                            }
                        } else { // if there is no date_from specified --> we add all milestones with due_date
                            if(aux.get("description").getAsString().equals("null")) throw new Exception ("Description is null");
                            Milestone newMilestone = new Milestone();
                            newMilestone.setName(aux.get("summary").getAsString());
                            newMilestone.setDate(date);
                            newMilestone.setDescription(aux.get("description").getAsString());
                            newMilestone.setType("Milestone");
                            milestones.add(newMilestone);
                        }
                    }
                }
                Collections.sort(milestones, (Milestone o1, Milestone o2) -> o1.getDate().compareTo(o2.getDate()));
                return new ResponseEntity<>(milestones, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/api/phases")
    public ResponseEntity<Object> getPhases(@RequestParam String project_id,
                                            @RequestParam(value = "date_from", required = false) String date_from,
                                            @RequestParam(value = "num_weeks", required = false) String num_weeks,
                                            @RequestParam(value = "duration_sprint", required = false) String duration_sprint) throws ParseException {
        ResponseEntity<Object> milestonesList = getMilestones(project_id,date_from);
        int numWeeks = 10;
        int duration = 1;
        if (milestonesList.getStatusCode() == HttpStatus.OK) {

            List<Milestone> milestones = (List<Milestone>) milestonesList.getBody();
            List<Phase> phases = new ArrayList<>();
            if (milestones.isEmpty())
                return new ResponseEntity<>(phases, HttpStatus.OK);
            else {
                // get next milestone from today
                Date now = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                int i = 0;
                boolean found = false;
                while (i < milestones.size() && !found) {
                    Date due = sdf.parse(milestones.get(i).getDate());
                    if (due.after(now))
                        found = true;
                    else
                        i++;
                }
                if (found) {

                    if (duration_sprint != null){
                        duration =  Integer.parseInt(duration_sprint);
                    }
                    // put milestone phase to the list
                    Phase firstPhase = new Phase();
                    LocalDate date = LocalDate.parse(milestones.get(i).getDate()); // milestone date
                    firstPhase.setDateFrom(date.minusWeeks(duration).toString());
                    firstPhase.setDateTo(date.toString());
                    firstPhase.setName("Phase " + i);
                    firstPhase.setDescription("");
                    phases.add(firstPhase);
                    // add others phases to the list
                    if (num_weeks != null){
                        numWeeks =  Integer.parseInt(num_weeks);
                    }

                    for (int j = 1; j < numWeeks; ++j) {
                        Phase newPhase = new Phase();
                        newPhase.setDateFrom(date.minusWeeks(j + duration).toString());
                        newPhase.setDateTo(date.minusWeeks(j).toString());
                        newPhase.setName("Phase " + i);
                        newPhase.setDescription("");
                        phases.add(newPhase);
                    }
                }
                return new ResponseEntity<>(phases, HttpStatus.OK);
            }
        } else {
            ErrorResponse error = (ErrorResponse) milestonesList.getBody();
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/api/issues")
    public ResponseEntity<Object> getIssues(@RequestParam String project_id) throws ParseException {
        try {
            //Creating the request authentication by username and apiKey
            ClientResponse con;
            String auth = new String(Base64.encode(jiraURL + ":" + token));
            final String headerAuthorization = "Authorization";
            final String headerAuthorizationValue = "Basic " + auth;
            final String headerType = "application/json";
            Client client = Client.create();

            WebResource webResource = client.resource("https://ariadnavinets.atlassian.net/rest/api/2/search?project=" + project_id);
            con = webResource.header(headerAuthorization, headerAuthorizationValue).type(headerType).accept(headerType).get(ClientResponse.class);
            int status = con.getStatus();
            System.out.println(status);
            if (status != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + status);
            } else {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getEntityInputStream(), "utf-8"));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                con.close();

                List<QualityRequirement> qualityRequirements = new ArrayList<>();

                JsonParser parser = new JsonParser();
                JsonObject obj = parser.parse(content.toString()).getAsJsonObject();
                JsonArray data = obj.getAsJsonArray("issues");
                List<Issue> issues = new ArrayList<>();

                for (int i = 0; i < data.size(); ++i) {
                    JsonObject object = data.get(i).getAsJsonObject(); //primer elemento de milestones
                    Issue newIssue = new Issue();
                    newIssue.setIssue_id(object.get("id").getAsString());

                    JsonObject aux = object.getAsJsonObject().get("fields").getAsJsonObject(); //obtencion campos del objeto
                    newIssue.setIssue_summary(aux.get("summary").getAsString());

                    newIssue.setIssue_description(aux.get("description").isJsonNull() ?  null : aux.get("description").getAsString());
                    System.out.println(newIssue);
                    int size = aux.get("customfield_10029").getAsJsonArray().size();
                    List<String> acc_criteria = null;
                   /* for(int i = 0; i < size; i++) {
                        newIssue.setAcceptance_criteria(aux.get("customfield_10029").getAsJsonArray());
                    }

                    issues.add(newIssue);*/
                }

                return new ResponseEntity<>(issues, HttpStatus.OK);
            }

        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PutMapping("/api/acceptancecriteria")
    public ResponseEntity<Object> putAcceptanceCriteria(@RequestBody Issue issue,
                                                        @RequestParam String acc_criteria) throws ParseException {
        try {
            //Create list of acceptante criterias
            List<String> acc_criteriaList = issue.getAcceptance_criteria();
            //Creating the request authentication by username and apiKey
            ClientResponse con;
            String auth = new String(Base64.encode(jiraURL + ":" + token));
            final String headerAuthorization = "Authorization";
            final String headerAuthorizationValue = "Basic " + auth;
            final String headerType = "application/json";
            Client client = Client.create();
            JSONObject content = new JSONObject();
            JSONObject acc_issue = new JSONObject();
            JSONObject field = new JSONObject();
            JSONObject version = new JSONObject();
            version.put("version", 1);
            version.put("type", "doc");
            JSONArray items = new JSONArray();
            if(acc_criteriaList != null) {
                for (String s : acc_criteriaList) { //creation of the acceptance criteria of the issue to JSON
                    JSONObject paragraph1 = new JSONObject();
                    paragraph1.put("type", "paragraph");
                    JSONObject text1 = new JSONObject();
                    JSONObject type1 = new JSONObject();
                    type1.put("type", "text");
                    type1.put("text", s);
                    JSONArray ja1 = new JSONArray();
                    ja1.put(type1);
                    text1.put("type", "paragraph");
                    text1.put("content", ja1);
                    items.put(text1);
                    }
            }

            JSONObject paragraph = new JSONObject();
            paragraph.put("type", "paragraph");
            JSONObject text = new JSONObject();
            JSONObject type = new JSONObject();
            type.put("type", "text");
            type.put("text", acc_criteria);
            JSONArray ja1 = new JSONArray();
            ja1.put(type);
            text.put("type", "paragraph");
            text.put("content", ja1);
            items.put(text);
            JSONArray ja_content = new JSONArray();
            ja_content.put(content);
            version.put("content", items);
            JSONObject set = new JSONObject();
            set.put("set", version );
            JSONArray ja_set = new JSONArray();
            ja_set.put(set);
            field.put("customfield_10029", ja_set);
            acc_issue.put("update", field);

            WebResource webResource = client.resource("https://ariadnavinets.atlassian.net/rest/api/3/issue/" + issue.getIssue_id());
            con = webResource.header(headerAuthorization, headerAuthorizationValue).type(headerType).accept(headerType).put(ClientResponse.class, acc_issue.toString());
            int status = con.getStatus();
            System.out.println(status);
            if (status != 204) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + status);
            } else {
                if (acc_criteriaList == null) {
                    acc_criteriaList = Collections.singletonList(acc_criteria);
                }
                else {
                    acc_criteriaList.add(acc_criteria);
                }
                issue.setAcceptance_criteria(acc_criteriaList);

                return new ResponseEntity<>(issue, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/api/createIssue")
    public ResponseEntity<Object> createIssue(@RequestBody QualityRequirement requirement) throws IOException {
        try {
            SuccessResponse newIssue = null;
            //Creating the request authentication by username and apiKey
            ClientResponse response;
            String auth = new String(Base64.encode(jiraURL + ":" + token ));
            final String headerAuthorization = "Authorization";
            final String headerAuthorizationValue = "Basic " + auth;
            final String headerType = "application/json";

            Client client = Client.create();
            //fields of project
            JSONObject projectvar = new JSONObject();
            projectvar.put("key", requirement.getProject_id());
            JSONObject fieldsvar = new JSONObject();
            fieldsvar.put("project", projectvar);
            fieldsvar.put("summary", requirement.getIssue_summary());
            fieldsvar.put("description", requirement.getIssue_description());

            if(requirement.getDue_date() != null ) fieldsvar.put("duedate", requirement.getDue_date());

            //Assignee
            if(requirement.getAssignee() != null) {
                JSONObject assignee = new JSONObject();
                assignee.put("accountId", requirement.getAssignee().getId());
                fieldsvar.put("assignee", assignee);
            }

            //Priority
            if(requirement.getPriority() != null) {
                JSONObject objPriority = new JSONObject();
                objPriority.put("name", requirement.getPriority());
                fieldsvar.put("priority", objPriority);
            }

            //Put sprint on issue
            if(requirement.getSprint() != null ) {
            JSONObject sprint = new JSONObject();
            sprint.put("name", requirement.getSprint().getName());
            sprint.put("id", requirement.getSprint().getId());
            sprint.put("boardId", getBoardId(requirement.getProject_id()));
            }

           // fieldsvar.put("customfield_10020",Integer.valueOf(requirement.getSprint().getId()));

            if(requirement.getIssue_type() != null ) {
                JSONObject issuetype = new JSONObject();
                issuetype.put("name", "Story");
                fieldsvar.put("issuetype", issuetype);
            }

            //final JSON
            JSONObject fields = new JSONObject();
            fields.put("fields", fieldsvar);

            //POST METHOD JIRA
            WebResource webResource = client.resource("https://ariadnavinets.atlassian.net/rest/api/2/issue");
            response = webResource.header(headerAuthorization, headerAuthorizationValue).type(headerType).accept(headerType).post(ClientResponse.class, fields.toString());
            int statusCode = response.getStatus();


            if (statusCode != 201) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + statusCode);

            } else {
                try
                        (BufferedReader br = new BufferedReader(
                                new InputStreamReader(response.getEntityInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder resp = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        resp.append(responseLine.trim());
                    }

                    JsonParser parser = new JsonParser();
                    JsonObject object = parser.parse(resp.toString()).getAsJsonObject();

                    newIssue = new SuccessResponse(object.get("id").getAsString(), object.get("self").getAsString());
                    return new ResponseEntity<>(newIssue, HttpStatus.OK);
                }
            }
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/api/assignees")
    public ResponseEntity<Object> getAssignees(@RequestParam String project_id) throws ParseException {
        try {
            //Creating the request authentication by username and apiKey
            ClientResponse con;
            String auth = new String(Base64.encode(jiraURL + ":" + token));
            final String headerAuthorization = "Authorization";
            final String headerAuthorizationValue = "Basic " + auth;
            final String headerType = "application/json";
            Client client = Client.create();

            WebResource webResource = client.resource("https://ariadnavinets.atlassian.net/rest/api/2/user/assignable/search?project=" + project_id);
            con = webResource.header(headerAuthorization, headerAuthorizationValue).type(headerType).accept(headerType).get(ClientResponse.class);
            int status = con.getStatus();
            System.out.println(status);
            if (status != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + status);
            } else {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getEntityInputStream(), "utf-8"));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                System.out.println(content.toString());
                in.close();
                con.close();

                JsonParser parser = new JsonParser();

                JsonArray obj = parser.parse(content.toString()).getAsJsonArray();

                List<Assignee> assignees = new ArrayList<>();
                System.out.println(obj.size());

                for (int i = 0; i < obj.size(); ++i) {
                    JsonObject object = obj.get(i).getAsJsonObject(); //primer elemento de milestones
                    Assignee newAssignee = new Assignee();
                    newAssignee.setId(object.get("accountId").getAsString());
                    newAssignee.setName(object.get("displayName").getAsString());
                    assignees.add(newAssignee);
                }
                return new ResponseEntity<>(assignees, HttpStatus.OK);
            }
        }
        catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/api/sprints")
    public ResponseEntity<Object> getSprints(@RequestParam String project_id) throws ParseException {
        try {
            //Creating the request authentication by username and apiKey
            ClientResponse con;
            String auth = new String(Base64.encode(jiraURL + ":" + token));
            final String headerAuthorization = "Authorization";
            final String headerAuthorizationValue = "Basic " + auth;
            final String headerType = "application/json";
            Client client = Client.create();

            int id = getBoardId(project_id);

            WebResource webResource = client.resource("https://ariadnavinets.atlassian.net/rest/agile/1.0/board/" + id + "/sprint");
            con = webResource.header(headerAuthorization, headerAuthorizationValue).type(headerType).accept(headerType).get(ClientResponse.class);
            int status = con.getStatus();
            System.out.println(status);
            System.out.println(id);
            if (status != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + status);
            } else {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getEntityInputStream(), "utf-8"));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                System.out.println(content.toString());
                in.close();
                con.close();

                JsonParser parser = new JsonParser();
                JsonObject data = parser.parse(content.toString()).getAsJsonObject();

                JsonArray obj = data.getAsJsonArray("values");

                System.out.println(obj.toString());
                List<Sprint> sprints = new ArrayList<>();
                System.out.println(obj.size());

                for (int i = 0; i < obj.size(); ++i) {
                    JsonObject object = obj.get(i).getAsJsonObject(); //primer elemento de milestones
                    Sprint newSprint = new Sprint();
                    newSprint.setId(object.get("id").getAsString());
                    newSprint.setName(object.get("name").getAsString());
                    sprints.add(newSprint);
                }
                return new ResponseEntity<>(sprints, HttpStatus.OK);
            }
        }
        catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private int getBoardId(String project_id) throws IOException {
        int id;
        ClientResponse con;
        String auth = new String(Base64.encode(jiraURL + ":" + token));
        final String headerAuthorization = "Authorization";
        final String headerAuthorizationValue = "Basic " + auth;
        final String headerType = "application/json";
        Client client = Client.create();

        WebResource webResource = client.resource("https://ariadnavinets.atlassian.net/rest/agile/1.0/board?projectKeyOrId=" + project_id);
        con = webResource.header(headerAuthorization, headerAuthorizationValue).type(headerType).accept(headerType).get(ClientResponse.class);
        int status = con.getStatus();
        System.out.println(status);


        if (status != 200) {
            throw new RuntimeException("Failed : HTTP error code : "
                    + status);
        } else {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getEntityInputStream(), "utf-8"));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            System.out.println(content.toString());
            in.close();
            con.close();

            JsonParser parser = new JsonParser();
            JsonObject obj = parser.parse(content.toString()).getAsJsonObject();
            JsonArray data = obj.getAsJsonArray("values");

                JsonObject object = data.get(0).getAsJsonObject();
                id = object.get("id").getAsInt();
            }
            return id;
        }
}




