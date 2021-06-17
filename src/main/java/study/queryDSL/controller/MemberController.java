package study.queryDSL.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.queryDSL.dto.MemberSearchCondition;
import study.queryDSL.dto.MemberTeamDto;
import study.queryDSL.repository.MemberJpaRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) { // 파라미터값은 자동 바인딩
        return memberJpaRepository.search(condition);
    }

}
