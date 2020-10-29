package com.botlaneranking.www.BotLaneRankingBackend.acceptance;

import com.botlaneranking.www.BotLaneRankingBackend.controllers.SummonerController;
import com.botlaneranking.www.BotLaneRankingBackend.controllers.pojo.matches.matchlist.Match;
import com.botlaneranking.www.BotLaneRankingBackend.controllers.responses.SummonerResponse;
import com.botlaneranking.www.BotLaneRankingBackend.support.RequestWithSummonerName;
import com.botlaneranking.www.BotLaneRankingBackend.support.TestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.botlaneranking.www.BotLaneRankingBackend.support.SummonerBuilder.aDefaultSummoner;
import static com.botlaneranking.www.BotLaneRankingBackend.support.riot.summerV4.MatchBuilder.aDefaultMatch;
import static com.botlaneranking.www.BotLaneRankingBackend.support.riot.summerV4.MatchListResponseBuilder.aDefaultMatchListResponse;
import static com.botlaneranking.www.BotLaneRankingBackend.support.riot.summerV4.detailedmatch.DetailedMatchBuilder.aDefaultDetailedMatch;
import static com.botlaneranking.www.BotLaneRankingBackend.support.riot.summerV4.detailedmatch.ParticipantBuilder.aDefaultParticipant;
import static com.botlaneranking.www.BotLaneRankingBackend.support.riot.summerV4.detailedmatch.StatsBuilder.aDefaultStatsBuilder;
import static com.botlaneranking.www.BotLaneRankingBackend.support.riot.summerV4.detailedmatch.TimeLineBuilder.aDefaultTimeLine;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SummonerController.class)
public class UpdateSummonerTest extends TestSupport {

    @Autowired
    private MockMvc mockMvc;

    private static final String UPDATE = "/update";

