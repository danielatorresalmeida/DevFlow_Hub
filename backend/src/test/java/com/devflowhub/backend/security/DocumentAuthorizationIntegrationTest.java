package com.devflowhub.backend.security;

import com.devflowhub.backend.domain.*;
import com.devflowhub.backend.entity.*;
import com.devflowhub.backend.repository.*;
import com.devflowhub.backend.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocumentAuthorizationIntegrationTest {
    enum Parent { PROJECT, PROJECT_TASK, STANDALONE }
    record Scope(Long projectId, Long taskId, Project project) {
        String body() {
            return """
                    {"title":"Updated notes","content":"safe content","projectId":%s,"taskId":%s}
                    """.formatted(projectId, taskId);
        }
        String listUrl() {
            return projectId != null ? "/api/projects/" + projectId + "/documents"
                    : "/api/tasks/" + taskId + "/documents";
        }
    }
    @Autowired MockMvc mvc;
    @Autowired CollaboratorRepository collaborators;
    @Autowired ProjectRepository projects;
    @Autowired ProjectMembershipRepository memberships;
    @Autowired TaskRepository tasks;
    @Autowired DocumentRepository documents;
    @Autowired JwtTokenService tokens;

    @ParameterizedTest
    @EnumSource(Parent.class)
    void outsiderCannotReadListCreateEditOrDelete(Parent kind) throws Exception {
        Collaborator owner = user();
        Collaborator outsider = user();
        Scope scope = scope(kind, owner, ProjectMembershipRole.OWNER);
        Document document = document(scope);
        mvc.perform(as(outsider, get("/api/documents/{id}", document.getId())))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Document not found."));
        mvc.perform(as(outsider, get(scope.listUrl()))).andExpect(status().isNotFound());
        mvc.perform(as(outsider, post("/api/documents")).content(scope.body())).andExpect(status().isNotFound());
        mvc.perform(as(outsider, put("/api/documents/{id}", document.getId())).content(scope.body()))
                .andExpect(status().isNotFound());
        mvc.perform(as(outsider, delete("/api/documents/{id}", document.getId())))
                .andExpect(status().isNotFound());
        assertUnchanged(document, scope);
    }

    @ParameterizedTest
    @EnumSource(Parent.class)
    void authorizedOwnerCanCompleteDocumentWorkflow(Parent kind) throws Exception {
        Collaborator owner = user();
        Scope scope = scope(kind, owner, ProjectMembershipRole.OWNER);
        mvc.perform(as(owner, post("/api/documents")).content(scope.body())).andExpect(status().isCreated());
        Document doc = documents.findAll().stream().filter(d -> "Updated notes".equals(d.getTitle())).findFirst().orElseThrow();
        mvc.perform(as(owner, get("/api/documents/{id}", doc.getId()))).andExpect(status().isOk());
        mvc.perform(as(owner, get(scope.listUrl()))).andExpect(status().isOk());
        mvc.perform(as(owner, put("/api/documents/{id}", doc.getId())).content(scope.body())).andExpect(status().isOk());
        mvc.perform(as(owner, delete("/api/documents/{id}", doc.getId()))).andExpect(status().isNoContent());
        assertThat(documents.existsById(doc.getId())).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Parent.class, names = {"PROJECT", "PROJECT_TASK"})
    void viewerReadsButCannotWriteAndAssignedContributorCannotEditDocuments(Parent kind) throws Exception {
        Collaborator caller = user();
        Scope scope = scope(kind, caller, ProjectMembershipRole.VIEWER);
        Document doc = document(scope);
        mvc.perform(as(caller, get("/api/documents/{id}", doc.getId()))).andExpect(status().isOk());
        mvc.perform(as(caller, get(scope.listUrl()))).andExpect(status().isOk());
        mvc.perform(as(caller, post("/api/documents")).content(scope.body())).andExpect(status().isForbidden());
        mvc.perform(as(caller, put("/api/documents/{id}", doc.getId())).content(scope.body())).andExpect(status().isForbidden());
        mvc.perform(as(caller, delete("/api/documents/{id}", doc.getId()))).andExpect(status().isForbidden());
        membership(scope.project(), caller, ProjectMembershipRole.CONTRIBUTOR, ProjectMembershipStatus.ACTIVE);
        mvc.perform(as(caller, post("/api/documents")).content(scope.body())).andExpect(status().isCreated());
        mvc.perform(as(caller, put("/api/documents/{id}", doc.getId())).content(scope.body())).andExpect(status().isForbidden());
        mvc.perform(as(caller, delete("/api/documents/{id}", doc.getId()))).andExpect(status().isForbidden());
        assertUnchanged(doc, scope);
        membership(scope.project(), caller, ProjectMembershipRole.MANAGER, ProjectMembershipStatus.ACTIVE);
        mvc.perform(as(caller, put("/api/documents/{id}", doc.getId())).content(scope.body())).andExpect(status().isOk());
        mvc.perform(as(caller, delete("/api/documents/{id}", doc.getId()))).andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @EnumSource(Parent.class)
    void movingToHiddenDestinationLeavesOriginalUnchanged(Parent destinationKind) throws Exception {
        Collaborator caller = user();
        Scope original = scope(Parent.PROJECT, caller, ProjectMembershipRole.OWNER);
        Scope target = scope(destinationKind, user(), ProjectMembershipRole.OWNER);
        Document doc = document(original);
        mvc.perform(as(caller, put("/api/documents/{id}", doc.getId())).content(target.body()))
                .andExpect(status().isNotFound());
        assertUnchanged(doc, original);
    }

    @ParameterizedTest
    @EnumSource(Parent.class)
    void hiddenOriginalCannotBeMovedIntoVisibleProject(Parent originalKind) throws Exception {
        Collaborator caller = user();
        Scope original = scope(originalKind, user(), ProjectMembershipRole.OWNER);
        Scope target = scope(Parent.PROJECT, caller, ProjectMembershipRole.OWNER);
        Document doc = document(original);
        mvc.perform(as(caller, put("/api/documents/{id}", doc.getId())).content(target.body()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Document not found."));
        assertUnchanged(doc, original);
    }

    @Test
    void reassignmentChecksDestinationRoleAndPermitsAuthorizedProjectTaskAndStandaloneMoves() throws Exception {
        Collaborator caller = user();
        Scope original = scope(Parent.PROJECT, caller, ProjectMembershipRole.OWNER);
        Scope target = scope(Parent.PROJECT_TASK, caller, ProjectMembershipRole.VIEWER);
        Document doc = document(original);
        mvc.perform(as(caller, put("/api/documents/{id}", doc.getId())).content(target.body())).andExpect(status().isForbidden());
        assertUnchanged(doc, original);
        membership(target.project(), caller, ProjectMembershipRole.MANAGER, ProjectMembershipStatus.ACTIVE);
        mvc.perform(as(caller, put("/api/documents/{id}", doc.getId())).content(target.body())).andExpect(status().isOk());
        Scope standalone = scope(Parent.STANDALONE, caller, ProjectMembershipRole.OWNER);
        mvc.perform(as(caller, put("/api/documents/{id}", doc.getId())).content(standalone.body())).andExpect(status().isOk());
        mvc.perform(as(caller, put("/api/documents/{id}", doc.getId())).content(original.body())).andExpect(status().isOk());
    }

    @Test
    void inactiveMembershipIsHiddenAndGlobalAdminDoesNotBypassProjectMembership() throws Exception {
        Collaborator caller = user();
        caller.setSystemRole(SystemRole.ADMIN);
        collaborators.saveAndFlush(caller);
        Scope scope = scope(Parent.PROJECT, user(), ProjectMembershipRole.OWNER);
        Document doc = document(scope);
        mvc.perform(as(caller, get("/api/documents/{id}", doc.getId()))).andExpect(status().isNotFound());
        membership(scope.project(), caller, ProjectMembershipRole.OWNER, ProjectMembershipStatus.INACTIVE);
        mvc.perform(as(caller, get("/api/documents/{id}", doc.getId()))).andExpect(status().isNotFound());
        mvc.perform(as(caller, post("/api/documents")).content(scope.body())).andExpect(status().isNotFound());
    }

    @Test
    void missingResourcesAndMissingAuthenticationUseExistingErrorContract() throws Exception {
        Collaborator caller = user();
        mvc.perform(get("/api/documents/999999")).andExpect(status().isUnauthorized());
        for (var request : new MockHttpServletRequestBuilder[]{get("/api/documents/999999"), delete("/api/documents/999999"),
                put("/api/documents/999999").content(new Scope(999999L, null, null).body())}) {
            mvc.perform(as(caller, request)).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Document not found."));
        }
        for (Scope missing : new Scope[]{new Scope(999999L, null, null), new Scope(null, 999999L, null)}) {
            mvc.perform(as(caller, get(missing.listUrl()))).andExpect(status().isNotFound());
            mvc.perform(as(caller, post("/api/documents")).content(missing.body())).andExpect(status().isNotFound());
        }
    }

    @Test
    void deactivatedIdentityWithPreviouslyIssuedJwtCannotReadDocuments() throws Exception {
        Collaborator caller = user();
        Scope scope = scope(Parent.PROJECT, caller, ProjectMembershipRole.OWNER);
        Document doc = document(scope);
        String token = tokens.issue(caller).accessToken();
        caller.setActive(false);
        collaborators.saveAndFlush(caller);
        mvc.perform(get("/api/documents/{id}", doc.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder as(Collaborator caller, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(caller).accessToken()).contentType(MediaType.APPLICATION_JSON);
    }
    private Collaborator user() {
        Collaborator user = new Collaborator();
        user.setName("Document test"); user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPassword("unused-in-signed-token-tests"); user.setRole("Developer"); user.setActive(true);
        return collaborators.saveAndFlush(user);
    }
    private Scope scope(Parent kind, Collaborator caller, ProjectMembershipRole role) {
        Project project = null;
        if (kind != Parent.STANDALONE) {
            project = new Project(); project.setName("Document test " + UUID.randomUUID()); project.setStatus("PLANNED");
            project = projects.saveAndFlush(project);
            membership(project, caller, role, ProjectMembershipStatus.ACTIVE);
        }
        if (kind == Parent.PROJECT) return new Scope(project.getId(), null, project);
        Task task = new Task(); task.setTitle("Document parent"); task.setStatus("PENDING"); task.setPriority("MEDIUM");
        task.setProjectId(project == null ? null : project.getId()); task.setAssigneeId(caller.getId());
        return new Scope(null, tasks.saveAndFlush(task).getId(), project);
    }
    private void membership(Project project, Collaborator caller, ProjectMembershipRole role, ProjectMembershipStatus status) {
        ProjectMembership m = memberships.findByProjectIdAndCollaboratorId(project.getId(), caller.getId()).orElseGet(ProjectMembership::new);
        m.setProjectId(project.getId()); m.setCollaboratorId(caller.getId()); m.setRole(role); m.setStatus(status);
        memberships.saveAndFlush(m);
    }
    private Document document(Scope scope) {
        Document doc = new Document(); doc.setTitle("Original notes"); doc.setContent("Private content");
        doc.setProjectId(scope.projectId()); doc.setTaskId(scope.taskId());
        return documents.saveAndFlush(doc);
    }
    private void assertUnchanged(Document doc, Scope original) {
        Document actual = documents.findById(doc.getId()).orElseThrow();
        assertThat(actual.getTitle()).isEqualTo("Original notes");
        assertThat(actual.getProjectId()).isEqualTo(original.projectId());
        assertThat(actual.getTaskId()).isEqualTo(original.taskId());
    }
}
