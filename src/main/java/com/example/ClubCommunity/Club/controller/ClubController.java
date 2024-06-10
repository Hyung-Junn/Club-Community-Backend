package com.example.ClubCommunity.Club.controller;

import com.example.ClubCommunity.Club.dto.ClubDto;
import com.example.ClubCommunity.Club.service.ClubService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @PostMapping("/apply")
    public ResponseEntity<ClubDto> applyForClub(@RequestBody ClubDto clubDto, Authentication authentication) {
        // 동아리 신청 처리
        ClubDto createdClub = clubService.applyForClub(clubDto, authentication);
        return ResponseEntity.ok(createdClub);
    }

    @GetMapping("/applications")
    public ResponseEntity<List<ClubDto>> getAllClubApplications() {
        // 모든 동아리 신청 목록 조회
        List<ClubDto> clubApplications = clubService.getAllClubApplications();
        return ResponseEntity.ok(clubApplications);
    }

    @GetMapping("/applications/pending")
    public ResponseEntity<List<ClubDto>> getPendingClubApplications() {
        // PENDING 상태의 동아리 신청 목록 조회
        List<ClubDto> pendingClubApplications = clubService.getPendingClubApplications();
        return ResponseEntity.ok(pendingClubApplications);
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<ClubDto> approveClubApplication(@PathVariable("id") Long id) {
        // 동아리 신청 승인 처리
        ClubDto approvedClub = clubService.approveClubApplication(id);
        return ResponseEntity.ok(approvedClub);
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<ClubDto> rejectClubApplication(@PathVariable("id") Long id, @RequestParam("reason") String reason) {
        // 동아리 신청 거절 처리
        ClubDto rejectedClub = clubService.rejectClubApplication(id, reason);
        return ResponseEntity.ok(rejectedClub);
    }

    @PutMapping("/approve")
    public ResponseEntity<List<ClubDto>> approveClubApplications(@RequestBody IdsRequest idsRequest) {
        // 다건 동아리 신청 승인 처리
        List<ClubDto> approvedClubs = clubService.approveClubApplications(idsRequest.getIds());
        return ResponseEntity.ok(approvedClubs);
    }

    @PutMapping("/reject")
    public ResponseEntity<List<ClubDto>> rejectClubApplications(@RequestBody List<RejectionRequest> rejections) {
        // 다건 동아리 신청 거절 처리
        List<ClubDto> rejectedClubs = clubService.rejectClubApplications(rejections);
        return ResponseEntity.ok(rejectedClubs);
    }

    @Data
    public static class RejectionRequest {
        private Long id;
        private String reason;
    }

    @Data
    public static class IdsRequest {
        private List<Long> ids;
    }
}