    @Test
    void onlyUpdateMatchesWhereTheSummonerPlayedAdc() throws Exception {
        givenTheDatabaseContains(aDefaultSummoner()
                .withSummonerName(SUMMONER_NAME)
                .withAccountId(ENCRYPTED_ACCOUNT_ID)
                .build());

        stubFor(get(urlEqualTo(format("/lol/match/v4/matchlists/by-account/%s?queue=420&endIndex=100&beginIndex=0&api_key=%s", ENCRYPTED_ACCOUNT_ID, API_KEY)))
                .withHeader("X-Riot-Token", matching(API_KEY)).willReturn(
                        aResponse().withStatus(200)
                                .withBody(gson.toJson(aDefaultMatchListResponse()
                                        .withMatches(asList(
                                                aDefaultMatch()
                                                        .withGameId("1")
                                                        .withRole("SOLO")
                                                        .withChampion("20")
                                                        .withLane("TOP")
                                                        .build(),
                                                aDefaultMatch()
                                                        .withGameId("2")
                                                        .withRole("DUO_CARRY")
                                                        .withChampion("50")
                                                        .withLane("BOTTOM")
                                                        .build()))
                                        .build()
                                ))
                ));

        stubFor(get(urlEqualTo(format("/lol/match/v4/matches/%s?api_key=%s", "2", API_KEY)))
                .withHeader("X-Riot-Token", matching(API_KEY)).willReturn(
                        aResponse().withStatus(200)
                                .withBody(gson.toJson(aDefaultDetailedMatch()
                                        .withGameId("2")
                                        .withParticipantList(
                                                asList(aDefaultParticipant()
                                                                .withChampionId("50")
                                                                .withTeamId("200")
                                                                .withTimeLine(aDefaultTimeLine()
                                                                        .withRole("DUO_CARRY")
                                                                        .withLane("BOTTOM")
                                                                        .build())
                                                                .withStats(aDefaultStatsBuilder()
                                                                        .withWin("true")
                                                                        .build())
                                                                .build(),
                                                        aDefaultParticipant()
                                                                .withChampionId("40")
                                                                .withTeamId("200")
                                                                .withTimeLine(aDefaultTimeLine()
                                                                        .withRole("DUO_SUPPORT")
                                                                        .withLane("NONE")
                                                                        .build())
                                                                .withStats(aDefaultStatsBuilder()
                                                                        .withWin("true")
                                                                        .build())
                                                                .build(),
                                                        aDefaultParticipant()
                                                                .withChampionId("30")
                                                                .withTeamId("100")
                                                                .withTimeLine(aDefaultTimeLine()
                                                                        .withRole("DUO_SUPPORT")
                                                                        .withLane("NONE")
                                                                        .build())
                                                                .withStats(aDefaultStatsBuilder()
                                                                        .withWin("false")
                                                                        .build())
                                                                .build()))
                                        .build()))));

        String jsonString = mockMvc.perform(MockMvcRequestBuilders
                .post(UPDATE)
                .content(gson.toJson(new RequestWithSummonerName(SUMMONER_NAME)))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        SummonerResponse summonerResponse = gson.fromJson(jsonString, SummonerResponse.class);
        assertNotNull(summonerResponse.getChampions());
        assertThat(summonerResponse.getChampions().size(), is(1));
        assertThat(summonerResponse.getChampions().get("Swain").getSupports().get("Janna").getWins(), is("1"));
        assertThat(summonerResponse.getChampions().get("Swain").getSupports().get("Janna").getLosses(), is("0"));
    }

    @Test
    void increaseIndexBy100WhenMaxEachIndexForCurrentSearchIsReached() throws Exception {
        givenTheDatabaseContains(aDefaultSummoner()
                .withSummonerName(SUMMONER_NAME)
                .withAccountId(ENCRYPTED_ACCOUNT_ID)
                .build());

        List<Match> matchList = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            matchList.add(
                    aDefaultMatch()
                            .withGameId(String.valueOf(i))
                            .withRole("DUO_CARRY")
                            .withChampion("50")
                            .withLane("BOTTOM")
                            .build()
            );
        }

        stubFor(get(urlEqualTo(format("/lol/match/v4/matchlists/by-account/%s?queue=420&endIndex=100&beginIndex=0&api_key=%s", ENCRYPTED_ACCOUNT_ID, API_KEY)))
                .withHeader("X-Riot-Token", matching(API_KEY)).willReturn(
                        aResponse().withStatus(200)
                                .withBody(gson.toJson(aDefaultMatchListResponse()
                                        .withMatches(matchList)
                                        .build()
                                ))
                ));

        stubFor(get(urlEqualTo(format("/lol/match/v4/matchlists/by-account/%s?queue=420&endIndex=200&beginIndex=100&api_key=%s", ENCRYPTED_ACCOUNT_ID, API_KEY)))
                .withHeader("X-Riot-Token", matching(API_KEY)).willReturn(
                        aResponse().withStatus(200)
                                .withBody(gson.toJson(aDefaultMatchListResponse()
                                        .withMatches(Collections.singletonList(
                                                aDefaultMatch()
                                                        .withGameId("101")
                                                        .withRole("DUO_CARRY")
                                                        .withChampion("50")
                                                        .withLane("BOTTOM")
                                                        .build()))
                                        .withStartIndex(100)
                                        .withEndIndex(200)
                                        .build()
                                ))
                ));

        for (int i = 1; i <= 101; i++) {
            stubFor(get(urlEqualTo(format("/lol/match/v4/matches/%s?api_key=%s", i, API_KEY)))
                    .withHeader("X-Riot-Token", matching(API_KEY)).willReturn(
                            aResponse().withStatus(200)
                                    .withBody(gson.toJson(aDefaultDetailedMatch()
                                            .withGameId(String.valueOf(i))
                                            .withParticipantList(
                                                    asList(aDefaultParticipant()
                                                                    .withChampionId("50")
                                                                    .withTeamId("200")
                                                                    .withTimeLine(aDefaultTimeLine()
                                                                            .withRole("DUO_CARRY")
                                                                            .withLane("BOTTOM")
                                                                            .build())
                                                                    .withStats(aDefaultStatsBuilder()
                                                                            .withWin("true")
                                                                            .build())
                                                                    .build(),
                                                            aDefaultParticipant()
                                                                    .withChampionId("40")
                                                                    .withTeamId("200")
                                                                    .withTimeLine(aDefaultTimeLine()
                                                                            .withRole("DUO_SUPPORT")
                                                                            .withLane("NONE")
                                                                            .build())
                                                                    .withStats(aDefaultStatsBuilder()
                                                                            .withWin("true")
                                                                            .build())
                                                                    .build(),
                                                            aDefaultParticipant()
                                                                    .withChampionId("30")
                                                                    .withTeamId("100")
                                                                    .withTimeLine(aDefaultTimeLine()
                                                                            .withRole("DUO_SUPPORT")
                                                                            .withLane("NONE")
                                                                            .build())
                                                                    .withStats(aDefaultStatsBuilder()
                                                                            .withWin("false")
                                                                            .build())
                                                                    .build()))
                                            .build()))));
        }

        String jsonString = mockMvc.perform(MockMvcRequestBuilders
                .post(UPDATE)
                .content(gson.toJson(new RequestWithSummonerName(SUMMONER_NAME)))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Mockito.verify(riotApiClient, times(101)).getIndividualMatch(any());
        Mockito.verify(riotApiClient, times(2)).getMatchListFor(anyString(), anyInt(), anyInt());
        Mockito.verify(riotApiClient, never()).getSummonerBySummonerName(any());
        Mockito.verify(dao, times(1)).updateChampions(any());
        Mockito.verify(dao, times(1)).getUserBySummonerName(any());

        SummonerResponse summonerResponse = gson.fromJson(jsonString, SummonerResponse.class);
        assertNotNull(summonerResponse.getChampions());
        assertThat(summonerResponse.getChampions().size(), is(1));
        assertThat(summonerResponse.getChampions().get("Swain").getSupports().get("Janna").getWins(), is("101"));
        assertThat(summonerResponse.getChampions().get("Swain").getSupports().get("Janna").getLosses(), is("0"));
    }
}