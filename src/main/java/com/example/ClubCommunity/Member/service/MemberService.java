package com.example.ClubCommunity.Member.service;

import com.example.ClubCommunity.Club.domain.ClubMember;
import com.example.ClubCommunity.Club.repository.ClubMemberRepository;
import com.example.ClubCommunity.Member.domain.Member;
import com.example.ClubCommunity.Member.dto.*;
import com.example.ClubCommunity.Member.repository.MemberRepository;
import com.example.ClubCommunity.Member.util.JwtTokenProvider;
import com.example.ClubCommunity.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ClubMemberRepository clubMemberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public TokenDto registerMember(MemberRegistrationDto registrationDto) { //회원가입 시 DB저장 후 토큰 반환
        validateRegistration(registrationDto);
        Member member = buildMember(registrationDto);
        memberRepository.save(member);
        String token = jwtTokenProvider.createToken(member.getLoginId(), member.getUserType());
        return TokenDto.builder()
                .token(token)
                .build();
    }

    public TokenDto registerKakaoMember(MemberRegistrationKakaoDto registrationDto) { //회원가입 시 DB저장 후 토큰 반환
        validateKakaoRegistration(registrationDto);
        Member member = buildKakaoMember(registrationDto);
        memberRepository.save(member);
        String token = jwtTokenProvider.createToken(member.getEmail(), member.getUserType());
        return TokenDto.builder()
                .token(token)
                .build();
    }

    private void validateRegistration(MemberRegistrationDto dto) { //회원가입 정보 검증
        memberRepository.findByLoginId(dto.getLoginId()).ifPresent(m -> {
            throw new DuplicateLoginIdException("이미 등록된 ID 입니다.");
        });
        memberRepository.findByEmail(dto.getEmail()).ifPresent(m -> {
            throw new DuplicateEmailException("이미 등록된 이메일입니다.");
        });
        memberRepository.findByStudentId(dto.getStudentId()).ifPresent(m -> {
            throw new DuplicateNicknameException("이미 등록된 학번입니다.");
        });
        memberRepository.findByPhoneNumber(dto.getPhoneNumber()).ifPresent(m -> {
            throw new DuplicateNicknameException("이미 등록된 전화번호 입니다.");
        });
    }

    private void validateKakaoRegistration(MemberRegistrationKakaoDto dto) { //회원가입 정보 검증
        memberRepository.findByEmail(dto.getEmail()).ifPresent(m -> {
            throw new DuplicateEmailException("이미 등록된 이메일입니다.");
        });
        memberRepository.findByStudentId(dto.getStudentId()).ifPresent(m -> {
            throw new DuplicateNicknameException("이미 등록된 학번입니다.");
        });
        memberRepository.findByPhoneNumber(dto.getPhoneNumber()).ifPresent(m -> {
            throw new DuplicateNicknameException("이미 등록된 전화번호 입니다.");
        });
    }

    private Member buildMember(MemberRegistrationDto dto) { //dto로 Member 엔티티 생성
        return Member.builder()
                .username(dto.getUsername())
                .birth(dto.getBirth())
                .gender(dto.getGender())
                .department(dto.getDepartment())
                .studentId(dto.getStudentId())
                .loginId(dto.getLoginId())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .phoneNumber(dto.getPhoneNumber())
                .email(dto.getEmail())
                .userType(dto.getUserType())
                .build();
    }

    private Member buildKakaoMember(MemberRegistrationKakaoDto dto) { //dto로 Member 엔티티 생성
        return Member.builder()
                .username(dto.getUsername())
                .birth(dto.getBirth())
                .gender(dto.getGender())
                .department(dto.getDepartment())
                .studentId(dto.getStudentId())
                .phoneNumber(dto.getPhoneNumber())
                .loginId(dto.getEmail())
                .email(dto.getEmail())
                .userType(dto.getUserType())
                .build();
    }


    public TokenDto loginMember(MemberLoginDto loginDto) { //로그인 시 아이디 비밀번호 검증 후 토큰 반환
        Member member = memberRepository.findByLoginId(loginDto.getLoginId())
                .orElseThrow(() -> new UserNotFoundException("회원 정보를 찾을 수 없습니다."));//아이디가 존재하지 않을때
        validatePassword(loginDto.getPassword(), member.getPasswordHash()); //비밀번호 검증
        String token = jwtTokenProvider.createToken(member.getLoginId(), member.getUserType());
        return TokenDto.builder()
                .token(token)
                .build();
    }

    public TokenDto KakaologinMember(String email) { //로그인 시 이메일 검증 후 토큰 반환
        Optional<Member> OptionalMember = memberRepository.findByEmail(email);
        if (OptionalMember.isPresent()) {
            String token = jwtTokenProvider.createToken(email, OptionalMember.get().getUserType());
            return TokenDto.builder()
                    .token(token)
                    .build();
        } else {
            return null;
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) { //비밀번호 검증
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new InvalidPasswordException("비밀번호가 일치하지 않습니다.");
        }
    }

    private String createToken(Member member) {
        return jwtTokenProvider.createToken(member.getLoginId(), member.getUserType());
    }

    public MemberKakaoDto getKakaoMemberDetailsByUsername(String username) {
        return memberRepository.findByEmail(username)
                .map(this::convertToMemberKakaoDto)
                .orElseThrow(() -> new UserNotFoundException("회원 정보를 찾을 수 없습니다."));
    }
    public MemberDto getMemberDetailsByUsername(String username) {
        return memberRepository.findByLoginId(username)
                .map(this::convertToMemberDto)
                .orElseThrow(() -> new UserNotFoundException("회원 정보를 찾을 수 없습니다."));
    }
    public MemberDto updateMemberDetails(Long id, MemberUpdateDto updateDto, String username) {
        Member existingMember = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 회원을 찾을 수 없습니다: " + id));

        if (!existingMember.getLoginId().equals(username) && !existingMember.getUserType().equals(Member.UserType.ROLE_ADMIN)) {
            throw new UnauthorizedAccessException("이 회원 정보를 업데이트할 권한이 없습니다.");
        }

        updateMemberFromDto(existingMember, updateDto);
        memberRepository.save(existingMember);
        return convertToMemberDto(existingMember);
    }

    private void updateMemberFromDto(Member member, MemberUpdateDto dto) {

        member.setUsername(dto.getUsername());
        member.setEmail(dto.getEmail());
        member.setGender(dto.getGender());
        member.setUserType(dto.getUserType());
    }


    public List<MemberDto> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(this::convertToMemberDto)
                .collect(Collectors.toList());
    }
    private MemberDto convertToMemberDto(Member member) {
        return new MemberDto(
                member.getId(),
                member.getUsername(),
                member.getBirth(),
                member.getGender(),
                member.getDepartment(),
                member.getStudentId(),
                member.getLoginId(),
                member.getPhoneNumber(),
                member.getEmail(),
                member.getUserType()
        );
    }
    private MemberKakaoDto convertToMemberKakaoDto(Member member) {
        return new MemberKakaoDto(
                member.getId(),
                member.getUsername(),
                member.getBirth(),
                member.getGender(),
                member.getDepartment(),
                member.getStudentId(),
                member.getPhoneNumber(),
                member.getEmail(),
                member.getUserType()
        );
    }

    public boolean checkIfUserIsAdmin(String token) {
        Member.UserType userType = jwtTokenProvider.getUserTypeFromToken(token);
        return userType == Member.UserType.ROLE_ADMIN;
    }

    @Transactional
    public void deleteMember(Long id, String username) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("회원을 찾을 수 없습니다: " + id));
        if (!member.getLoginId().equals(username) && !member.getUserType().equals(Member.UserType.ROLE_ADMIN)) {
            throw new UnauthorizedAccessException("삭제 권한이 없습니다.");
        }
        // 마지막으로 회원 정보 삭제
        memberRepository.deleteById(id);
    }

    public void changeMemberPassword(Long memberId, String currentPassword, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 회원을 찾을 수 없습니다: " + memberId));

        if (!passwordEncoder.matches(currentPassword, member.getPasswordHash())) {
            throw new InvalidPasswordException("현재 비밀번호가 정확하지 않습니다.");
        }

        member.setPasswordHash(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    public void changeUserRole(Long memberId, Member.UserType newRole) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 ID의 회원을 찾을 수 없습니다: " + memberId));
        member.setUserType(newRole);
        memberRepository.save(member);
    }

    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("회원 정보를 찾을 수 없습니다."));
    }

    public Member getMemberLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException("회원 정보를 찾을 수 없습니다."));
    }

    //유저의 동아리 가입 여부 반환
    public Boolean checkJoinClub(Long clubId, String username) {
        Member member = memberRepository.findByLoginId(username)
                .orElseThrow(() -> new UserNotFoundException("회원 정보를 찾을 수 없습니다."));
        ClubMember clubMember = clubMemberRepository.findByMemberIdAndClubId(member.getId(), clubId)
                .orElseThrow(() -> new UserNotFoundException("가입 신청 정보를 찾을 수 없습니다."));

        if(clubMember.getStatus() == ClubMember.MembershipStatus.APPROVED)
            return true;
        else
            return false;

    }
}
