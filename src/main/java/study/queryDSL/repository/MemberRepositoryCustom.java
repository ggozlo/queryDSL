package study.queryDSL.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.queryDSL.dto.MemberSearchCondition;
import study.queryDSL.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {
    // DATA JPA 와 쿼리DSL을 결합하기 위한 커스텀 레퍼지토리
    List<MemberTeamDto> search(MemberSearchCondition condition);

    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);

    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}
