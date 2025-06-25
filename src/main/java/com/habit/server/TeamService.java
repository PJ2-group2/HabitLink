package com.habit.server;

import com.habit.domain.Team;
import com.habit.domain.TeamMode;
import com.habit.domain.User;
import java.util.List;

public class TeamService {
    private TeamRepository teamRepository;
    private UserRepository userRepository;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    public Team createTeam(String creatorId, String teamName, TeamMode mode) {
        // チームID生成は省略
        Team team = new Team(teamName, creatorId, mode);
        team.setteamName(teamName);
        teamRepository.save(team);
        return team;
    }

    public Team joinTeam(String userId, String teamID) {
        Team team = teamRepository.findById(teamID);
        if (team != null) {
            team.addMember(userId);
            teamRepository.addMember(teamID, userId);
        }
        return team;
    }

    public List<Team> getPublicTeams() {
        return teamRepository.findAllPublicTeams();
    }

    public Team getTeamById(String teamID) {
        return teamRepository.findById(teamID);
    }

    public void updateTeamMembers(String teamID, List<User> members) {
        // メンバー更新ロジックは省略
    }
}