package com.qrapids.backlog_jira.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qrapids.backlog_jira.data.*;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;
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
                    JsonObject object = data.get(i).getAsJsonObject(); //primer elemento de milestones
                    JsonObject aux = object.getAsJsonObject().get("fields").getAsJsonObject(); //obtencion campos del objeto
                    System.out.println(aux.toString());
                    System.out.println(aux.get("duedate").toString());
                    if (!aux.get("duedate").isJsonNull()) { // check if milestone have due_date
                        System.out.println("DUE DATE NOT NULL");
                        String date = aux.get("duedate").getAsString();
                        if(date_from != null && !date_from.isEmpty()) {
                            System.out.println("not null");
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
                                            @RequestParam(value = "date_from", required = false) String date_from) throws ParseException {
        ResponseEntity<Object> milestonesList = getMilestones(project_id,date_from);
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
                    // put milestone phase to the list
                    Phase firstPhase = new Phase();
                    LocalDate date = LocalDate.parse(milestones.get(i).getDate()); // milestone date
                    firstPhase.setDateFrom(date.minusWeeks(1).toString());
                    firstPhase.setDateTo(date.toString());
                    firstPhase.setName("");
                    firstPhase.setDescription("");
                    phases.add(firstPhase);
                    // add others phases to the list
                    for (int j = 1; j < 10; ++j) {
                        Phase newPhase = new Phase();
                        newPhase.setDateFrom(date.minusWeeks(j + 1).toString());
                        newPhase.setDateTo(date.minusWeeks(j).toString());
                        newPhase.setName("");
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
            fieldsvar.put("duedate", requirement.getDue_date());
                //Assignee hardcoded
            JSONObject assignee = new JSONObject();
            assignee.put("accountId", requirement.getAssignee().getId());
            fieldsvar.put("assignee", assignee);
            if(requirement.getPriority() != null) {
                JSONObject objPriority = new JSONObject();
                objPriority.put("name", requirement.getPriority());
                fieldsvar.put("priority", objPriority);
            }
            //put sprint on issue
            JSONObject sprint = new JSONObject();
            System.out.println(requirement.getSprint().getId());
            sprint.put("name", requirement.getSprint().getName());
            sprint.put("id", requirement.getSprint().getId());
            sprint.put("boardId", getBoardId(requirement.getProject_id()));

            fieldsvar.put("customfield_10020",Integer.valueOf(requirement.getSprint().getId()));

            JSONObject issuetype = new JSONObject();
            issuetype.put("name", requirement.getIssue_type());
            fieldsvar.put("issuetype", issuetype);
            //final JSON
            JSONObject fields = new JSONObject();
            fields.put("fields", fieldsvar);

            //POST METHOD JIRA
            WebResource webResource = client.resource("https://ariadnavinets.atlassian.net/rest/api/2/issue");
            response = webResource.header(headerAuthorization, headerAuthorizationValue).type(headerType).accept(headerType).post(ClientResponse.class, fields.toString());
            int statusCode = response.getStatus();
            System.out.println(statusCode);


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
                    System.out.println(resp.toString());

                    JsonParser parser = new JsonParser();
                    JsonObject object = parser.parse(resp.toString()).getAsJsonObject();

                    System.out.println(object);
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




