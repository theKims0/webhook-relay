package com.abdul.relay.repository;

import com.abdul.relay.entity.Project;
import com.abdul.relay.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findProjectsByUserAndIsDeleted(User user, Boolean isDeleted);
    Boolean existsProjectByNameAndUserAndIsDeleted(String name, User user, Boolean isDeleted);

    Optional<Project> findProjectByPublicUrlAndUserAndIsDeleted(String publicUrl, User user, Boolean isDeleted);
}
