package com.abdul.relay.service.impl;

import com.abdul.relay.dto.ProjectRequestDTO;
import com.abdul.relay.entity.Project;
import com.abdul.relay.entity.User;
import com.abdul.relay.repository.ProjectRepository;
import com.abdul.relay.service.JwtService;
import com.abdul.relay.service.ProjectService;
import com.abdul.relay.service.RedisService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectServiceImpl implements ProjectService {
    @Autowired
    private final ProjectRepository projectRepository;
    @Value("${relay.jwt-expiration-in-day-access-token}")
    private Integer TIME_EXPIRED_DAY;

    private final JwtService jwtService;
    private final RedisService redisService;


    public ProjectServiceImpl(ProjectRepository projectRepository, JwtService jwtService, RedisService redisService) {
        this.projectRepository = projectRepository;
        this.jwtService = jwtService;
        this.redisService = redisService;
    }

    @Override
    public List<Project> getProjectsByUser(User user) {
        return projectRepository.findProjectsByUserAndIsDeleted(user, false);
    }

    public Boolean isProjectExistByNameAndUser(String name, User user){
        return projectRepository.existsProjectByNameAndUserAndIsDeleted(name, user, false);
    }

    @Override
    @Transactional
    public void createProject(ProjectRequestDTO projectRequestDTO, User user) throws Exception {

        if(isProjectExistByNameAndUser(projectRequestDTO.getName(), user)){
            throw new IllegalArgumentException("Project sudah terdaftar");
        }
        try {
            Project project = mapProjectDtoToProject(projectRequestDTO);
            project.setUser(user);
            projectRepository.save(project);
            String projectToken = jwtService.generateToken(user);
            String keyRedis = user.getId().toString() + ":"+project.getPublicUrl();
            Duration duration = Duration.ofDays(TIME_EXPIRED_DAY);
            redisService.save(keyRedis, projectToken, duration);
        }catch (Exception e){
            e.getMessage();
            throw new Exception("Create Project Gagal");
        }
    }
    // ProjectService
    public Project getProjectBySlugAndUser(String slug, User user) {
        return projectRepository.findProjectByPublicUrlAndUserAndIsDeleted(slug, user, false)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project tidak ditemukan"));
    }
    public String getRelayTokenProject(String userId, String projectName){
        String keyRedis = userId + ":"+projectName;
        return redisService.get(keyRedis);
    }

    @Override
    public void deleteProject(String slug, User user) {
        Project project = projectRepository.findProjectByPublicUrlAndUserAndIsDeleted(slug, user, false)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project tidak ditemukan"));
        project.setIsDeleted(true);
        projectRepository.save(project);
        String keyRedis = user.getId().toString() + ":"+project.getPublicUrl();
        redisService.delete(keyRedis);
    }

    @Override
    public void changeStatusProject(String slug, User user) {
        Project project = projectRepository.findProjectByPublicUrlAndUserAndIsDeleted(slug, user, false)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project tidak ditemukan"));
        project.setIsActive(!project.getIsActive());
        projectRepository.save(project);
    }

    @Override
    public void generateTokenProject(String slug, User user) {
        Project project = projectRepository.findProjectByPublicUrlAndUserAndIsDeleted(slug, user, false)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project tidak ditemukan"));
        String keyRedis = user.getId().toString() + ":"+project.getPublicUrl();
        redisService.delete(keyRedis);
        String projectToken = jwtService.generateToken(user);
        Duration duration = Duration.ofDays(TIME_EXPIRED_DAY);
        redisService.save(keyRedis, projectToken, duration);
    }

    @Override
    public Boolean isTokenValid(String relayToken, String slug, String userId) {
        String keyRedis = userId + ":"+slug;
        String existingToken = redisService.get(keyRedis);
        return relayToken.equals(existingToken);
    }

    public Project mapProjectDtoToProject(ProjectRequestDTO dto){
        System.out.println("INI MASUK ====> "+ dto.getPublicUrl());
        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setPublicUrl(dto.getPublicUrl());
        project.setQueueName(dto.getPublicUrl());
        LocalDateTime localDateTime = LocalDateTime.now();
        project.setCreatedAt(Timestamp.valueOf(localDateTime));
        project.setUpdatedAt(Timestamp.valueOf(localDateTime));
        System.out.println("=====");
        System.out.println(project.getPublicUrl());
        return project;
    }
}
