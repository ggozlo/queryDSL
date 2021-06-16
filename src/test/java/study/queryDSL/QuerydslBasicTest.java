package study.queryDSL;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.queryDSL.entity.Member;
import study.queryDSL.entity.QMember;
import study.queryDSL.entity.QTeam;
import study.queryDSL.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.queryDSL.entity.QMember.*;
import static study.queryDSL.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;
    @BeforeEach // 개별 테스트 이전에 실행
    public void before() {
        queryFactory = new JPAQueryFactory(em);

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
    }

    @Test
    public void startJPQL() {
        String qlString =
                "select m from Member m " +
                "where m.username = :username";
        Member finfMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(finfMember.getUsername()).isEqualTo("member1");
        //then
    }

    @Test
    public void startQuerydsl() {

        //QMember m = new QMember("m"); // 같은 테이블 조인할 때만 직접 선언해서 사용

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        //given
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        //member.username.eq("member1").and(member.age.eq(10))
                        member.username.eq("member1"),(member.age.eq(10)) // and
                )
                .fetchOne();
            /*
            *   eq : equal, =
            *   ne : not equal, !=, .eq(x).not()
            *   .isNotNull
            *   .in()
            *  .notIn()
            *  .between()
            *  .goe() >= , .gt() > , .loe() <= , .lt() <
            *  .like() % , .contains() %~% , startWith ~%
            * */
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        //given
//        List<Member> fetch = queryFactory.selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .selectFrom(QMember.member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(QMember.member)
//                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults(); // 페이징 쿼리, 카운트 쿼리도 같이 보냄, 성능이 중요하면 카운트 쿼리는 따로쓰기 - 조인쿼리에선 성능저하

        results.getTotal(); // 요소의 총계
        List<Member> content = results.getResults(); // 요소 본문

        long total = queryFactory
                .selectFrom(member)
                .fetchCount(); // 카운트 쿼리

        //then
    }

    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()) // null이 마지막으로, 복수조건 정렬엔 콤마 로 구분
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging() {
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 시작 위치 - 최소는 0
                .limit(2) // 가져올 최대 갯수
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);

    }

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(), // 각종 집계함수도 사용 가능
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch(); // 복수의 데이터 타입을 반환할땐 튜플로 반환

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4); // 튜플에서 값을 획득할려면 select 절에 쓴 집계함수를 넣으면 값이 나옴
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    public void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name) // 그룹핑 가능능
                 .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) // 조인대상, 약칭
                //  기본은 inner 조인, leftjoin() 등은 따로 있음
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username").containsExactly("member1", "member2");
    }

    // 세타 조인
    // 회원 이름이 팀 이름과 같은 회원 조회
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        // 세타조인, from 절에 대상 테이블을 둘다 기입, 카테이션곱 으로 쿼리문을 짜서 조건에 맞게 가져옴

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    // 회원과 팀을 조인하면 팀 이름은 teamA, 회원은 모두 조회회
    // jpql : select m ,t from Member m left join m.team t on t.name = 'teamA'
   @Test
    public void join_in_filtering() {
       List<Tuple> result = queryFactory
               .select(member, team)
               .from(member)
               .leftJoin(member.team, team).on(team.name.eq("teamA")) // 외부조인은 on으로 해야함
//               .join(member.team, team).on(team.name.eq("teamA"))
//               .join(member.team, team).where(team.name.eq("teamA")) 내부 조인은 where로 일치조건 가능
               .fetch();

       for (Tuple tuple : result) {
           System.out.println("tuple = " + tuple);
       }

   }

   // 연관관계 없는 엔티티 외부조인
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team) // 연관관계 에 속한 테이블이 아닌 테이블 자체를  조인
                .on(member.username.eq(team.name)) // on절로 조인 조건을 지정
                .fetch();


        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    @PersistenceUnit // 엔티티 매니저 팩토리를 획득 하기 위한 어노테이션
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// 초기화가 된 엔티티 인지 아니면 안된 (프록시인?) 엔티티 인지
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() // 조인은 똑같이 쓰고 체이닝기법으로 fetch 조인임을 지정
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// 초기화가 된 엔티티 인지 아니면 안된 (프록시인?) 엔티티 인지
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }

    // 나이가 가장 많은
    // 나이가 평균 이상인
    @Test
    public void subQuery() {

        QMember memberSub = new QMember("memberSub"); // 메인 쿼리와 서브쿼리의 별칭은 다르게 써야함

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions // 서브쿼리를 표현하기 위한 JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        List<Member> result2 = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions // 서브쿼리를 표현하기 위한 JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);
        assertThat(result2).extracting("age").containsExactly(30,40);
    }

    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub"); // 메인 쿼리와 서브쿼리의 별칭은 다르게 써야함

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in( // 서브쿼리에서 반환된 나이들과 비교하여 필터링
                        JPAExpressions // 서브쿼리를 표현하기 위한 JPAExpressions
                            .select(memberSub.age)
                            .from(memberSub)
                            .where(memberSub.age.gt(10)) // 서브쿼리에서 나이가 10보다 큰 데이터들을 찾아내서 반환
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20,30,40);
    }

    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");


        List<Tuple> fetch = queryFactory
                .select(member.username,
                        JPAExpressions // 스태틱 임포트 가능
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member) // jpa 는 from 절의 서브쿼리가 안됨, 써야한다면 다른 jdbc 구현체를 쓸것
                .fetch();

        //then
        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder() // 복잡한 케이스문 을 생성 하기 위한 caseBuilder
                        .when(member.age.between(0, 20)).then("0~20살살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }

    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A")) // Expressions 클래스를 통한 상수 추력, 쿼리문에는 포함되지 않느다
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() {
        // {username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) // 문자열로 변환하여 문자열 더하기
                .from(member)
                .fetch();
        // .stringValue - Enum 타입 처리할때 유용하다
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


}
