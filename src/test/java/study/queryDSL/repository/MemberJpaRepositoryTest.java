package study.queryDSL.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.queryDSL.dto.MemberSearchCondition;
import study.queryDSL.dto.MemberTeamDto;
import study.queryDSL.entity.Member;
import study.queryDSL.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    private EntityManager em;
    @Autowired
    MemberJpaRepository membeJparRepository;

    @Test
    public void basicTest() {
        Member member = new Member("username1", 10);
        membeJparRepository.save(member);

        Member findMember = membeJparRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = membeJparRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = membeJparRepository.findByUsername("username1");
        assertThat(result2).containsExactly(member);
    }

    @Test
    public void basicTest_Querydsl() {
        Member member = new Member("username1", 10);
        membeJparRepository.save(member);

        Member findMember = membeJparRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = membeJparRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = membeJparRepository.findByUsername("username1");
        assertThat(result2).containsExactly(member);
    }

    @Test
    public void basicQuerydslTest() {
        Member member = new Member("username1", 10);
        membeJparRepository.save(member);

        Member findMember = membeJparRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = membeJparRepository.findAll_Querydsl();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = membeJparRepository.findByUsername_Querydsl("username1");
        assertThat(result2).containsExactly(member);
    }

    @Test
    public void searchTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = membeJparRepository.searchByBuilder(condition);

        assertThat(result).extracting("username").containsExactly("member4");
    }

    @Test
    public void searchTest2() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition(); // 동적 쿼리의 조건이 아무것도 없으면 전부 끌고옴
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = membeJparRepository.search(condition);

        assertThat(result).extracting("username").containsExactly("member4");
    }


}