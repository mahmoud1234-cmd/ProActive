package com.proactive.demo.dto.project;

import com.proactive.demo.model.ProjectMember;
import com.proactive.demo.model.User;
import lombok.Data;

@Data
public class ProjectMemberDto {

    private Long userId;
    private String userName;
    private String userEmail;
    private ProjectMember.ProjectRole projectRole;

    public static ProjectMemberDto from(ProjectMember pm) {
        ProjectMemberDto dto = new ProjectMemberDto();
        User u = pm.getUser();
        dto.setUserId(u.getId());
        dto.setUserName(u.getFirstName() + " " + u.getLastName());
        dto.setUserEmail(u.getEmail());
        dto.setProjectRole(pm.getProjectRole());
        return dto;
    }
}
