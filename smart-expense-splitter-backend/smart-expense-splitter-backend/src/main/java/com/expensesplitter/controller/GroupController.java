package com.expensesplitter.controller;

import com.expensesplitter.dto.request.AddMemberRequest;
import com.expensesplitter.dto.request.CreateGroupRequest;
import com.expensesplitter.dto.response.GroupResponse;
import com.expensesplitter.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(request));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupResponse> addMember(@PathVariable Long groupId,
                                                    @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.ok(groupService.addMember(groupId, request));
    }

    @GetMapping("/my")
    public List<GroupResponse> getMyGroups() {
        return groupService.getMyGroups();
    }

    @GetMapping("/{groupId}")
    public GroupResponse getGroup(@PathVariable Long groupId) {
        return groupService.getGroupById(groupId);
    }
}
