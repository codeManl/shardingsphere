/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.test.sql.parser.parameterized.engine;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sql.parser.api.CacheOption;
import org.apache.shardingsphere.sql.parser.api.SQLParserEngine;
import org.apache.shardingsphere.sql.parser.api.SQLVisitorEngine;
import org.apache.shardingsphere.sql.parser.core.ParseASTNode;
import org.apache.shardingsphere.sql.parser.exception.SQLASTVisitorException;
import org.apache.shardingsphere.sql.parser.exception.SQLParsingException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public abstract class DynamicLoadingSQLParserParameterizedTest {
    
    private final String sqlCaseId;
    
    private final String sql;
    
    private final String databaseType;
    
    protected static Collection<Object[]> getTestParameters(final String sqlCaseURL) {
        Collection<Object[]> result = new LinkedList<>();
        for (Map<String, String> each : getResponse(sqlCaseURL)) {
            result.addAll(getSQLCases(each));
        }
        return result;
    }
    
    private static Collection<Map<String, String>> getResponse(final String sqlCaseURL) {
        Collection<Map<String, String>> result = new LinkedList<>();
        String[] patches = sqlCaseURL.split("/", 8);
        String casesOwner = patches[3];
        String casesRepo = patches[4];
        String casesDirectory = patches[7];
        String casesGitHubApiURL = "https://api.github.com/repos/" + casesOwner + "/" + casesRepo + "/contents/" + casesDirectory;
        String casesGitHubApiContent = getContent(casesGitHubApiURL);
        if (casesGitHubApiContent.isEmpty()) {
            result.add(ImmutableMap.of("name", "null", "download_url", "null"));
            return result;
        }
        List<String> casesName = JsonPath.parse(casesGitHubApiContent).read("$..name");
        List<String> casesDownloadURL = JsonPath.parse(casesGitHubApiContent).read("$..download_url");
        List<String> casesHtmlURL = JsonPath.parse(casesGitHubApiContent).read("$..html_url");
        IntStream.range(0, JsonPath.parse(casesGitHubApiContent).read("$.length()"))
                .forEach(each -> {
                    String eachName = casesName.get(each);
                    if (eachName.endsWith(".sql") || eachName.endsWith(".test")) {
                        result.add(ImmutableMap.of("name", eachName, "download_url", casesDownloadURL.get(each)));
                    } else if (!eachName.contains(".")) {
                        result.addAll(getResponse(casesHtmlURL.get(each)));
                    }
                });
        return result;
    }
    
    private static String getContent(final String url) {
        String result = "";
        try {
            InputStreamReader in = new InputStreamReader(new URL(url).openStream());
            result = new BufferedReader(in).lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException ingore) {
            log.warn("Error: GitHub API rate limit exceeded");
        }
        return result;
    }
    
    private static Collection<Object[]> getSQLCases(final Map<String, String> elements) {
        Collection<Object[]> result = new LinkedList<>();
        String sqlCaseFileName = elements.get("name");
        String[] lines = getContent(elements.get("download_url")).split("\n");
        int sqlCaseEnum = 1;
        for (String each : lines) {
            if (!each.isEmpty() && Character.isLetter(each.charAt(0)) && each.charAt(each.length() - 1) == ';') {
                String sqlCaseId = sqlCaseFileName.split("\\.")[0] + sqlCaseEnum;
                result.add(new Object[]{sqlCaseId, each});
                sqlCaseEnum++;
            }
        }
        return result;
    }
    
    @Ignore
    @Test
    public final void assertParseSQL() {
        try {
            ParseASTNode parseASTNode = new SQLParserEngine(databaseType, new CacheOption(128, 1024L)).parse(sql, false);
            new SQLVisitorEngine(databaseType, "STATEMENT", true, new Properties()).visit(parseASTNode);
        } catch (final SQLParsingException | ClassCastException | NullPointerException | SQLASTVisitorException | NumberFormatException | StringIndexOutOfBoundsException ignore) {
            log.warn("ParserError: " + sqlCaseId + " value: " + sql + " db-type: " + databaseType);
        }
    }
}
