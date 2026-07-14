package com.abdul.relay.service;

import com.abdul.relay.dto.ProjectRequestDTO;
import com.abdul.relay.entity.Project;
import com.abdul.relay.entity.User;

import java.util.List;

public interface ProjectService {
    List<Project> getProjectsByUser(User user);
    void createProject(ProjectRequestDTO projectRequestDTO, User user) throws Exception;
    Project getProjectBySlugAndUser(String slug, User user);
    String getRelayTokenProject(String userId, String projectName);
    void deleteProject(String slug, User user);
    void changeStatusProject(String slug, User user);
    void generateTokenProject(String slug, User user);
    Boolean isTokenValid(String relayToken, String slug, String userId);
}
