package study.queryDSL.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import study.queryDSL.entity.Member;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {
// 데이터 JPA 와 쿼리DSL을 이용한 레퍼지토리
    List<Member> findByUsername(String username);
}
