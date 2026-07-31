package com.expensesplitter.service;

import com.expensesplitter.dto.request.AddMemberRequest;
import com.expensesplitter.dto.request.CreateGroupRequest;
import com.expensesplitter.dto.response.GroupMemberResponse;
import com.expensesplitter.dto.response.GroupResponse;
import com.expensesplitter.entity.Group;
import com.expensesplitter.entity.GroupMember;
import com.expensesplitter.entity.User;
import com.expensesplitter.exception.BadRequestException;
import com.expensesplitter.exception.ForbiddenException;
import com.expensesplitter.exception.ResourceNotFoundException;
import com.expensesplitter.repository.GroupMemberRepository;
import com.expensesplitter.repository.GroupRepository;
import com.expensesplitter.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserService userService;

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        User currentUser = userService.getUserEntityByEmail(SecurityUtil.getCurrentUserEmail());

        Group group = Group.builder()
                .name(request.getName())
                .createdBy(currentUser)
                .build();
        Group saved = groupRepository.save(group);

        // The creator is automatically the first member of their own group —
        // otherwise they couldn't add expenses to it themselves.
        GroupMember membership = new GroupMember(saved, currentUser);
        groupMemberRepository.save(membership);

        return toResponse(saved, List.of(membership));
    }

    @Transactional
    public GroupResponse addMember(Long groupId, AddMemberRequest request) {
        Group group = getGroupOrThrow(groupId);
        User requester = userService.getUserEntityByEmail(SecurityUtil.getCurrentUserEmail());

        // Only existing members can add new members — prevents randoms from
        // adding themselves (or others) to a group they have no relation to.
        assertIsMember(groupId, requester.getId());

        User userToAdd = userService.getUserEntityById(request.getUserId());

        if (groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, userToAdd.getId())) {
            throw new BadRequestException("User is already a member of this group");
        }

        GroupMember membership = new GroupMember(group, userToAdd);
        groupMemberRepository.save(membership);

        List<GroupMember> allMembers = groupMemberRepository.findByGroup_Id(groupId);
        return toResponse(group, allMembers);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getMyGroups() {
        User currentUser = userService.getUserEntityByEmail(SecurityUtil.getCurrentUserEmail());
        List<GroupMember> myMemberships = groupMemberRepository.findMyGroups(currentUser.getId());

        return myMemberships.stream()
                .map(gm -> {
                    List<GroupMember> allMembers = groupMemberRepository.findByGroup_Id(gm.getGroup().getId());
                    return toResponse(gm.getGroup(), allMembers);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(Long groupId) {
        Group group = getGroupOrThrow(groupId);
        User requester = userService.getUserEntityByEmail(SecurityUtil.getCurrentUserEmail());
        assertIsMember(groupId, requester.getId());

        List<GroupMember> members = groupMemberRepository.findByGroup_Id(groupId);
        return toResponse(group, members);
    }

    // ---------- helpers used by other services too (e.g. ExpenseService) ----------

    public Group getGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
    }

    public void assertIsMember(Long groupId, Long userId) {
        boolean isMember = groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId);
        if (!isMember) {
            throw new ForbiddenException("You are not a member of this group");
        }
    }

    private GroupResponse toResponse(Group group, List<GroupMember> members) {
        List<GroupMemberResponse> memberResponses = members.stream()
                .map(gm -> GroupMemberResponse.builder()
                        .userId(gm.getUser().getId())
                        .name(gm.getUser().getName())
                        .email(gm.getUser().getEmail())
                        .joinedAt(gm.getJoinedAt())
                        .build())
                .toList();

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .createdBy(group.getCreatedBy().getId())
                .createdByName(group.getCreatedBy().getName())
                .createdAt(group.getCreatedAt())
                .members(memberResponses)
                .build();
    }
}
