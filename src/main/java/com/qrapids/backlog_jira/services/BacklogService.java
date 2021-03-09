package com.qrapids.backlog_jira.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qrapids.backlog_jira.data.ErrorResponse;
import com.qrapids.backlog_jira.data.QualityRequirement;
import com.qrapids.backlog_jira.data.SuccessResponse;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class BacklogService {

    @PostMapping("/api/createIssue")
    public ResponseEntity<Object> createIssue(@RequestBody QualityRequirement requirement) throws IOException {
        try {
            SuccessResponse newIssue = null;
            //Creating the request authentication by username and apiKey
            ClientResponse response;
            String auth = new String(Base64.encode("ariadna.vinets@estudiantat.upc.edu" + ":" + "BaDD9uM0B6VtdOWuwRkQ3C2B"));
            final String headerAuthorization = "Authorization";
            final String headerAuthorizationValue = "Basic " + auth;
            final String headerType = "application/json";

            Client client = Client.create();

            String data1 = "{\"fields\":{\"project\":{\"key\":\"TP\"},\"summary\":\"REST Test\",\"description\": \"Creating of an issue using project keys and issue type names using the REST API\",\"issuetype\":{\"name\":\"Bug\"}}}";

            //Creating all the data with the requirement
            String data = "{\"fields\":{\"project\":{\"key\":\"" + requirement.getProject_id() + "\""
                    + "},\"summary\":\"" + requirement.getIssue_summary() + "\""
                    + ",\"description\":\" " + requirement.getIssue_description() + "\"" + ",\"issuetype\":{\"name\":\""
                    + requirement.getIssue_type() + "\"" + "}}}";

            //POST METHOD JIRA
            WebResource webResource = client.resource("https://ariadnavinets.atlassian.net/rest/api/2/issue");
            response = webResource.header(headerAuthorization, headerAuthorizationValue).type(headerType).accept(headerType).post(ClientResponse.class, data);
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
}



