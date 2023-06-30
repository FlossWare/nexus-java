package org.flossware.nexus;

import java.util.LinkedList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Nexus {

    @Autowired
    Credentials creds;

    void queryRepoItems(final JSONArray items, final List<RepoRecord> repoRecords) {
        for (int item = 0; item < items.length(); item++) {
            final String id = items.getJSONObject(item).getString("id");
            final JSONArray assets = items.getJSONObject(item).getJSONArray("assets");

            repoRecords.add(new RepoRecord(id, assets.getJSONObject(0).getInt("fileSize"), assets.getJSONObject(0).getString("path")));
        }
    }

    String queryRepoForUrl(final RestTemplate rest, final String url, final List<RepoRecord> repoRecords) {
        System.out.println("URL:  " + url);

        final JSONObject repoJson = new JSONObject(rest.getForObject(url, String.class));

        JSONArray items = repoJson.getJSONArray("items");

        for (int item = 0; item < items.length(); item++) {
            final String id = items.getJSONObject(item).getString("id");
            final JSONArray assets = items.getJSONObject(item).getJSONArray("assets");

            repoRecords.add(new RepoRecord(id, assets.getJSONObject(0).getInt("fileSize"), assets.getJSONObject(0).getString("path")));
        }

        return repoJson.isNull("continuationToken") ? null : repoJson.getString("continuationToken");
    }

    List<RepoRecord> queryRepo(final RestTemplate rest, final String repo) {
        final String baseUrl = creds.getUrl() + "/service/rest/v1/components?repository=" + repo;
        String url = baseUrl;

        String continuousToken = "";

        List<RepoRecord> repoRecords = new LinkedList<>();

        while(continuousToken != null) {
            continuousToken = queryRepoForUrl(rest, url, repoRecords);

            url = baseUrl + "&continuationToken=" + continuousToken;
        }

        return repoRecords;
    }

    void list(final RestTemplate rest, final String repo) {
        final List<RepoRecord> repoRecords = queryRepo(rest, repo);

        long fileSize = 0;

        for (RepoRecord repoRecord : repoRecords) {
            System.out.printf("%s  %15d  %s\n", repoRecord.id(), repoRecord.fileSize(), repoRecord.path());

            fileSize += repoRecord.fileSize();
        }

        System.out.println("\n\nTotal components:  " + repoRecords.size());
        System.out.println("Total size:        " + fileSize + "\n\n");
    }

    void listWithFilter(final RestTemplate rest, final String repo, final String regEx) {
        final List<RepoRecord> repoRecords = queryRepo(rest, repo);

        int matches = 0;
        long fileSize = 0;

        for (RepoRecord repoRecord : repoRecords) {
            if (repoRecord.path().matches(regEx)) {
                System.out.printf("%s  %15d  %s\n", repoRecord.id(), repoRecord.fileSize(), repoRecord.path());
                matches++;

                fileSize += repoRecord.fileSize();
            }
        }


        System.out.println("\n\nTotal components:  " + repoRecords.size());
        System.out.println("Matches:           " + matches);
        System.out.println("Total size:        " + fileSize + "\n\n");
    }

    void delete(final RestTemplate rest, final String repo) {
        final String baseUrl = creds.getUrl() + "/service/rest/v1/components/";

        int total = 0;
        long fileSize = 0;

        for (RepoRecord repoRecord : queryRepo(rest, repo)) {
            System.out.println("Deleting [" + repoRecord.path() + "] -> " + baseUrl + repoRecord.id());
            rest.delete(baseUrl + repoRecord.id());

            total++;
            fileSize += repoRecord.fileSize();
        }

        System.out.println("\n\nTotal deleted:  " + total);
        System.out.println("Total size:    " + fileSize + "\n\n");
    }

    void deleteWithFilter(final RestTemplate rest, final String repo, final String regEx) {
        final String baseUrl = creds.getUrl() + "/service/rest/v1/components/";

        int total = 0;
        long fileSize = 0;

        for (RepoRecord repoRecord : queryRepo(rest, repo)) {
            if (repoRecord.path().matches(regEx)) {
                System.out.println("Deleting [" + repoRecord.path() + "]");
                rest.delete(baseUrl + repoRecord.id());

                total++;
                fileSize += repoRecord.fileSize();
            }
        }

        System.out.println("\n\nTotal deleted: " + total);
        System.out.println("Total size:    " + fileSize + "\n\n");
    }

    @Bean
    public RestTemplate restTemplate(final RestTemplateBuilder builder) {
            return builder.basicAuthentication(creds.getUser(), creds.getPassword()).build();
    }

    @Bean
    public CommandLineRunner run(final RestTemplate restTemplate) throws Exception {
            return args -> {
                if (0 == args.length) {
                    System.err.println("Provide command line args!");
                    System.exit(1);
                }

                switch (args[0]) {
                    case "list":
                        System.out.println("Listing...");

                        if (2 == args.length) {
                            list(restTemplate, args[1]);
                        } else {
                            listWithFilter(restTemplate, args[1], args[2]);
                        }

                        break;

                    case "delete":
                        System.out.println("Deleting...");

                        if (2 == args.length) {
                            delete(restTemplate, args[1]);
                        } else {
                            deleteWithFilter(restTemplate, args[1], args[2]);
                        }

                        break;

                    default:
                        ;
                }
            };
    }

    public static void main(final String[] args) {
        SpringApplication.run(Nexus.class, args);
    }
}
